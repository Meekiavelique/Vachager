package com.meekdev.vachager.features.worlds;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.logging.log4j.LogManager.getLogger;


public class EndEventManager {
    private final VachagerSMP plugin;
    private final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
    private BukkitTask scheduleTask;
    private boolean endEnabled = false;
    private long nextEventTime = -1;
    private final List<Vector> safeSpawnLocations = new ArrayList<>();
    private final Random random = new Random();

    private static final String COLOR_SUCCESS = "#a7ff78";
    private static final String COLOR_INFO = "#ff9d52";

    private static final String CONFIG_FILE = "end_data.yml";

    private static final int START_HOUR = 13;
    private static final int START_MINUTE = 30;
    private static final int END_HOUR = 17;
    private static final int END_MINUTE = 30;
    private static final int SPAWN_HEIGHT = 65;

    public EndEventManager(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getConfigManager().createConfig(CONFIG_FILE);
        loadData();
        setupSafeLocations();
        scheduleNextEvent();
    }

    private void saveData() {
        FileConfiguration config = plugin.getConfigManager().getConfig(CONFIG_FILE);
        config.set("end-enabled", endEnabled);
        config.set("next-event-time", nextEventTime);
        plugin.getConfigManager().saveConfig(CONFIG_FILE);
    }

    private void setupSafeLocations() {
        safeSpawnLocations.addAll(Arrays.asList(
                new Vector(100, SPAWN_HEIGHT, 0),
                new Vector(-100, SPAWN_HEIGHT, 0),
                new Vector(0, SPAWN_HEIGHT, 100),
                new Vector(0, SPAWN_HEIGHT, -100),
                new Vector(75, SPAWN_HEIGHT, 75),
                new Vector(-75, SPAWN_HEIGHT, -75),
                new Vector(75, SPAWN_HEIGHT, -75),
                new Vector(-75, SPAWN_HEIGHT, 75)
        ));
    }


    public boolean toggleEndManually(Player player) {
        if (scheduleTask != null && !scheduleTask.isCancelled()) {
            plugin.getLogger().warning("§c[End] Toggle attempt by " + player.getName() + " during event");
            return false;
        }

        endEnabled = !endEnabled;
        saveData();

        plugin.getLogger().info("§e[End] Manually toggled by " + player.getName() + " to: " + endEnabled);

        String stateMsg = endEnabled ? "activé" : "désactivé";
        MessageUtils.sendMessage(player, "L'End a été " + stateMsg, COLOR_SUCCESS);

        return true;
    }

    private void scheduleNextEvent() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
        }

        LocalDateTime now = LocalDateTime.now(PARIS_ZONE);
        LocalDateTime nextEvent = calculateNextEventTime(now);
        nextEventTime = nextEvent.atZone(PARIS_ZONE).toInstant().toEpochMilli();

        saveData();

        long delayTicks = ChronoUnit.SECONDS.between(now, nextEvent) * 20L;
        scheduleTask = plugin.getServer().getScheduler().runTaskLater(plugin,
                this::triggerEndEvent, delayTicks);
    }


    private LocalDateTime calculateNextEventTime(LocalDateTime now) {
        LocalDateTime base = now.withHour(START_HOUR).withMinute(START_MINUTE).withSecond(0);

        if (now.getHour() >= END_HOUR && now.getMinute() >= END_MINUTE) {
            base = base.plusDays(1);
        }
        else if (now.getHour() < START_HOUR ||
                (now.getHour() == START_HOUR && now.getMinute() < START_MINUTE)) {
            // Keep base as is
        }
        else {
            base = now;
        }

        int minMinutes = START_HOUR * 60 + START_MINUTE;
        int maxMinutes = END_HOUR * 60 + END_MINUTE;
        int targetMinutes = ThreadLocalRandom.current().nextInt(minMinutes, maxMinutes + 1);

        return base.withHour(targetMinutes / 60)
                .withMinute(targetMinutes % 60)
                .withSecond(0);
    }

    private void triggerEndEvent() {
        endEnabled = true;
        saveData();

        String time = LocalDateTime.now(PARIS_ZONE).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        plugin.getLogger().info("§a[End] Event triggered at " + time);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            MessageUtils.sendMessage(player, "L'END EST MAINTENANT OUVERT!", COLOR_SUCCESS);
            MessageUtils.sendMessage(player, "Utilisez une Ender Pearl dans le portail pour spawn aléatoirement!", COLOR_INFO);
        }

        scheduleNextEvent();
    }
    public Location getRandomSafeLocation(World endWorld) {
        if (safeSpawnLocations.isEmpty()) return null;

        Vector randomVector = safeSpawnLocations.get(
                random.nextInt(safeSpawnLocations.size())
        );

        return new Location(endWorld,
                randomVector.getX(),
                randomVector.getY(),
                randomVector.getZ()
        );
    }

    public boolean isEndEnabled() {
        return endEnabled;
    }

    private void loadData() {
        FileConfiguration config = plugin.getConfigManager().getConfig(CONFIG_FILE);
        endEnabled = config.getBoolean("end-enabled", false);
        nextEventTime = config.getLong("next-event-time", -1);

        if (nextEventTime > System.currentTimeMillis()) {
            long delayTicks = (nextEventTime - System.currentTimeMillis()) / 50;
            scheduleTask = plugin.getServer().getScheduler().runTaskLater(plugin,
                    this::triggerEndEvent, Math.max(0, delayTicks));
        } else {
            scheduleNextEvent();
        }
    }

    public void cleanup() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
        }
        saveData();
    }

    public void notifyEndDisabled(Player player) {
        MessageUtils.sendMessage(player, "L'END EST ACTUELLEMENT DÉSACTIVÉ!", "#ffce47");
    }

    public void notifyEndPearlUsage(Player player) {
        MessageUtils.sendMessage(player, "Vous apparaîtrez à un endroit aléatoire dans l'End!", COLOR_INFO);
    }

    public void notifyEndTeleportFailed(Player player) {
        MessageUtils.sendMessage(player, "Aucun emplacement sûr trouvé dans l'End!", "#ff6b6b");
    }

    public void notifyEndTeleport(Player player) {
        MessageUtils.sendMessage(player, "Vous allez apparaître à un endroit aléatoire dans l'End!", COLOR_INFO);
        getLogger().info("§a[End] Player " + player.getName() + " teleported to End");
    }
}