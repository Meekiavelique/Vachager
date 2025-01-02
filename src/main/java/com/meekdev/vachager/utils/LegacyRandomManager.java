package com.meekdev.vachager.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class LegacyRandomManager {
    private static Method getHandleMethod;
    private static Field randomField;
    private static final ConcurrentHashMap<Integer, Random> entityRandoms = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    static {
        try {

            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftEntityClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftEntity");

            getHandleMethod = craftEntityClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);


            Class<?> nmsEntityClass = getHandleMethod.invoke(craftEntityClass.cast(
                    Bukkit.getServer().getWorlds().get(0).getEntities().stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No entities found"))
            )).getClass().getSuperclass();

            for (Field field : nmsEntityClass.getDeclaredFields()) {
                if (field.getType() == Random.class) {
                    randomField = field;
                    randomField.setAccessible(true);
                    break;
                }
            }

            if (randomField == null) {
                throw new IllegalStateException("Could not find random field");
            }

            initialized = true;
            Bukkit.getLogger().info("LegacyRandomManager initialized successfully");
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to initialize LegacyRandomManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateEntityRandom(Entity entity, Long seed) {
        if (!initialized || entity == null) {
            return;
        }

        try {
            Object nmsEntity = getHandleMethod.invoke(entity);
            Random currentRandom = (Random) randomField.get(nmsEntity);

            if (currentRandom == null || seed != null) {
                Random newRandom = new Random();
                if (seed != null) {
                    newRandom.setSeed(seed);
                }
                randomField.set(nmsEntity, newRandom);
                entityRandoms.put(entity.getEntityId(), newRandom);

                if (seed != null) {
                    Bukkit.getLogger().info("Updated random for entity " + entity.getType() +
                            " (ID: " + entity.getEntityId() + ") with seed " + seed);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to update random for entity " +
                    entity.getType() + ": " + e.getMessage());
        }
    }

    public static void cleanup(Entity entity) {
        if (entity != null) {
            entityRandoms.remove(entity.getEntityId());
        }
    }

    public static void cleanupAll() {
        entityRandoms.clear();
    }
}