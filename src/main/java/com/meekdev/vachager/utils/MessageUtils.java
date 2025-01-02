package com.meekdev.vachager.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void sendMessage(Player player, String message, String color) {
        player.sendMessage(miniMessage.deserialize(String.format("<%s>%s</%s>", color, message, color)));
    }

    public static Component createComponent(String message, String color) {
        return miniMessage.deserialize(String.format("<%s>%s</%s>", color, message, color));
    }
}