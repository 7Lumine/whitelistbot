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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Discord Bot 管理クラス
 */
public class DiscordBot {

    private final DiscordWhitelistPlugin plugin;
    private JDA jda;
    private TextChannel chatChannel;
    private String webhookUrl;

    // プレイヤーのスキンヘッドURL (mc-heads.net)
    private static final String AVATAR_URL_TEMPLATE = "https://mc-heads.net/avatar/%s/64";

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

            // Webhook URLの取得
            reloadWebhook();

            plugin.getLogger().info("Discord Botが起動しました: " + jda.getSelfUser().getName());

            // プレイヤー人数を表示
            updatePlayerCount();

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
     * Webhook URLをリロード
     */
    public void reloadWebhook() {
        webhookUrl = plugin.getConfig().getString("chat-sync.webhook-url", "");
        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL")) {
            webhookUrl = null;
            plugin.getLogger().info("Webhook URLが未設定です。通常のBotメッセージで送信します。");
        } else {
            plugin.getLogger().info("Webhook URLが設定されています。プレイヤーアバター付きで送信します。");
        }
        initChatChannel();
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
                chatChannel.sendMessage(message).complete();
            } catch (Exception e) {
                plugin.getLogger().warning("サーバー停止メッセージの送信に失敗: " + e.getMessage());
            }
        }
    }

    /**
     * チャットメッセージをDiscordに送信 (通常メッセージ)
     */
    public void sendChatMessage(String message) {
        if (chatChannel != null) {
            chatChannel.sendMessage(message).queue();
        }
    }

    /**
     * Webhookを使ってプレイヤーアバター付きでメッセージを送信
     *
     * @param playerName プレイヤー名 (アバター取得用)
     * @param message    送信するメッセージ
     */
    public void sendWebhookMessage(String playerName, String message) {
        if (webhookUrl == null) {
            // Webhook未設定の場合は通常メッセージにフォールバック
            sendChatMessage(message);
            return;
        }

        // 非同期でWebhookを送信
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String avatarUrl = String.format(AVATAR_URL_TEMPLATE, playerName);

                // JSONペイロードを構築
                String jsonPayload = String.format(
                        "{\"username\":\"%s\",\"avatar_url\":\"%s\",\"content\":\"%s\"}",
                        escapeJson(playerName),
                        escapeJson(avatarUrl),
                        escapeJson(message));

                HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 429) {
                    // Rate Limited
                    plugin.getLogger().warning("Webhook rate limited. メッセージが送信できませんでした。");
                } else if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("Webhook送信エラー: HTTP " + responseCode);
                }

                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Webhook送信に失敗: " + e.getMessage());
            }
        });
    }

    /**
     * Webhookを使ってシステムメッセージを送信（カスタム名+アバター）
     *
     * @param displayName 表示名
     * @param avatarUrl   アバターURL
     * @param message     送信するメッセージ
     */
    public void sendWebhookSystemMessage(String displayName, String avatarUrl, String message) {
        if (webhookUrl == null) {
            sendChatMessage(message);
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String jsonPayload = String.format(
                        "{\"username\":\"%s\",\"avatar_url\":\"%s\",\"content\":\"%s\"}",
                        escapeJson(displayName),
                        escapeJson(avatarUrl),
                        escapeJson(message));

                HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Webhook送信に失敗: " + e.getMessage());
            }
        });
    }

    /**
     * JSON文字列のエスケープ
     */
    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Botを停止
     */
    public void shutdown() {
        if (jda != null) {
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

    /**
     * プレイヤー人数をBotステータスに表示
     */
    public void updatePlayerCount() {
        if (jda == null)
            return;
        int count = plugin.getServer().getOnlinePlayers().size();
        int max = plugin.getServer().getMaxPlayers();
        String status = plugin.getConfig().getString("chat-sync.formats.bot-status", "Minecraft | %online%/%max%人")
                .replace("%online%", String.valueOf(count))
                .replace("%max%", String.valueOf(max));
        jda.getPresence().setActivity(Activity.playing(status));
    }

    /**
     * プレイヤーのアバターURL取得
     */
    public static String getAvatarUrl(String playerName) {
        return String.format(AVATAR_URL_TEMPLATE, playerName);
    }
}
