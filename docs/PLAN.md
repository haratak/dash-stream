# 実装計画: 個人用 Android Auto で YouTube を見る

目的は **Android Auto の車載画面で YouTube を見られること**。そのために必要な 2 本の Android アプリをスクラッチで作ります。

| アプリ（仮称） | 役割 | 参考にしたもの |
| --- | --- | --- |
| `aa-tube` | 車載画面で YouTube を表示・再生する | CarStream / Fermata Auto |
| `aa-installer` | `aa-tube` を Android Auto に表示される形でインストール・更新する | AAAD |

> 停車中の利用を前提とします。走行中の動画表示は安全上・法令上 NG なので、車速による映像ブロックを必須要件にします。

---

## 1. 前提となる Android Auto の制約

1. **Play ストア以外のアプリは車載画面に出ない**
   回避策は次の 3 つ。
   - (A) Android Auto の開発者設定で「提供元不明のアプリ」を有効化（最も簡単）
   - (B) インストーラ名を `com.android.vending` にしてインストール（`pm install -i com.android.vending`）。一般アプリには権限がないので **Shizuku** を使う
   - (C) PC から `adb install -i com.android.vending app.apk`（アプリ不要の最低限の方法）

   まずは (A) または (C) で `aa-tube` を動かし、`aa-installer` で (B) を自動化します。

2. **自由に描画できるのはナビゲーションアプリの `Surface` だけ**
   公式 Car App Library（`androidx.car.app`）はテンプレート UI のみですが、ナビゲーションカテゴリのアプリには描画用の `Surface` が渡されます。ここに動画や WebView を映します。非公式 SDK（`CarActivity`）方式は Android Auto の内部実装に依存して壊れやすいので採用しません。

---

## 2. YouTube の再生方式

| 方式 | 内容 | 長所 | 短所 |
| --- | --- | --- | --- |
| **① WebView 方式（採用・主方式）** | 車載 Surface 上の `WebView` で `m.youtube.com` を開く | 公式サイトそのものなので壊れにくい。ログイン・履歴・登録チャンネルがそのまま使える | タッチ操作が車載画面向きではない。広告が出る |
| ② ネイティブ再生方式（任意・後回し） | NewPipe Extractor でストリーム URL を取得し ExoPlayer で再生 | 車向けの大きなボタン UI を作れる。広告なし | YouTube 側の変更で頻繁に壊れる。YouTube の利用規約に反する |

**① で完成させ、② は必要なら追加する**方針にします。どちらも「車載 Surface に View を映す」基盤は共通です。

---

## 3. 技術スタック

| 領域 | 採用技術 |
| --- | --- |
| 言語 / ビルド | Kotlin, Gradle (Kotlin DSL), Version Catalog |
| スマホ側 UI | Jetpack Compose + Material 3 |
| 車載側 | `androidx.car.app`（`NavigationTemplate` + `SurfaceCallback`） |
| YouTube 表示 | `WebView`（`VirtualDisplay` + `Presentation` 経由で車載 Surface に描画） |
| メディア操作 | Media3 `MediaSession`（ステアリングスイッチで再生/停止） |
| インストール | `PackageInstaller`, Shizuku API |
| 通信 | OkHttp + kotlinx.serialization（GitHub Releases API） |
| 設定 | DataStore |
| テスト | JUnit, Android Auto **Desktop Head Unit (DHU)** |
| minSdk | 26 |

---

## 4. リポジトリ構成

```
/
├─ settings.gradle.kts
├─ gradle/libs.versions.toml
├─ core/
│  ├─ car-surface/   # Surface → VirtualDisplay → Presentation, タッチ変換, 停車判定
│  └─ installer/     # GitHub Releases 取得, PackageInstaller / Shizuku
├─ app-tube/         # YouTube 視聴アプリ
└─ app-installer/    # インストーラ
```

---

## 5. フェーズ別計画

難易度は ★（低）〜 ★★★（高）。

### Phase 0: 開発環境 ★

- Android Studio と SDK、**DHU（Android Auto Desktop Head Unit）** を用意
- スマホの Android Auto で開発者モードを有効化 →「ヘッドユニットサーバーを起動」
- `adb forward tcp:5277 tcp:5277` の後に DHU を起動し、PC 上に車載画面を出す
- Gradle の雛形と CI（`./gradlew lint assembleDebug`）を作る

**完了条件**: 空の Car App が DHU に表示される。

### Phase 1: 車載画面に任意の View を映す（`core/car-surface`） ★★★

最も難しく、かつ YouTube 表示の土台になる部分。

