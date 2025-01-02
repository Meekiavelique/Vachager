package com.meekdev.vachager.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerUtils {

    public static void consumeItem(Player player, ItemStack item, int amount) {
        if (item.getAmount() <= amount) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - amount);
        }
    }

    public static int getTotalExperience(Player player) {
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

    public static void setTotalExperience(Player player, int experience) {
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

    private static int getExpAtLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }
}