package com.example.discordwhitelist.discord;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

/**
 * ボタンクリック処理リスナー
 */
public class ButtonListener extends ListenerAdapter {

    private final DiscordWhitelistPlugin plugin;

    public static final String WHITELIST_BUTTON_JAVA = "whitelist_register_java";
    public static final String WHITELIST_BUTTON_BEDROCK = "whitelist_register_bedrock";

    public ButtonListener(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        boolean isJava = buttonId.equals(WHITELIST_BUTTON_JAVA);
        boolean isBedrock = buttonId.equals(WHITELIST_BUTTON_BEDROCK);

        if (!isJava && !isBedrock) {
            return;
        }

        String discordId = event.getUser().getId();

        // 既に登録済みかチェック（修正モードかどうか判断）
        String existingPlayer = null;
        if (isJava) {
            existingPlayer = plugin.getWhitelistManager().getJavaPlayerByDiscordId(discordId);
        } else {
            existingPlayer = plugin.getWhitelistManager().getBedrockPlayerByDiscordId(discordId);
        }

        // Modalを表示
        String modalTitle;
        String inputLabel;
        String inputPlaceholder;
        String modalId;

        if (isJava) {
            if (existingPlayer != null) {
                modalTitle = plugin.getConfig().getString("messages.modal-title-java-edit", "ホワイトリスト修正 (Java版)");
            } else {
                modalTitle = plugin.getConfig().getString("messages.modal-title-java", "ホワイトリスト登録 (Java版)");
            }
            inputLabel = plugin.getConfig().getString("messages.modal-input-label-java", "Minecraft ID (Java版)");
            inputPlaceholder = plugin.getConfig().getString("messages.modal-input-placeholder-java", "例: Steve");
            modalId = ModalListener.WHITELIST_MODAL_JAVA;
        } else {
            if (existingPlayer != null) {
                modalTitle = plugin.getConfig().getString("messages.modal-title-bedrock-edit", "ホワイトリスト修正 (統合版)");
            } else {
                modalTitle = plugin.getConfig().getString("messages.modal-title-bedrock", "ホワイトリスト登録 (統合版)");
            }
            inputLabel = plugin.getConfig().getString("messages.modal-input-label-bedrock", "ゲーマータグ (Xbox/統合版)");
            inputPlaceholder = plugin.getConfig().getString("messages.modal-input-placeholder-bedrock", "例: Steve1234");
            modalId = ModalListener.WHITELIST_MODAL_BEDROCK;
        }

        // 既存のプレイヤー名をプレースホルダーに表示（修正の場合）
        String defaultValue = "";
        if (existingPlayer != null) {
            // Bedrockの場合はプレフィックスを除去
            if (isBedrock) {
                String prefix = plugin.getConfig().getString("bedrock.prefix", ".");
                if (existingPlayer.startsWith(prefix)) {
                    defaultValue = existingPlayer.substring(prefix.length());
                } else {
                    defaultValue = existingPlayer;
                }
            } else {
                defaultValue = existingPlayer;
            }
        }

        TextInput.Builder inputBuilder = TextInput.create("mcid", inputLabel, TextInputStyle.SHORT)
                .setPlaceholder(inputPlaceholder)
                .setMinLength(3)
                .setMaxLength(16)
                .setRequired(true);

        if (!defaultValue.isEmpty()) {
            inputBuilder.setValue(defaultValue);
        }

        Modal modal = Modal.create(modalId, modalTitle)
                .addActionRow(inputBuilder.build())
                .build();

        event.replyModal(modal).queue();
    }
}