1. `CarAppService` をナビゲーションカテゴリで宣言
   ```xml
   <service android:name=".TubeCarAppService" android:exported="true">
     <intent-filter>
       <action android:name="androidx.car.app.CarAppService" />
       <category android:name="androidx.car.app.category.NAVIGATION" />
     </intent-filter>
   </service>
   ```
2. `AppManager.setSurfaceCallback()` で `SurfaceContainer`（Surface, 幅, 高さ, DPI）を受け取る
3. `DisplayManager.createVirtualDisplay(..., surface, ...)` で Surface を出力先とする仮想ディスプレイを作る
4. 仮想ディスプレイ上に `Presentation` を出し、中に任意の View を置く
5. `SurfaceCallback.onClick / onScroll / onFling / onScale` を `MotionEvent` に変換して View に渡す
6. `CarHardwareManager` から車速を購読し、走行中は映像を黒いカバーで隠す（音声は継続）

**完了条件**: DHU 上で Compose の画面が表示され、タップ・スクロールが効く。車速シミュレーションで走行状態にすると映像が隠れる。

### Phase 2: `aa-tube`（YouTube 視聴） ★★

1. Presentation 内に `WebView` を配置し `https://m.youtube.com` を開く
   - JavaScript / DOM Storage 有効、`mediaPlaybackRequiresUserGesture = false`
   - Cookie を永続化してログイン状態を保持（ログインはスマホ側の画面で行う）
2. 車載画面上部の ActionStrip: 戻る / ホーム / 再生・一時停止 / 全画面
   - 再生操作は `evaluateJavascript` で `<video>` 要素を直接操作する
3. **スマホから送る**: スマホの YouTube アプリの「共有」→ `aa-tube` を選ぶと、その動画が車載画面で開く（車載画面での文字入力を避けるため）
4. 検索: 音声入力（車載テンプレートの検索）→ 検索結果ページを開く
5. `MediaSession` 連携で、ステアリングスイッチや AA の標準ボタンから再生/停止
6. 画面の見やすさ: ズーム倍率・ダークモードの設定（スマホ側の設定画面）

**完了条件**: DHU と実車（停車中）で、YouTube の動画を開いて再生・停止・シークでき、スマホから共有した動画が車載画面で再生される。

### Phase 3: `aa-installer`（AAAD 相当） ★★

`aa-tube` を毎回 PC から入れなくて済むようにする。

1. `aa-tube` を GitHub Actions でビルドし、GitHub Releases に APK を置く
2. GitHub API `GET /repos/{owner}/{repo}/releases/latest` で最新版を取得し、インストール済みのバージョンと比較
3. APK をダウンロード（進捗表示）
4. インストール
   - Shizuku あり: `pm install -i com.android.vending -r <apk>`（Android Auto に表示される）
   - Shizuku なし: 通常の `PackageInstaller` でインストールし、AA の「提供元不明のアプリ」設定を案内
5. `WorkManager` で定期的に更新をチェックして通知

**完了条件**: ボタン 1 つで `aa-tube` の最新版が入り、`adb shell pm list packages -i` でインストーラが `com.android.vending` になっている。

### Phase 4（任意）: ネイティブ再生方式 ★★

WebView の操作性に不満がある場合のみ。NewPipe Extractor で動画情報とストリーム URL を取得し、ExoPlayer の映像を車載 Surface に直接出力する。車向けの大きなボタン UI を自作できる。

---

## 6. 進め方

```
Phase 0 ──► Phase 1 ──► Phase 2（ここで YouTube が見られる）──► Phase 3 ──► Phase 4（任意）
```

Phase 2 までは、PC からの `adb install -i com.android.vending` で実機に入れて確認します。

---

## 7. リスクと対策

| リスク | 対策 |
| --- | --- |
| Android Auto の更新で、ナビゲーションカテゴリの Surface 描画が制限される | 描画部分を `core/car-surface` に閉じ込め、別方式に差し替えられるようにする |
| Android Auto の更新で提供元不明アプリの扱いが変わる | Play ストア名義でのインストール（Shizuku / adb）を主経路にする |
| YouTube の UI 変更で、JavaScript による再生操作が効かなくなる | 操作は `<video>` 要素の標準 API のみに依存させる |
| WebView のタッチ操作が車載画面で使いにくい | スマホからの共有を主な動画選択手段にする。それでも不満なら Phase 4 |
| 走行中の視聴 | 車速による映像ブロックを無効化できない仕様にする |

---

## 8. 最初の一歩

1. Gradle の雛形（`core/car-surface`, `app-tube`）を作る
2. DHU をセットアップし、空の `CarAppService`（ナビゲーションカテゴリ）を表示する
3. Surface → VirtualDisplay → Presentation で単色の画面を出す
4. その画面を `WebView`（`m.youtube.com`）に置き換え、タップ操作を通す
