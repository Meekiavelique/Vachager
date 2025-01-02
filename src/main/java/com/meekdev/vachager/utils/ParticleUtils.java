package com.meekdev.vachager.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleUtils {

    public static void CircularParticleEffect(JavaPlugin plugin, Location center, Particle particleType, int duration) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final double radius = 0.5;

            @Override
            public void run() {
                if (ticks >= duration) {
                    this.cancel();
                    return;
                }

                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle + (ticks * 0.5)) * radius;
                    double z = Math.sin(angle + (ticks * 0.5)) * radius;
                    Location particleLoc = center.clone().add(0.5 + x, ticks * 0.1, 0.5 + z);

                    center.getWorld().spawnParticle(
                            particleType,
                            particleLoc,
                            1, 0, 0, 0, 0
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    public static void enhanceVisuals(Location loc) {
        loc.getWorld().spawnParticle(Particle.ENCHANT,
                loc.clone().add(0.5, 1, 0.5),
                15, 0.2, 0.2, 0.2, 0);

        for (double t = 0; t < Math.PI * 2; t += Math.PI / 16) {
            double x = Math.cos(t) * 0.8;
            double z = Math.sin(t) * 0.8;
            loc.getWorld().spawnParticle(Particle.END_ROD,
                    loc.clone().add(0.5 + x, 0.1, 0.5 + z),
                    1, 0, 0, 0, 0);
        }
    }
}