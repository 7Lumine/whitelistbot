# Discord Whitelist Plugin

Minecraft Java版 Paper/Spigot 1.21.x対応のホワイトリスト管理プラグインです。
Discordサーバーのメンバーが自分でホワイトリストに登録できます。

## 機能

- **セルフ登録**: Discordでボタンをクリック → Modal入力でMCIDを登録
- **自動ブロック**: ホワイトリスト未登録プレイヤーはサーバーに接続不可
- **管理機能**: Discord/ゲーム内から管理者がホワイトリストを操作可能
- **Discord ID紐付け**: 1つのDiscordアカウントにつき1つのMCIDのみ登録可能

## 必要環境

- Java 21以上
- Paper/Spigot 1.21.1
- Discord Bot Token

## インストール

### 1. ビルド

```bash
mvn clean package
```

`target/discord-whitelist-1.0.0.jar` が生成されます。

### 2. プラグイン配置

生成されたJARファイルをサーバーの `plugins/` フォルダにコピーします。

### 3. Discord Bot設定

1. [Discord Developer Portal](https://discord.com/developers/applications) でアプリケーションを作成
2. Botを追加し、Tokenを取得
3. OAuth2 > URL Generator で以下の権限を選択してURLを生成:
   - Scopes: `bot`, `applications.commands`
   - Bot Permissions: `Send Messages`, `Use Slash Commands`
4. 生成されたURLでBotをサーバーに招待

### 4. config.yml設定

`plugins/DiscordWhitelist/config.yml` を編集:

```yaml
discord:
  token: "your-bot-token-here"
  guild-id: "your-server-id"
  admin-role-id: "admin-role-id"
```

### 5. サーバー再起動

```
/dwl reload
```
または、サーバーを再起動してください。

## 使い方

### Discord

1. 管理者が `/setup-whitelist` を実行して登録ボタンを設置
2. ユーザーがボタンをクリックしてMCIDを入力
3. 登録完了！

#### 管理者コマンド

- `/whitelist add <player>` - プレイヤーを追加
- `/whitelist remove <player>` - プレイヤーを削除
- `/whitelist list` - 一覧表示

### ゲーム内

- `/dwl add <player>` - プレイヤーを追加
- `/dwl remove <player>` - プレイヤーを削除
- `/dwl list` - 一覧表示
- `/dwl reload` - 設定リロード

## 権限

- `discordwhitelist.admin` - 管理コマンド使用権限 (デフォルト: OP)
- `discordwhitelist.bypass` - ホワイトリストバイパス (デフォルト: OP)

## ファイル構成

```
plugins/DiscordWhitelist/
├── config.yml      # 設定ファイル
└── whitelist.yml   # ホワイトリストデータ
```

## ライセンス

MIT License
