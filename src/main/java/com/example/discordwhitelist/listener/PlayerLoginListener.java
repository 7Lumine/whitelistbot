package com.example.discordwhitelist.listener;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * プレイヤーログイン制御リスナー
 */
public class PlayerLoginListener implements Listener {

    private final DiscordWhitelistPlugin plugin;

    public PlayerLoginListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();

        // ホワイトリストをチェック
        if (!plugin.getWhitelistManager().isWhitelisted(playerName)) {
            String message = plugin.getConfig().getString("messages.not-whitelisted",
                    "§cあなたはホワイトリストに登録されていません。\n§7Discordサーバーでホワイトリスト登録をしてください。");

            // カラーコード変換
            Component kickMessage = LegacyComponentSerializer.legacySection().deserialize(message);

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, kickMessage);

            plugin.getLogger().info("ホワイトリスト未登録のプレイヤーをブロック: " + playerName);
        }
    }
}
