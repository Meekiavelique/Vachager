package com.meekdev.vachager.features.pvp;

import com.meekdev.vachager.VachagerSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NewPlayerImmunityListener implements Listener {
    private final VachagerSMP plugin;
    private final Set<UUID> immunePlayers;
    private static final int IMMUNITY_DURATION_MINUTES = 30;
    private static final int IMMUNITY_DURATION_TICKS = IMMUNITY_DURATION_MINUTES * 60 * 20;

    public NewPlayerImmunityListener(VachagerSMP plugin) {
        this.plugin = plugin;
        this.immunePlayers = new HashSet<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            grantImmunity(player);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !immunePlayers.contains(victim.getUniqueId())) {
            return;
        }

        if (event.getDamager() instanceof Player attacker) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>Ce joueur est immunisé contre les dégâts PvP!</gradient>"
            ));
        }

    }

    private void grantImmunity(Player player) {

        immunePlayers.add(player.getUniqueId());


        player.sendMessage(plugin.getMiniMessage().deserialize(
                "<gradient:#00FF00:#00FFAA>Vous avez " + IMMUNITY_DURATION_MINUTES + " minutes d'immunité PvP!</gradient>"
        ));

        new BukkitRunnable() {
            @Override
            public void run() {
                removeImmunity(player);
            }
        }.runTaskLater(plugin, IMMUNITY_DURATION_TICKS);
    }

    private void removeImmunity(Player player) {
        immunePlayers.remove(player.getUniqueId());

        if (player.isOnline()) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<gradient:#FF0000:#FF6600>Votre immunité PvP est terminée!</gradient>"
            ));
        }
    }
}