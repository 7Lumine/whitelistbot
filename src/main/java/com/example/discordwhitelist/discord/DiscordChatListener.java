package com.example.discordwhitelist.discord;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Discord → Minecraft チャット同期リスナー
 */
public class DiscordChatListener extends ListenerAdapter {

    private final DiscordWhitelistPlugin plugin;

    public DiscordChatListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Bot自身のメッセージは無視
        if (event.getAuthor().isBot()) return;

        // チャット同期が無効の場合は無視
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false)) return;
        if (!plugin.getConfig().getBoolean("chat-sync.discord-to-minecraft", true)) return;

        // 指定されたチャンネル以外は無視
        String channelId = plugin.getConfig().getString("chat-sync.channel-id", "");
        if (channelId.isEmpty() || !event.getChannel().getId().equals(channelId)) return;

        String userName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        String message = event.getMessage().getContentDisplay();

        // 空メッセージは無視
        if (message.isEmpty()) return;

        // フォーマット適用
        String format = plugin.getConfig().getString("chat-sync.formats.chat-to-minecraft", "§9[Discord]§r §b%user%§r: %message%")
                .replace("%user%", userName)
                .replace("%message%", message);

        // Minecraftに送信（メインスレッドで実行）
        Bukkit.getScheduler().runTask(plugin, () -> {
            Component component = LegacyComponentSerializer.legacySection().deserialize(format);
            Bukkit.getServer().sendMessage(component);
        });
    }
}
