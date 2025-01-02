package com.meekdev.vachager.features.qol;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class XPBottleEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ItemStack bottle;

    public XPBottleEvent(Player player, ItemStack bottle) {
        this.player = player;
        this.bottle = bottle;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getBottle() {
        return bottle;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}