package com.meekdev.vachager.utils;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownUtils {
    private static final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    public static void setCooldown(String cooldownName, Player player, int seconds) {
        cooldowns.computeIfAbsent(cooldownName, k -> new ConcurrentHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public static boolean isOnCooldown(String cooldownName, Player player) {
        Map<UUID, Long> cooldownMap = cooldowns.get(cooldownName);
        if (cooldownMap == null) return false;

        Long cooldownUntil = cooldownMap.get(player.getUniqueId());
        return cooldownUntil != null && cooldownUntil > System.currentTimeMillis();
    }

    public static long getRemainingCooldown(String cooldownName, Player player) {
        Map<UUID, Long> cooldownMap = cooldowns.get(cooldownName);
        if (cooldownMap == null) return 0;

        Long cooldownUntil = cooldownMap.get(player.getUniqueId());
        if (cooldownUntil == null) return 0;

        long remainingTime = cooldownUntil - System.currentTimeMillis();
        return Math.max(0, remainingTime / 1000);
    }

    public static void removeCooldown(String cooldownName, Player player) {
        Map<UUID, Long> cooldownMap = cooldowns.get(cooldownName);
        if (cooldownMap != null) {
            cooldownMap.remove(player.getUniqueId());
        }
    }
}