package com.example.discordwhitelist.discord;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.logging.Level;

/**
 * Discord Bot 管理クラス
 */
public class DiscordBot {

    private final DiscordWhitelistPlugin plugin;
    private JDA jda;
    private TextChannel chatChannel;

    public DiscordBot(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Botを起動
     */
    public void start() {
        String token = plugin.getConfig().getString("discord.token", "");

        try {
            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("Minecraft"))
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(
                            new SlashCommandListener(plugin),
                            new ButtonListener(plugin),
                            new ModalListener(plugin),
                            new DiscordChatListener(plugin))
                    .build();

            jda.awaitReady();

            // スラッシュコマンドの登録
            registerCommands();

            // チャット同期チャンネルの取得
            initChatChannel();

            plugin.getLogger().info("Discord Botが起動しました: " + jda.getSelfUser().getName());

            // サーバー起動通知
            sendServerStartMessage();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Discord Botの起動に失敗しました", e);
        }
    }

    /**
     * チャット同期チャンネルを初期化
     */
    private void initChatChannel() {
        String channelId = plugin.getConfig().getString("chat-sync.channel-id", "");
        if (!channelId.isEmpty() && !channelId.equals("YOUR_CHAT_CHANNEL_ID")) {
            chatChannel = jda.getTextChannelById(channelId);
            if (chatChannel != null) {
                plugin.getLogger().info("チャット同期チャンネル: #" + chatChannel.getName());
            } else {
                plugin.getLogger().warning("チャット同期チャンネルが見つかりません: " + channelId);
            }
        }
    }

    /**
     * スラッシュコマンドを登録
     */
    private void registerCommands() {
        String guildId = plugin.getConfig().getString("discord.guild-id", "");

        if (guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID")) {
            plugin.getLogger().warning("Guild IDが設定されていません。コマンドをグローバルに登録します。");

            jda.updateCommands().addCommands(
                    Commands.slash("setup-whitelist", "ホワイトリスト登録ボタンを設置します"),
                    Commands.slash("whitelist", "ホワイトリスト管理コマンド")
                            .addSubcommands(
                                    new SubcommandData("add", "プレイヤーをホワイトリストに追加")
                                            .addOption(OptionType.STRING, "player", "Minecraft ID", true),
                                    new SubcommandData("remove", "プレイヤーをホワイトリストから削除")
                                            .addOption(OptionType.STRING, "player", "Minecraft ID", true),
                                    new SubcommandData("list", "ホワイトリスト一覧を表示")))
                    .queue();
        } else {
            jda.getGuildById(guildId).updateCommands().addCommands(
                    Commands.slash("setup-whitelist", "ホワイトリスト登録ボタンを設置します"),
                    Commands.slash("whitelist", "ホワイトリスト管理コマンド")
                            .addSubcommands(
                                    new SubcommandData("add", "プレイヤーをホワイトリストに追加")
                                            .addOption(OptionType.STRING, "player", "Minecraft ID", true),
                                    new SubcommandData("remove", "プレイヤーをホワイトリストから削除")
                                            .addOption(OptionType.STRING, "player", "Minecraft ID", true),
                                    new SubcommandData("list", "ホワイトリスト一覧を表示")))
                    .queue();

            plugin.getLogger().info("ギルド " + guildId + " にコマンドを登録しました。");
        }
    }

    /**
     * サーバー起動メッセージを送信
     */
    private void sendServerStartMessage() {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.server-status-messages", true))
            return;

        String message = plugin.getConfig().getString("chat-sync.formats.server-start", "🟢 **サーバーが起動しました**");
        sendChatMessage(message);
    }

    /**
     * サーバー停止メッセージを送信
     */
    public void sendServerStopMessage() {
        if (!plugin.getConfig().getBoolean("chat-sync.enabled", false))
            return;
        if (!plugin.getConfig().getBoolean("chat-sync.server-status-messages", true))
            return;

        String message = plugin.getConfig().getString("chat-sync.formats.server-stop", "🔴 **サーバーが停止しました**");

        if (chatChannel != null) {
            try {
                // 同期的に送信（シャットダウン時）
                chatChannel.sendMessage(message).complete();
            } catch (Exception e) {
                plugin.getLogger().warning("サーバー停止メッセージの送信に失敗: " + e.getMessage());
            }
        }
    }

    /**
     * チャットメッセージをDiscordに送信
     */
    public void sendChatMessage(String message) {
        if (chatChannel != null) {
            chatChannel.sendMessage(message).queue();
        }
    }

    /**
     * Botを停止
     */
    public void shutdown() {
        if (jda != null) {
            // サーバー停止通知
            sendServerStopMessage();

            jda.shutdown();
            plugin.getLogger().info("Discord Botを停止しました。");
        }
    }

    /**
     * JDAインスタンスを取得
     */
    public JDA getJDA() {
        return jda;
    }
}
