package com.example.discordwhitelist.command;

import com.example.discordwhitelist.DiscordWhitelistPlugin;
import com.example.discordwhitelist.manager.WhitelistManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ゲーム内管理コマンド
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private final DiscordWhitelistPlugin plugin;

    public AdminCommand(DiscordWhitelistPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage("§a設定をリロードしました。");
            }
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /dwl add <プレイヤー名>");
                    return true;
                }
                String playerName = args[1];
                WhitelistManager.AddResult result = plugin.getWhitelistManager().addPlayer(playerName, null);
                switch (result) {
                    case SUCCESS -> sender.sendMessage("§a" + playerName + " をホワイトリストに追加しました。");
                    case ALREADY_EXISTS -> sender.sendMessage("§e" + playerName + " は既にホワイトリストに登録されています。");
                    case INVALID_NAME -> sender.sendMessage("§c無効なMinecraft IDです。");
                    case DISCORD_ALREADY_REGISTERED -> sender.sendMessage("§cこのDiscordアカウントは既に別のMinecraft IDで登録されています。");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c使用法: /dwl remove <プレイヤー名>");
                    return true;
                }
                String playerName = args[1];
                if (plugin.getWhitelistManager().removePlayer(playerName)) {
                    sender.sendMessage("§a" + playerName + " をホワイトリストから削除しました。");
                } else {
                    sender.sendMessage("§c" + playerName + " はホワイトリストに登録されていません。");
                }
            }
            case "list" -> {
                List<WhitelistManager.WhitelistEntry> entries = plugin.getWhitelistManager().getWhitelist();
                sender.sendMessage("§6===== ホワイトリスト (" + entries.size() + "人) =====");
                for (WhitelistManager.WhitelistEntry entry : entries) {
                    String discordInfo = entry.getDiscordId().isEmpty() ? "" : " §7(Discord: " + entry.getDiscordId() + ")";
                    sender.sendMessage("§f- " + entry.getPlayerName() + discordInfo);
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== DiscordWhitelist コマンド =====");
        sender.sendMessage("§e/dwl reload §7- 設定をリロード");
        sender.sendMessage("§e/dwl add <プレイヤー名> §7- ホワイトリストに追加");
        sender.sendMessage("§e/dwl remove <プレイヤー名> §7- ホワイトリストから削除");
        sender.sendMessage("§e/dwl list §7- ホワイトリスト一覧");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "add", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return plugin.getWhitelistManager().getWhitelist().stream()
                    .map(WhitelistManager.WhitelistEntry::getPlayerName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
