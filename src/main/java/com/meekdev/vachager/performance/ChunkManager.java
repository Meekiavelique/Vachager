package com.meekdev.vachager.performance;

import com.meekdev.vachager.VachagerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.scheduler.BukkitTask;

public class ChunkManager implements Listener {

    private final VachagerPlugin plugin;
    private final TPSMonitor tpsMonitor;


    private volatile boolean generationEnabled = true;
    private volatile boolean wasRecentlyDisabled = false;

    private final double MIN_TPS;
    private static final int TPS_CHECK_INTERVAL = 100;
    private BukkitTask monitorTask;

    public ChunkManager(VachagerPlugin plugin) {
        this.plugin = plugin;
        this.tpsMonitor = new TPSMonitor();


        this.MIN_TPS = plugin.getConfig().getDouble("performance.min-tps", 14.0);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);


        startTPSMonitoring();
    }

    private void startTPSMonitoring() {
        monitorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            double currentTPS = tpsMonitor.getCurrentTPS();

            if (currentTPS < MIN_TPS && generationEnabled) {
                disableGeneration();
            } else if (currentTPS >= MIN_TPS && !generationEnabled) {
                enableGeneration();
            }
        }, TPS_CHECK_INTERVAL, TPS_CHECK_INTERVAL);
    }

    private void disableGeneration() {
        generationEnabled = false;
        if (!wasRecentlyDisabled) {
            plugin.getServer().broadcast(
                    Component.text("⚠ La génération de nouveaux chunks est temporairement désactivée en raison de la charge serveur.",
                            NamedTextColor.RED)
            );
            wasRecentlyDisabled = true;


            plugin.getServer().getWorlds().forEach(this::updateWorldSettings);
        }
    }

    private void enableGeneration() {
        generationEnabled = true;
        if (wasRecentlyDisabled) {
            plugin.getServer().broadcast(
                    Component.text("✔ La génération de nouveaux chunks est à nouveau active.",
                            NamedTextColor.GREEN)
            );
            wasRecentlyDisabled = false;


            plugin.getServer().getWorlds().forEach(this::updateWorldSettings);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldInit(WorldInitEvent event) {
        updateWorldSettings(event.getWorld());
    }

    private void updateWorldSettings(World world) {

        if (!generationEnabled) {
            world.setKeepSpawnInMemory(false);
            world.setViewDistance(Math.min(world.getViewDistance(), 4));
        } else {

            world.setKeepSpawnInMemory(true);
            world.setViewDistance(plugin.getServer().getViewDistance());
        }
    }

    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }


        plugin.getServer().getWorlds().forEach(world -> {
            world.setKeepSpawnInMemory(true);
            world.setViewDistance(plugin.getServer().getViewDistance());
        });
    }


    private static class TPSMonitor {
        private static final int SAMPLE_SIZE = 40; // 2 seconds of ticks
        private final long[] tickTimes = new long[SAMPLE_SIZE];
        private int currentIndex = 0;
        private long lastTick = System.nanoTime();

        public void addTick() {
            long now = System.nanoTime();
            long duration = now - lastTick;
            tickTimes[currentIndex] = duration;
            currentIndex = (currentIndex + 1) % SAMPLE_SIZE;
            lastTick = now;
        }

        public double getCurrentTPS() {
            long total = 0;
            for (long time : tickTimes) {
                total += time;
            }

            double averageTickTime = (total / (double) SAMPLE_SIZE) / 1_000_000.0;
            return Math.min(20.0, 1000.0 / averageTickTime);
        }
    }

    public boolean isGenerationEnabled() {
        return generationEnabled;
    }
}