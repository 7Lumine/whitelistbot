package com.example.discordwhitelist.discord;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
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
                            new ModalListener(plugin))
                    .build();

            jda.awaitReady();

            // スラッシュコマンドの登録
            registerCommands();

            plugin.getLogger().info("Discord Botが起動しました: " + jda.getSelfUser().getName());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Discord Botの起動に失敗しました", e);
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
     * Botを停止
     */
    public void shutdown() {
        if (jda != null) {
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
