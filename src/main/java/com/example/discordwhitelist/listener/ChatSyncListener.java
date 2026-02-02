package com.example.discordwhitelist.listener;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * チャット同期リスナー
 * Minecraft → Discord のメッセージ送信を処理
 */
public class ChatSyncListener implements Listener {

    private final DiscordWhitelistPlugin plugin;

    public ChatSyncListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * プレイヤーのチャットをDiscordに送信
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false)) return;
        if (!plugin.getConfig().getBoolean("chat-sync.minecraft-to-discord", true)) return;

        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        String format = plugin.getConfig().getString("chat-sync.formats.chat-to-discord", "**%player%**: %message%")
                .replace("%player%", escapeMarkdown(playerName))
                .replace("%message%", escapeMarkdown(message));

        plugin.getDiscordBot().sendChatMessage(format);
    }

    /**
     * プレイヤー参加をDiscordに通知
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false)) return;
        if (!plugin.getConfig().getBoolean("chat-sync.join-leave-messages", true)) return;

        String playerName = event.getPlayer().getName();

        String format = plugin.getConfig().getString("chat-sync.formats.join-to-discord", "📥 **%player%** がサーバーに参加しました")
                .replace("%player%", escapeMarkdown(playerName));

        plugin.getDiscordBot().sendChatMessage(format);
    }

    /**
     * プレイヤー退出をDiscordに通知
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false)) return;
        if (!plugin.getConfig().getBoolean("chat-sync.join-leave-messages", true)) return;

        String playerName = event.getPlayer().getName();

        String format = plugin.getConfig().getString("chat-sync.formats.leave-to-discord", "📤 **%player%** がサーバーから退出しました")
                .replace("%player%", escapeMarkdown(playerName));

        plugin.getDiscordBot().sendChatMessage(format);
    }

    /**
     * Discordマークダウンをエスケープ
     */
    private String escapeMarkdown(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace("|", "\\|")
                .replace(">", "\\>");
    }
}
