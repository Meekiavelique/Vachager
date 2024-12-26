package com.meekdev.vachager.features.experience;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class XPBottleListener implements Listener {
    private static final int XP_PER_BOTTLE = 7;

    @EventHandler
    public void onEnchantTableInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.GLASS_BOTTLE) return;

        bottleExperience(player, item);
        event.setCancelled(true);
    }

    private void bottleExperience(Player player, ItemStack bottle) {
        int playerXP = getTotalExperience(player);

        if (playerXP < XP_PER_BOTTLE) {
            player.sendMessage("§cYou don’t have enough experience to bottle!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            return;
        }

        ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = xpBottle.getItemMeta();
        meta.setDisplayName("§aBottled Experience");
        xpBottle.setItemMeta(meta);

        bottle.setAmount(bottle.getAmount() - 1);
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(xpBottle);
        } else {
            player.getWorld().dropItem(player.getLocation(), xpBottle);
        }

        setTotalExperience(player, playerXP - XP_PER_BOTTLE);

        player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }

    private int getTotalExperience(Player player) {
        int experience = 0;
        int level = player.getLevel();

        if (level >= 0 && level <= 15) {
            experience = (int) Math.round(Math.pow(level, 2) + (6 * level));
        } else if (level > 15 && level <= 30) {
            experience = (int) Math.round((2.5 * Math.pow(level, 2)) - (40.5 * level) + 360);
        } else if (level > 30) {
            experience = (int) Math.round((4.5 * Math.pow(level, 2)) - (162.5 * level) + 2220);
        }

        return experience + Math.round(player.getExp() * player.getExpToLevel());
    }

    private void setTotalExperience(Player player, int experience) {
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0);

        while (experience > 0) {
            int expToLevel = getExpAtLevel(player.getLevel());
            experience -= expToLevel;

            if (experience >= 0) {
                player.giveExp(expToLevel);
            } else {
                experience += expToLevel;
                player.giveExp(experience);
                experience = 0;
            }
        }
    }

    private int getExpAtLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
}