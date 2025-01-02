package com.meekdev.vachager.features.respawn;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RespawnManager {
    private final VachagerSMP plugin;
    private final Map<UUID, RespawnPoint> respawnPoints;
    private final int minutesPerDiamond;

    public RespawnManager(VachagerSMP plugin) {
        this.plugin = plugin;
        this.respawnPoints = new HashMap<>();
        this.minutesPerDiamond = plugin.getConfigManager().getConfig("config.yml").getInt("respawn.minutes-per-diamond", 5);
        loadRespawnPoints();
    }

    public void setRespawnPoint(Player player, Location location) {
        Block block = location.getBlock();

        if (block.getType() != Material.LODESTONE) {
            return;
        }

        ItemStack diamondStack = player.getInventory().getItemInMainHand();
        if (diamondStack.getType() != Material.DIAMOND) {
            return;
        }

        int diamonds = diamondStack.getAmount();
        int minutes = diamonds * minutesPerDiamond;

        diamondStack.setAmount(diamondStack.getAmount() - diamonds);
        player.getInventory().setItemInMainHand(diamondStack);

        RespawnPoint respawnPoint = new RespawnPoint(location, System.currentTimeMillis() + (minutes * 60 * 1000));
        respawnPoints.put(player.getUniqueId(), respawnPoint);
        saveRespawnPoint(player.getUniqueId(), respawnPoint);
    }

    public Location getRespawnLocation(Player player) {
        RespawnPoint point = respawnPoints.get(player.getUniqueId());
        if (point == null || !point.isValid()) return null;

        Location loc = point.getLocation();
        if (!isValidRespawnLocation(loc)) return null;

        return loc.clone().add(0.5, 1, 0.5);
    }
    private boolean isValidRespawnLocation(Location loc) {
        return loc != null &&
                loc.getBlock().getType() == Material.LODESTONE &&
                plugin.getLodestoneManager().isLodestoneActive(loc) &&
                loc.getBlock().getRelative(0, 1, 0).getType() == Material.AIR &&
                loc.getBlock().getRelative(0, 2, 0).getType() == Material.AIR;
    }

    private void loadRespawnPoints() {
        var config = plugin.getConfigManager().getConfig("player_data.yml");
        if (config.getConfigurationSection("respawns") == null) return;

        for (String uuidStr : config.getConfigurationSection("respawns").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String worldName = config.getString("respawns." + uuidStr + ".world");
            double x = config.getDouble("respawns." + uuidStr + ".x");
            double y = config.getDouble("respawns." + uuidStr + ".y");
            double z = config.getDouble("respawns." + uuidStr + ".z");
            long expiry = config.getLong("respawns." + uuidStr + ".expiry");

            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            respawnPoints.put(uuid, new RespawnPoint(loc, expiry));
        }
    }

    private void saveRespawnPoint(UUID uuid, RespawnPoint point) {
        var config = plugin.getConfigManager().getConfig("player_data.yml");
        Location loc = point.getLocation();

        config.set("respawns." + uuid + ".world", loc.getWorld().getName());
        config.set("respawns." + uuid + ".x", loc.getX());
        config.set("respawns." + uuid + ".y", loc.getY());
        config.set("respawns." + uuid + ".z", loc.getZ());
        config.set("respawns." + uuid + ".expiry", point.getExpiryTime());

        plugin.getConfigManager().saveConfig("player_data.yml");
    }
    public void handlePlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        RespawnPoint point = respawnPoints.get(player.getUniqueId());

        if (point != null && point.isValid()) {
            Location loc = point.getLocation();
            if (loc.getBlock().getType() == Material.LODESTONE) {
                Location respawnLoc = loc.clone().add(0.5, 1, 0.5);
                event.setRespawnLocation(respawnLoc);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    LodestoneListener.playRespawnEffects(loc, player);
                }, 1L);
            }
        }
    }
}