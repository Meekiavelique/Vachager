package com.meekdev.vachager.core.commands.home;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeCommand implements CommandExecutor {
    private final VachagerSMP plugin;
    private final Map<UUID, Long> combatTagged = new HashMap<>();
    private static final int COMBAT_TAG_DURATION = 30; // seconds

    public HomeCommand(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getCommand("home").setExecutor(this);
        plugin.getCommand("sethome").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("sethome")) {
            return setHome(player);
        } else if (command.getName().equalsIgnoreCase("home")) {
            return teleportHome(player);
        }

        return false;
    }

    private boolean setHome(Player player) {
        Location loc = player.getLocation();
        String world = loc.getWorld().getName();

        if (!isAllowedWorld(world)) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>You cannot set home in this world!</gradient>"
            ));
            return true;
        }

        saveHome(player, loc);
        player.sendMessage(plugin.getMiniMessage().deserialize(
                "<gradient:#00FF00:#00FFAA>Home location set!</gradient>"
        ));
        return true;
    }

    private boolean teleportHome(Player player) {
        if (isInCombat(player)) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>You cannot teleport while in combat!</gradient>"
            ));
            return true;
        }

        Location home = getHome(player);
        if (home == null) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>You haven't set a home yet!</gradient>"
            ));
            return true;
        }

        player.teleport(home);
        player.sendMessage(plugin.getMiniMessage().deserialize(
                "<gradient:#00FF00:#00FFAA>Welcome home!</gradient>"
        ));
        return true;
    }

    private void saveHome(Player player, Location loc) {
        FileConfiguration config = plugin.getConfig();
        String path = "homes." + player.getUniqueId();

        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());

        plugin.saveConfig();
    }

    private Location getHome(Player player) {
        FileConfiguration config = plugin.getConfig();
        String path = "homes." + player.getUniqueId();

        if (!config.contains(path)) return null;

        return new Location(
                plugin.getServer().getWorld(config.getString(path + ".world")),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }

    public void tagPlayer(Player player) {
        combatTagged.put(player.getUniqueId(), System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                combatTagged.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, COMBAT_TAG_DURATION * 20L);
    }

    private boolean isInCombat(Player player) {
        Long tagTime = combatTagged.get(player.getUniqueId());
        if (tagTime == null) return false;

        return (System.currentTimeMillis() - tagTime) < (COMBAT_TAG_DURATION * 1000);
    }

    private boolean isAllowedWorld(String worldName) {
        return !worldName.toLowerCase().contains("nether") &&
                !worldName.toLowerCase().contains("the_end");
    }
}