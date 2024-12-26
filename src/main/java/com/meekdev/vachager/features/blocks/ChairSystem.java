package com.meekdev.vachager.features.blocks;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChairSystem implements Listener, CommandExecutor {
    private final VachagerSMP plugin;
    private final Map<UUID, ArmorStand> sittingPlayers = new HashMap<>();
    private boolean enabled = true;

    public ChairSystem(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getCommand("chairs").setExecutor(this);
        registerListener();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vachager.chairs.toggle")) {
            sender.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>You don't have permission!</gradient>"
            ));
            return true;
        }

        enabled = !enabled;
        if (enabled) {
            registerListener();
            sender.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#00FF00:#00FFAA>Chair system enabled!</gradient>"
            ));
        } else {
            HandlerList.unregisterAll(this);
            unsitAll();
            sender.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>Chair system disabled!</gradient>"
            ));
        }
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (!(block.getBlockData() instanceof Stairs stairs)) return;

        Player player = event.getPlayer();
        if (sittingPlayers.containsKey(player.getUniqueId())) {
            unsitPlayer(player);
            return;
        }

        if (!isValidChair(block, stairs)) return;

        sitPlayer(player, block);
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unsitPlayer(event.getPlayer());
    }

    private boolean isValidChair(Block block, Stairs stairs) {
        return stairs.getHalf() == Bisected.Half.BOTTOM &&
                block.getRelative(BlockFace.UP).getType() == Material.AIR;
    }

    private void sitPlayer(Player player, Block block) {
        ArmorStand chair = block.getWorld().spawn(
                block.getLocation().add(0.5, -0.2, 0.5),
                ArmorStand.class
        );

        chair.setGravity(false);
        chair.setVisible(false);
        chair.setSmall(true);
        chair.addPassenger(player);

        sittingPlayers.put(player.getUniqueId(), chair);
    }


    private void unsitPlayer(Player player) {
        ArmorStand chair = sittingPlayers.remove(player.getUniqueId());
        if (chair != null) {
            player.teleport(chair.getLocation().add(0, 0.5, 0));
            chair.remove();
        }
    }

    private void unsitAll() {
        new HashMap<>(sittingPlayers).keySet()
                .forEach(uuid -> {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player != null) {
                        unsitPlayer(player);
                    }
                });
    }

    private void registerListener() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void disableChairs() {
        enabled = false;
        HandlerList.unregisterAll(this);
        unsitAll();
    }
}