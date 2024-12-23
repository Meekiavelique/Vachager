package com.meekdev.vachager.chat;

import com.meekdev.vachager.VachagerSMP;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.users.CarbonPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatManager implements Listener {
    private final VachagerSMP plugin;
    private final List<Pattern> blockedPatterns;
    private CarbonChat carbonChat;

    public ChatManager(VachagerSMP plugin) {
        this.plugin = plugin;
        this.blockedPatterns = new ArrayList<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadBlockedPatterns();
        setupCarbonChat();
    }

    private void setupCarbonChat() {
        try {
            this.carbonChat = CarbonChatProvider.carbonChat();
            plugin.getLogger().info("Successfully hooked into CarbonChat!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook into CarbonChat: " + e.getMessage());
        }
    }

    private void loadBlockedPatterns() {
        List<String> patterns = plugin.getConfig().getStringList("chat.blocked-patterns");
        for (String pattern : patterns) {
            try {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid regex pattern: " + pattern);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (isMessageBlocked(event.getMessage())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYour message was blocked due to containing restricted content.");
        }
    }

    public boolean isMessageBlocked(String message) {
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }

    public void addBlockedPattern(String pattern) {
        try {
            blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            List<String> patterns = plugin.getConfig().getStringList("chat.blocked-patterns");
            patterns.add(pattern);
            plugin.getConfig().set("chat.blocked-patterns", patterns);
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add blocked pattern: " + e.getMessage());
        }
    }

    public void removeBlockedPattern(String pattern) {
        blockedPatterns.removeIf(p -> p.pattern().equals(pattern));
        List<String> patterns = plugin.getConfig().getStringList("chat.blocked-patterns");
        patterns.remove(pattern);
        plugin.getConfig().set("chat.blocked-patterns", patterns);
        plugin.saveConfig();
    }

    public List<String> getBlockedPatterns() {
        List<String> patterns = new ArrayList<>();
        for (Pattern pattern : blockedPatterns) {
            patterns.add(pattern.pattern());
        }
        return patterns;
    }
}