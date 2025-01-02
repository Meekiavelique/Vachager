package com.meekdev.vachager.core.listeners;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.features.blocks.ChairSystem;
import com.meekdev.vachager.features.blocks.DragonEggListener;
import com.meekdev.vachager.features.blocks.PistonListener;
import com.meekdev.vachager.features.pvp.NewPlayerImmunityListener;
import com.meekdev.vachager.features.pvp.PvPManager;
import com.meekdev.vachager.features.qol.BatDropListener;
import com.meekdev.vachager.features.qol.XPBottleListener;
import com.meekdev.vachager.features.respawn.LodestoneListener;
import com.meekdev.vachager.features.respawn.LodestoneManager;
import com.meekdev.vachager.features.respawn.RespawnManager;
import com.meekdev.vachager.features.worlds.BorderExpansionListener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public class ListenerManager {
    private final VachagerSMP plugin;
    private final List<Listener> listeners;

    public ListenerManager(VachagerSMP plugin) {
        this.plugin = plugin;
        this.listeners = new ArrayList<>();
        initializeListeners();
    }

    public void initializeListeners() {
        listeners.add(new ChairSystem(plugin));
        listeners.add(new DragonEggListener(plugin));
        listeners.add(new PistonListener());
        listeners.add(new NewPlayerImmunityListener(plugin));
        listeners.add(new PvPManager());
        listeners.add(new BatDropListener());
        listeners.add(new XPBottleListener());
        LodestoneManager lodestoneManager = new LodestoneManager(plugin);
        RespawnManager respawnManager = plugin.getRespawnManager();
        listeners.add(new LodestoneListener(plugin, lodestoneManager, respawnManager));
        listeners.add(new BorderExpansionListener());
        listeners.add(new EventListener(plugin));
    }

    public void registerListeners() {
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    public void unregisterListeners() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }
}