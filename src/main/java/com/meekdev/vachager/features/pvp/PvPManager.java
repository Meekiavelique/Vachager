package com.meekdev.vachager.features.pvp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvPManager implements Listener {
    private final Map<UUID, Long> combatTimers = new HashMap<>();
    private final long combatDuration = 15000; // 15 seconds

    public void setInCombat(Player player) {
        combatTimers.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean isInCombat(Player player) {
        Long combatStart = combatTimers.get(player.getUniqueId());
        return combatStart != null && (System.currentTimeMillis() - combatStart) < combatDuration;
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player damaged = (Player) event.getEntity();

            setInCombat(damager);
            setInCombat(damaged);
        }
    }
}