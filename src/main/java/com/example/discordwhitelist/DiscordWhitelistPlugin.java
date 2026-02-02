package com.example.discordwhitelist;

import com.example.discordwhitelist.command.AdminCommand;
import com.example.discordwhitelist.discord.DiscordBot;
import com.example.discordwhitelist.listener.ChatSyncListener;
import com.example.discordwhitelist.listener.PlayerLoginListener;
import com.example.discordwhitelist.manager.WhitelistManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Discord連携ホワイトリストプラグイン メインクラス
 */
public class DiscordWhitelistPlugin extends JavaPlugin {

    private static DiscordWhitelistPlugin instance;
    private WhitelistManager whitelistManager;
    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        instance = this;

        // 設定ファイルの保存
        saveDefaultConfig();

        // ホワイトリストマネージャーの初期化
        whitelistManager = new WhitelistManager(this);

        // Discord Botの初期化
        String token = getConfig().getString("discord.token", "");
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            getLogger().warning("Discord Bot Tokenが設定されていません。config.ymlを編集してください。");
        } else {
            discordBot = new DiscordBot(this);
            discordBot.start();
        }

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatSyncListener(this), this);

        // コマンドの登録
        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("dwl").setExecutor(adminCommand);
        getCommand("dwl").setTabCompleter(adminCommand);

        getLogger().info("DiscordWhitelistプラグインが有効化されました。");
    }

    @Override
    public void onDisable() {
        // Discord Botの停止
        if (discordBot != null) {
            discordBot.shutdown();
        }

        // ホワイトリストの保存
        if (whitelistManager != null) {
            whitelistManager.save();
        }

        getLogger().info("DiscordWhitelistプラグインが無効化されました。");
    }

    /**
     * プラグインインスタンスを取得
     */
    public static DiscordWhitelistPlugin getInstance() {
        return instance;
    }

    /**
     * ホワイトリストマネージャーを取得
     */
    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    /**
     * Discord Botを取得
     */
    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    /**
     * 設定とホワイトリストをリロード
     */
    public void reload() {
        reloadConfig();
        whitelistManager.reload();
        getLogger().info("設定をリロードしました。");
    }
}
