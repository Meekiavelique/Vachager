package com.meekdev.vachager.features.qol;

import com.meekdev.vachager.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Comparator;

public class XPBottleListener implements Listener {
    private static final int XP_PER_BOTTLE = 7;

    @EventHandler
    public void onXPBottle(XPBottleEvent event) {
        bottleExperience(event.getPlayer(), event.getBottle());
    }

    @EventHandler
    public void onBottleBreak(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof ThrownExpBottle)) return;

        Location hitLocation = event.getEntity().getLocation();
        Player nearestPlayer = hitLocation.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(hitLocation) <= 3)
                .min(Comparator.comparingDouble(p -> p.getLocation().distance(hitLocation)))
                .orElse(null);

        if (nearestPlayer != null) {
            nearestPlayer.giveExp(XP_PER_BOTTLE);
        }
    }

    private void bottleExperience(Player player, ItemStack bottle) {
        int playerXP = PlayerUtils.getTotalExperience(player);

        if (playerXP < XP_PER_BOTTLE) {
            player.sendMessage("§cVous n'avez pas assez d'expérience à embouteiller !");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            return;
        }

        ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = xpBottle.getItemMeta();
        meta.setDisplayName("§aExpérience embouteillée");
        xpBottle.setItemMeta(meta);

        bottle.setAmount(bottle.getAmount() - 1);
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(xpBottle);
        } else {
            player.getWorld().dropItem(player.getLocation(), xpBottle);
        }

        PlayerUtils.setTotalExperience(player, playerXP - XP_PER_BOTTLE);

        player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }
}