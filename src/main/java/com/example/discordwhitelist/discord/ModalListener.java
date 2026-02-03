package com.example.discordwhitelist.discord;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import com.example.discordwhitelist.manager.WhitelistManager;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Modal送信処理リスナー
 */
public class ModalListener extends ListenerAdapter {

    private final DiscordWhitelistPlugin plugin;

    public static final String WHITELIST_MODAL_JAVA = "whitelist_modal_java";
    public static final String WHITELIST_MODAL_BEDROCK = "whitelist_modal_bedrock";

    public ModalListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();

        boolean isJava = modalId.equals(WHITELIST_MODAL_JAVA);
        boolean isBedrock = modalId.equals(WHITELIST_MODAL_BEDROCK);

        if (!isJava && !isBedrock) {
            return;
        }

        String mcid = event.getValue("mcid").getAsString().trim();
        String discordId = event.getUser().getId();

        // 既存の登録があるかチェック
        String existingPlayer = isJava
                ? plugin.getWhitelistManager().getJavaPlayerByDiscordId(discordId)
                : plugin.getWhitelistManager().getBedrockPlayerByDiscordId(discordId);

        WhitelistManager.AddResult result;
        if (existingPlayer != null) {
            // 修正モード
            result = plugin.getWhitelistManager().updatePlayer(mcid, discordId, isBedrock);
        } else {
            // 新規登録モード
            result = plugin.getWhitelistManager().addPlayer(mcid, discordId, isBedrock);
        }

        String message;
        switch (result) {
            case SUCCESS -> {
                if (isJava) {
                    message = plugin.getConfig().getString("messages.success-java",
                            "✅ **%player%** をホワイトリストに登録しました！Java版でサーバーに参加できます。")
                            .replace("%player%", mcid);
                } else {
                    String prefix = plugin.getConfig().getString("bedrock.prefix", ".");
                    message = plugin.getConfig().getString("messages.success-bedrock",
                            "✅ **%player%** をホワイトリストに登録しました！統合版でサーバーに参加できます。")
                            .replace("%player%", prefix + mcid);
                }
                plugin.getLogger()
                        .info("ホワイトリストに追加: " + mcid + " (Discord: " + discordId + ", Bedrock: " + isBedrock + ")");
            }
            case UPDATED -> {
                if (isJava) {
                    message = plugin.getConfig().getString("messages.updated-java",
                            "✅ Minecraft IDを **%player%** に変更しました！")
                            .replace("%player%", mcid);
                } else {
                    String prefix = plugin.getConfig().getString("bedrock.prefix", ".");
                    message = plugin.getConfig().getString("messages.updated-bedrock",
                            "✅ ゲーマータグを **%player%** に変更しました！")
                            .replace("%player%", prefix + mcid);
                }
                plugin.getLogger()
                        .info("ホワイトリストを更新: " + mcid + " (Discord: " + discordId + ", Bedrock: " + isBedrock + ")");
            }
            case ALREADY_EXISTS -> {
                message = plugin.getConfig().getString("messages.name-already-taken",
                        "⚠️ **%player%** は既に他のユーザーが使用しています。")
                        .replace("%player%", mcid);
            }
            case INVALID_NAME -> {
                if (isJava) {
                    message = plugin.getConfig().getString("messages.invalid-name-java",
                            "❌ 無効なMinecraft IDです。正しいIDを入力してください。(英数字と_のみ、3-16文字)");
                } else {
                    message = plugin.getConfig().getString("messages.invalid-name-bedrock",
                            "❌ 無効なゲーマータグです。正しいタグを入力してください。(英数字とスペースのみ、3-16文字)");
                }
            }
            case DISCORD_ALREADY_REGISTERED -> {
                // This case shouldn't happen now since we check and use updatePlayer
                message = "❌ エラーが発生しました。";
            }
            default -> {
                message = "❌ エラーが発生しました。";
            }
        }

        event.reply(message).setEphemeral(true).queue();
    }
}
