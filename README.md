# Personal Android Auto YouTube

Android Auto の車載画面で YouTube（`m.youtube.com`）を表示する個人用アプリ **AA Tube** と、GitHub Releases から APK を入れる **AA Installer** です。

## モジュール

| モジュール | 説明 |
| --- | --- |
| `app-tube` | 車載 YouTube（WebView + Car App Navigation Surface） |
| `app-installer` | GitHub Releases から `app-tube` を取得してインストール |
| `core/car-surface` | Surface → VirtualDisplay → Presentation、タッチ |
| `core/installer` | GitHub API、PackageInstaller、Shizuku |

## ビルド

```bash
./gradlew :app-tube:assembleDebug :app-installer:assembleDebug
```

APK:

- `app-tube/build/outputs/apk/debug/app-tube-debug.apk`
- `app-installer/build/outputs/apk/debug/app-installer-debug.apk`

`app-installer` を使う前に `app-installer/build.gradle.kts` の `TUBE_REPO_OWNER` / `TUBE_REPO_NAME` を自分の GitHub リポジトリに変更してください。

## 実機へのインストール（Android Auto に表示させる）

**推奨（PC + adb）:**

```bash
adb install -i com.android.vending app-tube/build/outputs/apk/debug/app-tube-debug.apk
```

**代替:** Android Auto 開発者設定で「提供元不明のアプリ」を有効化し、通常インストール。

**Shizuku:** `aa-installer` アプリから Play ストア名義でインストール可能（Shizuku 起動・権限付与が必要）。

## 使い方

1. スマホに AA Tube をインストールし、Android Auto を接続（または DHU）。
2. 車載ランチャーから **YouTube（AA Tube）** を開く。
3. 動画 URL は **YouTube アプリ → 共有 → AA Tube** が最も簡単。
4. 車載画面の ActionStrip: 戻る / ホーム / 再生・停止 / 検索。
5. 走行中も車載 Surface に映像を表示します。**運転中の動画視聴は法令・安全上禁止される場合があります。自己責任で利用してください。**

## DHU（Desktop Head Unit）での開発

1. Android Auto 開発者モード →「ヘッドユニットサーバーを起動」
2. `adb forward tcp:5277 tcp:5277`
3. SDK の `desktop-head-unit` を起動
4. AA Tube を `-i com.android.vending` でインストール

詳細な背景は [docs/PLAN.md](docs/PLAN.md) を参照。
