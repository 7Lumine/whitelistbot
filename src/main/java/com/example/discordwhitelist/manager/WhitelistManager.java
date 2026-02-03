package com.example.discordwhitelist.manager;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ホワイトリスト管理クラス
 */
public class WhitelistManager {

    private final DiscordWhitelistPlugin plugin;
    private final File whitelistFile;
    private FileConfiguration whitelistConfig;

    // Minecraft名 -> WhitelistEntry
    private final Map<String, WhitelistEntry> whitelist = new HashMap<>();

    // Discord ID -> Minecraft名 (Java版)
    private final Map<String, String> discordToJava = new HashMap<>();

    // Discord ID -> Minecraft名 (Bedrock版)
    private final Map<String, String> discordToBedrock = new HashMap<>();

    public WhitelistManager(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
        this.whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");
        load();
    }

    /**
     * ホワイトリストを読み込み
     */
    public void load() {
        if (!whitelistFile.exists()) {
            try {
                whitelistFile.getParentFile().mkdirs();
                whitelistFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("whitelist.ymlの作成に失敗しました: " + e.getMessage());
            }
        }

        whitelistConfig = YamlConfiguration.loadConfiguration(whitelistFile);
        whitelist.clear();
        discordToJava.clear();
        discordToBedrock.clear();

        ConfigurationSection playersSection = whitelistConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String playerName : playersSection.getKeys(false)) {
                ConfigurationSection playerSection = playersSection.getConfigurationSection(playerName);
                if (playerSection != null) {
                    String discordId = playerSection.getString("discord-id", "");
                    String registeredAt = playerSection.getString("registered-at", "");
                    boolean isBedrock = playerSection.getBoolean("bedrock", false);

                    WhitelistEntry entry = new WhitelistEntry(playerName, discordId, registeredAt, isBedrock);
                    whitelist.put(playerName.toLowerCase(), entry);

                    if (!discordId.isEmpty()) {
                        if (isBedrock) {
                            discordToBedrock.put(discordId, playerName);
                        } else {
                            discordToJava.put(discordId, playerName);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("ホワイトリストを読み込みました: " + whitelist.size() + "人");
    }

    /**
     * ホワイトリストを保存
     */
    public void save() {
        whitelistConfig.set("players", null);

        for (WhitelistEntry entry : whitelist.values()) {
            String path = "players." + entry.getPlayerName();
            whitelistConfig.set(path + ".discord-id", entry.getDiscordId());
            whitelistConfig.set(path + ".registered-at", entry.getRegisteredAt());
            whitelistConfig.set(path + ".bedrock", entry.isBedrock());
        }

        try {
            whitelistConfig.save(whitelistFile);
        } catch (IOException e) {
            plugin.getLogger().severe("whitelist.ymlの保存に失敗しました: " + e.getMessage());
        }
    }

    /**
     * リロード
     */
    public void reload() {
        load();
    }

    /**
     * プレイヤーをホワイトリストに追加 (Java版)
     */
    public AddResult addPlayer(String playerName, String discordId) {
        return addPlayer(playerName, discordId, false);
    }

    /**
     * プレイヤーをホワイトリストに追加
     *
     * @param playerName Minecraft ID
     * @param discordId  Discord ID
     * @param isBedrock  Bedrock版かどうか
     * @return 追加結果
     */
    public AddResult addPlayer(String playerName, String discordId, boolean isBedrock) {
        // Minecraft名のバリデーション
        if (isBedrock) {
            if (!isValidBedrockName(playerName)) {
                return AddResult.INVALID_NAME;
            }
        } else {
            if (!isValidJavaName(playerName)) {
                return AddResult.INVALID_NAME;
            }
        }

        // Bedrock版の場合、プレフィックスを付けて保存
        String storedName = playerName;
        if (isBedrock) {
            String prefix = plugin.getConfig().getString("bedrock.prefix", ".");
            storedName = prefix + playerName;
        }

        String lowerName = storedName.toLowerCase();

        // 既に登録済みかチェック
        if (whitelist.containsKey(lowerName)) {
            return AddResult.ALREADY_EXISTS;
        }

        // Discord IDが既に同じエディションで登録されているかチェック
        if (discordId != null && !discordId.isEmpty()) {
            if (isBedrock && discordToBedrock.containsKey(discordId)) {
                return AddResult.DISCORD_ALREADY_REGISTERED;
            }
            if (!isBedrock && discordToJava.containsKey(discordId)) {
                return AddResult.DISCORD_ALREADY_REGISTERED;
            }
        }

        // 登録
        String registeredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        WhitelistEntry entry = new WhitelistEntry(storedName, discordId != null ? discordId : "", registeredAt,
                isBedrock);
        whitelist.put(lowerName, entry);

        if (discordId != null && !discordId.isEmpty()) {
            if (isBedrock) {
                discordToBedrock.put(discordId, storedName);
            } else {
                discordToJava.put(discordId, storedName);
            }
        }

        save();
        return AddResult.SUCCESS;
    }

    /**
     * プレイヤーのMinecraft IDを更新
     *
     * @param newPlayerName 新しいMinecraft ID
     * @param discordId     Discord ID
     * @param isBedrock     Bedrock版かどうか
     * @return 更新結果
     */
    public AddResult updatePlayer(String newPlayerName, String discordId, boolean isBedrock) {
        // Minecraft名のバリデーション
        if (isBedrock) {
            if (!isValidBedrockName(newPlayerName)) {
                return AddResult.INVALID_NAME;
            }
        } else {
            if (!isValidJavaName(newPlayerName)) {
                return AddResult.INVALID_NAME;
            }
        }

        // 古いエントリーを取得して削除
        String oldPlayerName = isBedrock ? discordToBedrock.get(discordId) : discordToJava.get(discordId);
        if (oldPlayerName != null) {
            whitelist.remove(oldPlayerName.toLowerCase());
            if (isBedrock) {
                discordToBedrock.remove(discordId);
            } else {
                discordToJava.remove(discordId);
            }
        }

        // Bedrock版の場合、プレフィックスを付けて保存
        String storedName = newPlayerName;
        if (isBedrock) {
            String prefix = plugin.getConfig().getString("bedrock.prefix", ".");
            storedName = prefix + newPlayerName;
        }

        String lowerName = storedName.toLowerCase();

        // 新しい名前が既に使用されているかチェック
        if (whitelist.containsKey(lowerName)) {
            return AddResult.ALREADY_EXISTS;
        }

        // 登録
        String registeredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        WhitelistEntry entry = new WhitelistEntry(storedName, discordId, registeredAt, isBedrock);
        whitelist.put(lowerName, entry);

        if (isBedrock) {
            discordToBedrock.put(discordId, storedName);
        } else {
            discordToJava.put(discordId, storedName);
        }

        save();
        return AddResult.UPDATED;
    }

    /**
     * プレイヤーをホワイトリストから削除
     *
     * @param playerName Minecraft ID
     * @return 削除できたかどうか
     */
    public boolean removePlayer(String playerName) {
        String lowerName = playerName.toLowerCase();
        WhitelistEntry entry = whitelist.remove(lowerName);

        if (entry != null) {
            if (!entry.getDiscordId().isEmpty()) {
                if (entry.isBedrock()) {
                    discordToBedrock.remove(entry.getDiscordId());
                } else {
                    discordToJava.remove(entry.getDiscordId());
                }
            }
            save();
            return true;
        }

        return false;
    }

    /**
     * プレイヤーがホワイトリストに登録されているかチェック
     * Floodgateプレフィックスを考慮
     *
     * @param playerName Minecraft ID (Floodgateプレフィックス付きの場合あり)
     * @return 登録されているかどうか
     */
    public boolean isWhitelisted(String playerName) {
        return whitelist.containsKey(playerName.toLowerCase());
    }

    /**
     * Discord IDでJava版プレイヤー名を取得
     */
    public String getJavaPlayerByDiscordId(String discordId) {
        return discordToJava.get(discordId);
    }

    /**
     * Discord IDでBedrock版プレイヤー名を取得
     */
    public String getBedrockPlayerByDiscordId(String discordId) {
        return discordToBedrock.get(discordId);
    }

    /**
     * Discord IDでプレイヤー名を取得 (Java版優先)
     * 
     * @deprecated Use getJavaPlayerByDiscordId or getBedrockPlayerByDiscordId
     */
    @Deprecated
    public String getPlayerByDiscordId(String discordId) {
        String java = discordToJava.get(discordId);
        if (java != null)
            return java;
        return discordToBedrock.get(discordId);
    }

    /**
     * ホワイトリスト一覧を取得
     *
     * @return エントリーのリスト
     */
    public List<WhitelistEntry> getWhitelist() {
        return new ArrayList<>(whitelist.values());
    }

    /**
     * ホワイトリストの人数を取得
     */
    public int getSize() {
        return whitelist.size();
    }

    /**
     * Java版Minecraft名のバリデーション
     */
    private boolean isValidJavaName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        return name.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Bedrock版ゲーマータグのバリデーション
     * スペースを含むことができる
     */
    private boolean isValidBedrockName(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        // Bedrockのゲーマータグは英数字とスペースを含むことができる
        return name.matches("^[a-zA-Z0-9_ ]+$");
    }

    /**
     * 追加結果
     */
    public enum AddResult {
        SUCCESS,
        UPDATED,
        ALREADY_EXISTS,
        INVALID_NAME,
        DISCORD_ALREADY_REGISTERED
    }

    /**
     * ホワイトリストエントリー
     */
    public static class WhitelistEntry {
        private final String playerName;
        private final String discordId;
        private final String registeredAt;
        private final boolean bedrock;

        public WhitelistEntry(String playerName, String discordId, String registeredAt, boolean bedrock) {
            this.playerName = playerName;
            this.discordId = discordId;
            this.registeredAt = registeredAt;
            this.bedrock = bedrock;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getDiscordId() {
            return discordId;
        }

        public String getRegisteredAt() {
            return registeredAt;
        }

        public boolean isBedrock() {
            return bedrock;
        }
    }
}
