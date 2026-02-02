package com.example.discordwhitelist.discord;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import com.example.discordwhitelist.manager.WhitelistManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

/**
 * スラッシュコマンド処理リスナー
 */
public class SlashCommandListener extends ListenerAdapter {

    private final DiscordWhitelistPlugin plugin;

    public SlashCommandListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "setup-whitelist" -> handleSetupWhitelist(event);
            case "whitelist" -> handleWhitelist(event);
        }
    }

    /**
     * /setup-whitelist コマンド処理
     */
    private void handleSetupWhitelist(SlashCommandInteractionEvent event) {
        // 管理者チェック
        if (!isAdmin(event.getMember())) {
            event.reply("❌ このコマンドは管理者のみ実行できます。").setEphemeral(true).queue();
            return;
        }

        String title = plugin.getConfig().getString("messages.button-title", "🎮 Minecraftホワイトリスト登録");
        String description = plugin.getConfig().getString("messages.button-description",
                "下のボタンを押してMinecraft IDを入力すると、サーバーに参加できるようになります。");
        String buttonLabelJava = plugin.getConfig().getString("messages.button-label-java", "☕ Java版で登録");
        String buttonLabelBedrock = plugin.getConfig().getString("messages.button-label-bedrock", "🪨 統合版で登録");

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(new Color(88, 101, 242)) // Discord Blurple
                .addField("☕ Java版", "PC (Windows/Mac/Linux) でプレイする方", true)
                .addField("🪨 統合版", "スマホ/Switch/Xbox/PS でプレイする方", true);

        Button javaButton = Button.success(ButtonListener.WHITELIST_BUTTON_JAVA, buttonLabelJava);
        Button bedrockButton = Button.primary(ButtonListener.WHITELIST_BUTTON_BEDROCK, buttonLabelBedrock);

        // コマンド使用者には非公開で確認メッセージを送信
        event.reply("✅ ホワイトリスト登録ボタンを設置しました。").setEphemeral(true).queue();

        // チャンネルに通常のメッセージとして送信（コマンド使用が表示されない）
        event.getChannel().sendMessageEmbeds(embed.build())
                .addActionRow(javaButton, bedrockButton)
                .queue();

        plugin.getLogger().info("ホワイトリスト登録ボタンを設置: チャンネル " + event.getChannel().getName());
    }

    /**
     * /whitelist コマンド処理
     */
    private void handleWhitelist(SlashCommandInteractionEvent event) {
        // 管理者チェック
        if (!isAdmin(event.getMember())) {
            event.reply("❌ このコマンドは管理者のみ実行できます。").setEphemeral(true).queue();
            return;
        }

        String subCommand = event.getSubcommandName();
        if (subCommand == null)
            return;

        switch (subCommand) {
            case "add" -> {
                String playerName = event.getOption("player").getAsString();
                WhitelistManager.AddResult result = plugin.getWhitelistManager().addPlayer(playerName, null);

                String message;
                switch (result) {
                    case SUCCESS -> message = plugin.getConfig().getString("messages.admin-added",
                            "✅ **%player%** をホワイトリストに追加しました。")
                            .replace("%player%", playerName);
                    case ALREADY_EXISTS -> message = plugin.getConfig().getString("messages.admin-already-exists",
                            "⚠️ **%player%** は既にホワイトリストに登録されています。")
                            .replace("%player%", playerName);
                    case INVALID_NAME -> message = plugin.getConfig().getString("messages.invalid-name-java",
                            "❌ 無効なMinecraft IDです。正しいIDを入力してください。");
                    default -> message = "❌ エラーが発生しました。";
                }

                event.reply(message).setEphemeral(true).queue();
            }
            case "remove" -> {
                String playerName = event.getOption("player").getAsString();
                boolean removed = plugin.getWhitelistManager().removePlayer(playerName);

                String message;
                if (removed) {
                    message = plugin.getConfig().getString("messages.admin-removed",
                            "✅ **%player%** をホワイトリストから削除しました。")
                            .replace("%player%", playerName);
                } else {
                    message = plugin.getConfig().getString("messages.admin-not-found",
                            "❌ **%player%** はホワイトリストに登録されていません。")
                            .replace("%player%", playerName);
                }

                event.reply(message).setEphemeral(true).queue();
            }
            case "list" -> {
                List<WhitelistManager.WhitelistEntry> entries = plugin.getWhitelistManager().getWhitelist();

                if (entries.isEmpty()) {
                    event.reply("📋 ホワイトリストは空です。").setEphemeral(true).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("📋 **ホワイトリスト一覧** (").append(entries.size()).append("人)\n\n");

                int count = 0;
                for (WhitelistManager.WhitelistEntry entry : entries) {
                    if (count >= 20) {
                        sb.append("\n... 他 ").append(entries.size() - 20).append("人");
                        break;
                    }

                    String edition = entry.isBedrock() ? "🪨" : "☕";
                    sb.append(edition).append(" `").append(entry.getPlayerName()).append("`");
                    if (!entry.getDiscordId().isEmpty()) {
                        sb.append(" (<@").append(entry.getDiscordId()).append(">)");
                    }
                    sb.append("\n");
                    count++;
                }

                event.reply(sb.toString()).setEphemeral(true).queue();
            }
        }
    }

    /**
     * 管理者かどうかチェック
     */
    private boolean isAdmin(Member member) {
        if (member == null)
            return false;

        // サーバー管理者権限を持っている場合
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }

        // 設定された管理者ロールを持っている場合
        String adminRoleId = plugin.getConfig().getString("discord.admin-role-id", "");
        if (!adminRoleId.isEmpty() && !adminRoleId.equals("ADMIN_ROLE_ID")) {
            for (Role role : member.getRoles()) {
                if (role.getId().equals(adminRoleId)) {
                    return true;
                }
            }
        }

        return false;
    }
}
