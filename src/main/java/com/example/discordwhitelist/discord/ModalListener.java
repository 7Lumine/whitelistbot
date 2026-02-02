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

        // ホワイトリストに追加
        WhitelistManager.AddResult result = plugin.getWhitelistManager().addPlayer(mcid, discordId, isBedrock);

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
                plugin.getLogger().info("ホワイトリストに追加: " + mcid + " (Discord: " + discordId + ", Bedrock: " + isBedrock + ")");
            }
            case ALREADY_EXISTS -> {
                message = plugin.getConfig().getString("messages.admin-already-exists",
                                "⚠️ **%player%** は既にホワイトリストに登録されています。")
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
                String existingPlayer;
                if (isJava) {
                    existingPlayer = plugin.getWhitelistManager().getJavaPlayerByDiscordId(discordId);
                } else {
                    existingPlayer = plugin.getWhitelistManager().getBedrockPlayerByDiscordId(discordId);
                }
                message = plugin.getConfig().getString("messages.already-registered",
                                "⚠️ あなたは既にホワイトリストに登録されています。\n登録名: **%player%**")
                        .replace("%player%", existingPlayer != null ? existingPlayer : "不明");
            }
            default -> {
                message = "❌ エラーが発生しました。";
            }
        }

        event.reply(message).setEphemeral(true).queue();
    }
}
