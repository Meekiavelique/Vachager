package com.meekdev.vachager.features.respawn;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
import com.meekdev.vachager.utils.LocationUtils;
import com.meekdev.vachager.utils.TimeUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LodestoneManager {
    private final VachagerSMP plugin;
    private final Map<Location, LodestoneData> activeLodestones;
    private final Map<UUID, Set<Location>> playerLodestones;
    private final Map<Location, Map<UUID, BlockDisplay>> playerGlowDisplays;
    private final Set<BukkitTask> tasks;
    private volatile boolean enabled;

    // Configuration constants
    private static final String COLOR_ERROR = "#ff4d2e";
    private static final String COLOR_WARN = "#ff9d52";
    private static final String COLOR_SUCCESS = "#a7ff78";

    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;
    private static final int MIN_DISTANCE = 100;
    private static final int MIN_DIAMONDS = 5;
    private static final int MAX_DIAMONDS = 32;
    private static final int MINUTES_PER_DIAMOND = 10;
    private static final int MAX_MINUTES = 360;
    private static final int WARNING_TIME = 300;
    private static final int DISPLAY_RANGE = 300;
    private static final int MAX_SHARED_PLAYERS = 5;
    private static final int BREAK_DURATION = 100;

    public LodestoneManager(VachagerSMP plugin) {
        this.plugin = plugin;
        this.activeLodestones = new ConcurrentHashMap<>();
        this.playerLodestones = new ConcurrentHashMap<>();
        this.playerGlowDisplays = new ConcurrentHashMap<>();
        this.tasks = ConcurrentHashMap.newKeySet();
        this.enabled = true;
        startCleanupTask();
    }

    public boolean setLodestone(Player player, Block block, int diamonds) {
        if (!enabled || !validSetup(player, block, diamonds)) {
            return false;
        }

        Location loc = block.getLocation();

        int minutes = Math.min(diamonds * MINUTES_PER_DIAMOND, MAX_MINUTES);
        long expiryTime = System.currentTimeMillis() + TimeUtils.getMillisFromMinutes(minutes);

        try {

            TextDisplay textDisplay = createTextDisplay(loc, player.getName());
            BlockDisplay glowDisplay = createGlowDisplay(loc);

            LodestoneData data = new LodestoneData(loc, player.getUniqueId(), expiryTime, textDisplay, glowDisplay);
            activeLodestones.put(loc, data);
            playerLodestones.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(loc);

            showGlowDisplay(loc, player);
            tagBlock(block, player.getUniqueId());
            startTimers(data);
            playSetupEffects(player, loc);

            plugin.getRespawnManager().setRespawnPoint(player, loc);

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            mainHand.setAmount(mainHand.getAmount() - diamonds);

            MessageUtils.sendMessage(player, "Point de réapparition créé! (" + minutes + " minutes)", COLOR_SUCCESS);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create lodestone at " + loc + ": " + e.getMessage());
            MessageUtils.sendMessage(player, "Erreur lors de la création du point de réapparition!", COLOR_ERROR);
            return false;
        }
    }

    private TextDisplay createTextDisplay(Location loc, String ownerName) {
        Location displayLoc = loc.clone().add(0.5, 1.75, 0.5);
        World world = loc.getWorld();
        if (world == null) throw new IllegalStateException("Invalid world for location");

        return world.spawn(displayLoc, TextDisplay.class, display -> {
            display.setText("§b⚡ §f" + ownerName + " §b⚡");
            display.setDefaultBackground(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setShadowed(true);
            display.setSeeThrough(false);
        });
    }

    private BlockDisplay createGlowDisplay(Location loc) {
        World world = loc.getWorld();
        if (world == null) throw new IllegalStateException("Invalid world for location");

        return world.spawn(loc, BlockDisplay.class, display -> {
            display.setBlock(Material.LODESTONE.createBlockData());
            display.setBrightness(new Display.Brightness(15, 15));
            display.setPersistent(false);
            display.getPersistentDataContainer().set(
                    plugin.getKey("lodestone"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        });
    }

    private void tagBlock(Block block, UUID ownerUUID) {
        block.setMetadata("lodestone_owner", new FixedMetadataValue(plugin, ownerUUID.toString()));
        block.setMetadata("lodestone_time", new FixedMetadataValue(plugin, System.currentTimeMillis()));
    }

    private void startTimers(LodestoneData data) {
        tasks.add(startExpiryTimer(data));
        tasks.add(startActionBarTimer(data));
    }

    private BukkitTask startExpiryTimer(LodestoneData data) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!data.isValid()) return;

            Location loc = data.getLocation();
            playParticleEffects(loc);

            long timeLeft = (data.getExpiryTime() - System.currentTimeMillis()) / 1000;
            if (timeLeft <= 0) {
                removeLodestone(loc);
                return;
            }

            if (timeLeft <= WARNING_TIME) {
                warnPlayers(data, (int)(timeLeft / 60) + 1);
            }
        }, 20L, 20L);
    }

    private void playParticleEffects(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Location particleLoc = loc.clone().add(0.5, 0.1, 0.5);

        world.spawnParticle(Particle.END_ROD, particleLoc.clone().add(0, 1, 0),
                1, 0.25, 0.25, 0.25, 0.01);

        if (Math.random() < 0.1) {
            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc.clone().add(0, 0.5, 0),
                    3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void warnPlayers(LodestoneData data, int minutesLeft) {
        Set<UUID> allPlayers = new HashSet<>(data.getSharedPlayers());
        allPlayers.add(data.getOwnerUUID());

        String color = minutesLeft <= 1 ? COLOR_ERROR : COLOR_WARN;
        String message = net.md_5.bungee.api.ChatColor.of(color) +
                "Point de réapparition: " + minutesLeft + " minutes";

        for (UUID uuid : allPlayers) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline() &&
                    LocationUtils.isWithinDistance(player.getLocation(), data.getLocation(), DISPLAY_RANGE)) {

                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));

                if (minutesLeft <= 5) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }
            }
        }
    }

    private BukkitTask startActionBarTimer(LodestoneData data) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!data.isValid()) return;

            long timeLeft = (data.getExpiryTime() - System.currentTimeMillis()) / 1000;
            if (timeLeft <= 0) return;

            updateActionBar(data.getOwnerUUID(), timeLeft, data.getLocation());
            for (UUID uuid : data.getSharedPlayers()) {
                updateActionBar(uuid, timeLeft, data.getLocation());
            }
        }, 20L, 20L);
    }

    private void updateActionBar(UUID playerUUID, long timeLeft, Location loc) {
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player == null || !player.isOnline() ||
                !LocationUtils.isWithinDistance(player.getLocation(), loc, DISPLAY_RANGE)) {
            return;
        }

        String timeStr = TimeUtils.formatTime(timeLeft);
        String color = timeLeft > 3600 ? COLOR_SUCCESS : timeLeft > 600 ? COLOR_WARN : COLOR_ERROR;

        String message = net.md_5.bungee.api.ChatColor.of(color) + "Point de réapparition: " + timeStr;
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }



    public void startBreaking(Player player, Block block) {
        LodestoneData data = activeLodestones.get(block.getLocation());
        if (data == null || !data.isValid()) return;

        if (!data.isOwner(player.getUniqueId()) && !data.getSharedPlayers().contains(player.getUniqueId())) {
            MessageUtils.sendMessage(player, "Vous ne pouvez pas détruire ce point!", COLOR_ERROR);
            return;
        }

        if (data.getBreakTask() != null) return;

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!data.isValid()) {
                if (data.getBreakTask() != null) {
                    data.getBreakTask().cancel();
                    data.setBreakTask(null);
                }
                return;
            }

            data.setBreakProgress(data.getBreakProgress() + 2);
            playBreakEffects(block);

            if (data.getBreakProgress() >= BREAK_DURATION) {
                removeLodestone(block.getLocation());
                data.setBreakTask(null);
            }
        }, 1L, 1L);

        data.setBreakTask(task);
    }

    private void playBreakEffects(Block block) {
        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.BLOCK_CRUMBLE,
                loc.clone().add(0.5, 0.5, 0.5),
                10, 0.3, 0.3, 0.3, 0,
                Material.LODESTONE.createBlockData());

        if (Math.random() < 0.05) {
            world.playSound(loc, Sound.BLOCK_STONE_HIT, 0.5f, 0.5f);
        }
    }

    public void stopBreaking(Location loc) {
        LodestoneData data = activeLodestones.get(loc);
        if (data != null) {
            if (data.getBreakTask() != null) {
                data.getBreakTask().cancel();
                data.setBreakTask(null);
            }
            data.setBreakProgress(0);
        }
    }

    public boolean upgradeLodestone(Player player, Location loc) {
        LodestoneData data = activeLodestones.get(loc);
        if (data == null || !data.isValid()) return false;


        if (data.getOwnerUUID().equals(player.getUniqueId())) {
            MessageUtils.sendMessage(player, "Vous êtes déjà le propriétaire!", COLOR_ERROR);
            return false;
        }

        if (data.getSharedPlayers().contains(player.getUniqueId())) {
            MessageUtils.sendMessage(player, "Vous partagez déjà ce point!", COLOR_ERROR);
            return false;
        }

        if (data.getSharedPlayers().size() >= MAX_SHARED_PLAYERS) {
            MessageUtils.sendMessage(player,
                    "Maximum de joueurs atteint! (" + MAX_SHARED_PLAYERS + ")", COLOR_ERROR);
            return false;
        }

        data.getSharedPlayers().add(player.getUniqueId());
        playUpgradeEffects(loc);
        updateDisplayText(data);

        plugin.getRespawnManager().setRespawnPoint(player, loc);

        MessageUtils.sendMessage(player, "Point de réapparition partagé!", COLOR_SUCCESS);
        return true;
    }

    private void playSetupEffects(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Location effectLoc = loc.clone().add(0.5, 0.5, 0.5);

        world.spawnParticle(Particle.END_ROD, effectLoc, 30, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticle(Particle.PORTAL, effectLoc, 50, 0.3, 0.3, 0.3, 0.5);

        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
    }

    private void playUpgradeEffects(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Location effectLoc = loc.clone().add(0.5, 0.5, 0.5);

        world.spawnParticle(Particle.END_ROD, effectLoc, 30, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticle(Particle.REVERSE_PORTAL, effectLoc, 50, 0.3, 0.3, 0.3, 0.05);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, effectLoc, 20, 0.2, 0.2, 0.2, 0.05);

        world.playSound(loc, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.0f, 1.0f);
        world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f);
    }

    private void updateDisplayText(LodestoneData data) {
        TextDisplay display = data.getTextDisplay();
        if (display == null || !display.isValid()) return;

        Player owner = plugin.getServer().getPlayer(data.getOwnerUUID());
        if (owner == null) return;

        String sharedText = data.getSharedPlayers().size() > 0 ?
                " §7+" + data.getSharedPlayers().size() : "";
        display.setText("§b⚡ §f" + owner.getName() + sharedText + " §b⚡");
    }

    private boolean validSetup(Player player, Block block, int diamonds) {
        if (block.getType() != Material.LODESTONE) {
            MessageUtils.sendMessage(player, "Ce bloc n'est pas un Lodestone!", COLOR_ERROR);
            return false;
        }

        if (diamonds < MIN_DIAMONDS) {
            MessageUtils.sendMessage(player, "Minimum " + MIN_DIAMONDS + " diamants requis!", COLOR_ERROR);
            return false;
        }

        if (diamonds > MAX_DIAMONDS) {
            MessageUtils.sendMessage(player, "Maximum " + MAX_DIAMONDS + " diamants permis!", COLOR_ERROR);
            return false;
        }

        Location loc = block.getLocation();

        if (loc.getY() < MIN_Y || loc.getY() > MAX_Y) {
            MessageUtils.sendMessage(player,
                    "Position invalide! Doit être entre Y=" + MIN_Y + " et Y=" + MAX_Y, COLOR_ERROR);
            return false;
        }

        if (block.getRelative(0, 1, 0).getType() != Material.AIR) {
            MessageUtils.sendMessage(player, "Le bloc au-dessus doit être de l'air!", COLOR_ERROR);
            return false;
        }

        if (!checkDistance(loc)) {
            MessageUtils.sendMessage(player,
                    "Trop proche d'un autre point! (Min: " + MIN_DISTANCE + " blocs)", COLOR_ERROR);
            return false;
        }

        return true;
    }

    private boolean checkDistance(Location loc) {
        return activeLodestones.keySet().stream()
                .filter(existingLoc -> existingLoc.getWorld().equals(loc.getWorld()))
                .noneMatch(existingLoc ->
                        LocationUtils.isWithinDistance(existingLoc, loc, MIN_DISTANCE));
    }

    public void removeLodestone(Location loc) {
        LodestoneData data = activeLodestones.remove(loc);
        if (data == null) return;

        data.invalidate();

        playerLodestones.computeIfPresent(data.getOwnerUUID(), (uuid, locs) -> {
            locs.remove(loc);
            return locs.isEmpty() ? null : locs;
        });

        Block block = loc.getBlock();
        if (block.getType() == Material.LODESTONE) {
            block.removeMetadata("lodestone_owner", plugin);
            block.removeMetadata("lodestone_time", plugin);
        }

        removeGlowDisplays(loc);
        notifyPlayers(data);
        playRemovalEffects(loc);
    }

    private void playRemovalEffects(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        Location effectLoc = loc.clone().add(0.5, 0.5, 0.5);


        world.spawnParticle(Particle.SMOKE, effectLoc, 20, 0.3, 0.3, 0.3, 0.05);
        world.spawnParticle(Particle.PORTAL, effectLoc, 30, 0.3, 0.3, 0.3, 0.1);

        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.0f);
        world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f);
    }

    private void notifyPlayers(LodestoneData data) {
        Set<UUID> allPlayers = new HashSet<>(data.getSharedPlayers());
        allPlayers.add(data.getOwnerUUID());

        for (UUID uuid : allPlayers) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                MessageUtils.sendMessage(player, "Votre point de réapparition a expiré!", COLOR_WARN);
            }
        }
    }

    private void showGlowDisplay(Location loc, Player player) {
        playerGlowDisplays.computeIfAbsent(loc, k -> new ConcurrentHashMap<>());

        if (!playerGlowDisplays.get(loc).containsKey(player.getUniqueId())) {
            Location displayLoc = loc.clone().add(0.1, 0, 0.1);

            BlockDisplay display = loc.getWorld().spawn(displayLoc, BlockDisplay.class, d -> {
                d.setBlock(Material.LODESTONE.createBlockData());
                d.setBrightness(new Display.Brightness(15, 15));
                d.setPersistent(false);
                d.setVisibleByDefault(false);

                org.bukkit.util.Transformation transform = d.getTransformation();
                transform.getScale().set(1.2f, 1.2f, 1.2f);
                d.setTransformation(transform);

                d.setInterpolationDuration(0);
                d.setInterpolationDelay(-1);
            });

            player.showEntity(plugin, display);
            playerGlowDisplays.get(loc).put(player.getUniqueId(), display);
        }

    }
    private void removeGlowDisplays(Location loc) {
        Map<UUID, BlockDisplay> displays = playerGlowDisplays.remove(loc);
        if (displays != null) {
            displays.values().forEach(display -> {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            });
        }
    }

    private void startCleanupTask() {
        tasks.add(plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Set<Location> toRemove = new HashSet<>();

            for (Map.Entry<Location, LodestoneData> entry : activeLodestones.entrySet()) {
                if (!entry.getValue().isValid() || entry.getValue().isExpired()) {
                    toRemove.add(entry.getKey());
                }
            }

            toRemove.forEach(this::removeLodestone);
        }, 20L * 60L, 20L * 60L));
    }

    public void cleanup() {
        enabled = false;

        tasks.forEach(task -> {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        });
        tasks.clear();

        playerGlowDisplays.keySet().forEach(this::removeGlowDisplays);
        playerGlowDisplays.clear();

        activeLodestones.values().forEach(LodestoneData::invalidate);
        activeLodestones.clear();
        playerLodestones.clear();
    }

    public boolean isLodestoneActive(Location loc) {
        LodestoneData data = activeLodestones.get(loc);
        return data != null && data.isValid() && !data.isExpired();
    }

    public boolean isLodestoneOwner(Location loc, UUID uuid) {
        LodestoneData data = activeLodestones.get(loc);
        return data != null && data.isOwner(uuid);
    }

    public Set<Location> getPlayerLodestones(UUID uuid) {
        return playerLodestones.getOrDefault(uuid, Collections.emptySet());
    }

    public void removeGlowDisplay(Location loc, @NotNull UUID uuid) {
        Map<UUID, BlockDisplay> displays = playerGlowDisplays.get(loc);
        if (displays != null) {
            BlockDisplay display = displays.remove(uuid);
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
    }
    public boolean addSharedPlayer(Location location, Player player) {
        LodestoneData data = activeLodestones.get(location);
        if (data == null || !data.isValid()) {
            return false;
        }
        if (data.isOwner(player.getUniqueId())) {
            return false;
        }

        if (data.getSharedPlayers().size() >= MAX_SHARED_PLAYERS) {
            return false;
        }

        if (data.addSharedPlayer(player.getUniqueId())) {
            plugin.getRespawnManager().setRespawnPoint(player, location);
            showGlowDisplay(location, player);

            updateDisplayText(data);

            return true;
        }

        return false;
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }
}