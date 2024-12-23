package com.meekdev.vachager.features.respawn;

import com.meekdev.vachager.VachagerSMP;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LodestoneListener implements Listener {
    private final VachagerSMP plugin;
    private final int MINUTES_PER_DIAMOND = 5;
    private final int MAX_MINUTES = 120;

    public LodestoneListener(VachagerSMP plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onLodestoneInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                event.getClickedBlock().getType() != Material.LODESTONE) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType() != Material.DIAMOND) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>Hold diamonds to set your respawn point!</gradient>"
            ));
            return;
        }

        int diamonds = Math.min(heldItem.getAmount(), MAX_MINUTES / MINUTES_PER_DIAMOND);
        int minutes = diamonds * MINUTES_PER_DIAMOND;

        if (minutes > MAX_MINUTES) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>Maximum respawn time is 2 hours!</gradient>"
            ));
            return;
        }

        heldItem.setAmount(heldItem.getAmount() - diamonds);
        plugin.getRespawnManager().setRespawnPoint(player, event.getClickedBlock().getLocation());

        player.sendMessage(plugin.getMiniMessage().deserialize(
                "<gradient:#00FF00:#00FFAA>Respawn point set for " + minutes + " minutes using " + diamonds + " diamonds!</gradient>"
        ));
    }
}