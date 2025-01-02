package com.meekdev.vachager.core.chat;

import com.meekdev.vachager.VachagerSMP;
import com.meekdev.vachager.utils.MessageUtils;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
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
    private static final String COLOR_ERROR = "#ff4d2e";

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
            plugin.getLogger().info("Connexion réussie avec CarbonChat !");
        } catch (Exception e) {
            plugin.getLogger().warning("Échec de la connexion avec CarbonChat : " + e.getMessage());
        }
    }

    private void loadBlockedPatterns() {
        List<String> patterns = plugin.getConfig().getStringList("chat.blocked-patterns");
        for (String pattern : patterns) {
            try {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                plugin.getLogger().warning("Motif regex invalide : " + pattern);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (isMessageBlocked(event.getMessage())) {
            event.setCancelled(true);
            MessageUtils.sendMessage(event.getPlayer(), "Votre message a été bloqué car il contient du contenu restreint.", COLOR_ERROR);
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
            plugin.getLogger().warning("Échec de l'ajout du motif bloqué : " + e.getMessage());
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