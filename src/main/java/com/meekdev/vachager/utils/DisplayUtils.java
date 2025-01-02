package com.meekdev.vachager.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class DisplayUtils {

    public static TextDisplay createTextDisplay(Location loc, String text, String color) {
        TextDisplay textDisplay = loc.getWorld().spawn(
                loc.clone().add(0.5, 1.5, 0.5),
                TextDisplay.class
        );
        textDisplay.text(MessageUtils.createComponent(text, color));
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        textDisplay.setViewRange(16.0f);
        textDisplay.setSeeThrough(true);
        return textDisplay;
    }

    public static BlockDisplay createGlowDisplay(Location loc, Material material) {
        BlockDisplay glowDisplay = loc.getWorld().spawn(
                loc.clone(),
                BlockDisplay.class
        );
        glowDisplay.setBlock(material.createBlockData());
        glowDisplay.setGlowing(true);
        glowDisplay.setVisibleByDefault(false);
        glowDisplay.setTransformation(new Transformation(
                new Vector3f(-0.1f, -0.1f, -0.1f),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(1.2f, 1.2f, 1.2f),
                new AxisAngle4f(0, 0, 0, 0)
        ));
        return glowDisplay;
    }

    public static void updateTextDisplay(TextDisplay display, String text, String color) {
        display.text(MessageUtils.createComponent(text, color));
    }
}