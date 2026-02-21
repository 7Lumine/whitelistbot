package com.example.discordwhitelist.listener;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
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
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.minecraft-to-discord", true))
            return;

        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Webhook経由で送信 (プレイヤーアバター付き)
        plugin.getDiscordBot().sendWebhookMessage(playerName, escapeMarkdown(message));
    }

    /**
     * プレイヤー参加をDiscordに通知
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.join-leave-messages", true))
            return;

        String playerName = event.getPlayer().getName();

        String format = plugin.getConfig()
                .getString("chat-sync.formats.join-to-discord", "📥 **%player%** がサーバーに参加しました")
                .replace("%player%", escapeMarkdown(playerName));

        plugin.getDiscordBot().sendChatMessage(format);
    }

    /**
     * プレイヤー退出をDiscordに通知
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.join-leave-messages", true))
            return;

        String playerName = event.getPlayer().getName();

        String format = plugin.getConfig()
                .getString("chat-sync.formats.leave-to-discord", "📤 **%player%** がサーバーから退出しました")
                .replace("%player%", escapeMarkdown(playerName));

        plugin.getDiscordBot().sendChatMessage(format);
    }

    /**
     * プレイヤーのデスメッセージをDiscordに送信
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.death-messages", true))
            return;

        String deathMessage = PlainTextComponentSerializer.plainText().serialize(event.deathMessage() != null
                ? event.deathMessage()
                : net.kyori.adventure.text.Component.text(event.getEntity().getName() + " died"));

        String format = plugin.getConfig().getString("chat-sync.formats.death-to-discord", "💀 %message%")
                .replace("%message%", escapeMarkdown(deathMessage));

        plugin.getDiscordBot().sendChatMessage(format);
    }

    /**
     * 実績解除をDiscordに通知
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.advancement-messages", true))
            return;

        // レシピ解除は無視 (minecraft:recipes/ で始まるもの)
        String advancementKey = event.getAdvancement().getKey().toString();
        if (advancementKey.contains("recipes/"))
            return;

        // 表示名がないものは無視 (隠し進捗の一部)
        if (event.getAdvancement().getDisplay() == null)
            return;

        String playerName = event.getPlayer().getName();
        String advancementTitle = PlainTextComponentSerializer.plainText()
                .serialize(event.getAdvancement().getDisplay().title());

        String format = plugin.getConfig()
                .getString("chat-sync.formats.advancement-to-discord", "🏆 **%player%** が実績 **%advancement%** を達成しました！")
                .replace("%player%", escapeMarkdown(playerName))
                .replace("%advancement%", escapeMarkdown(advancementTitle));

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
