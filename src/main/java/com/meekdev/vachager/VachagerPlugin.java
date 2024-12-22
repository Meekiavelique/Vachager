package com.meekdev.vachager;

import com.meekdev.vachager.chat.ChatManager;
import com.meekdev.vachager.performance.ChunkManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VachagerPlugin extends JavaPlugin {

    private ChunkManager chunkManager;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.chunkManager = new ChunkManager(this);
        this.chatManager = new ChatManager(this);

        getLogger().info("Vachager Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Vachager Plugin disabled successfully!");
    }


    public ChunkManager getChunkManager() {
        return chunkManager;
    }


    public ChatManager getChatManager() {
        return chatManager;
    }
}