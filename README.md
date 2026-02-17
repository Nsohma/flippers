## 課題
・現状のシステムだと、エクセルをユーザが直接操作する必要があるが、ユーザはエクセルに触れてきていない属性の人が多く、ミスマッチが生じている 

・エクセルファイルがどのようにレジに適用されるのか、usbファイルでいちいちレジにエクセルファイルを移して、さらにレジを再起動しなければ反映されず、自身のエクセル操作がどのようにシステムに反映されるか分かりづらい 

・古いシステムからのDX化が現場の人からの強い要望として上がっている

## 目的 
レジキー・ハンディ変更の自動化webアプリケーション

## 運用例
次のような運用を考える

1. 既存のレジのエクセルファイルを提案システムにアップロード 

2. 提案システム内でアップロードされたエクセルを解釈し、実際のレジキー or ハンディでどうなっているか、実物と同じ画面を再現しUIとして表示する。 

3. そのUI上のレジキーやハンディの項目ボタンをクリックして、削除や追加などを選べるようにする 

4. UI上の操作が終わり、ユーザの求めているレジキー、ハンディの形になると、操作を終え、エクセルをエクスポートする。 

5. ユーザはエクスポートされたエクセルファイルをusbを用いてレジのPOSシステムにアップロードする

## システム構成
バック : Java Spring Frame Work

フロント : Vue.js

## 実装状況（2026-02-17時点）
現在は、以下の業務フローのうち「Import -> UI編集 -> Export」まで実装済みです。

1. Excelアップロード (`/api/pos/import`)
2. POS画面再現（上段カテゴリ + 下段グリッド）
3. ボタン同士の入れ替え（ドラッグ&ドロップ）
4. 空セルへのボタン追加（カテゴリ絞り込み -> 商品選択）
5. ボタン削除
6. ボタン価格変更（`ItemMaster.UnitPrice` 反映）
7. 上段カテゴリの追加・削除
8. Excelエクスポート (`/api/pos/drafts/{draftId}/export`)
9. エクスポート時は `PresetMenuMaster` と `PresetMenuButtonMaster` を再構築し、`ItemMaster.UnitPrice` を更新

## アーキテクチャ概要
この実装は、**ドメインモデル (`PosConfig`) を中心に置いた凹型レイヤ（ヘキサゴナル寄り）** の構成です。

- 依存方向は原則として `controller -> service(usecase) -> model/port <- dao(adapter)`。
- ドメイン層（`model`）は Spring や POI に依存しません。
- Excel入出力・永続化は `service.port` のインターフェース越しに差し替え可能です。

### レイヤ責務
- `controller`: HTTP受け口。DTO変換、入力チェック、レスポンス整形。
- `service`: ユースケース実行。ドメイン操作の調停、リポジトリ保存、例外制御。
- `model`: 業務ルール本体（カテゴリ/ページ/ボタン操作）。
- `service.port`: 外部I/Oの抽象境界。
- `dao`: `port` の実装（Apache POI / ファイル保存）。

## Spring構成と主要クラス
### 1. Controller層
- `spring/src/main/java/com/example/demo/controller/PosImportController.java`
  - Import API。アップロードファイルを `ImportPosUseCase` に渡し、初期カテゴリ/初期ページを返却。
- `spring/src/main/java/com/example/demo/controller/PosDraftController.java`
  - Draft編集API群（ページ取得、swap、追加、削除、価格変更、カテゴリ追加削除、export）。
- `spring/src/main/java/com/example/demo/controller/GlobalExceptionHandler.java`
  - 例外を `ErrorResponse` へ統一変換（400/404/500）。
- `spring/src/main/java/com/example/demo/controller/CorsConfig.java`
  - Vue開発環境 (`http://localhost:5173`) 向け CORS 設定。

### 2. UseCase/Service層
- `spring/src/main/java/com/example/demo/service/ImportPosService.java`
  - Excel読込、`PosConfig` 生成、`PosDraft` 保存、`draftId` 発行。
- `spring/src/main/java/com/example/demo/service/GetPageService.java`
  - UI編集ユースケースを集約。
  - `swapButtons`, `addButton`, `deleteButton`, `updateUnitPrice`, `addCategory`, `deleteCategory` を実行し、更新後ドラフトを保存。
- `spring/src/main/java/com/example/demo/service/ExportPosService.java`
  - ドラフト取得後、`PosConfigExporter` でExcelへ反映。

### 3. Domain層（中心）
- `spring/src/main/java/com/example/demo/model/PosConfig.java`
  - ドメイン集約ルート。
  - 上段カテゴリ (`Category`) と下段ページ (`Page`) を保持。
  - 主要操作は `swapButtons`, `addButton`, `deleteButton`, `updateUnitPrice`, `addCategory`, `deleteCategory`。
- `spring/src/main/java/com/example/demo/model/PosDraft.java`
  - `draftId + PosConfig + originalExcelBytes` の編集セッション単位。
- `spring/src/main/java/com/example/demo/model/PosConfigSource.java`
  - ReaderがExcelから抽出した中間データ。
- `spring/src/main/java/com/example/demo/model/ItemCatalog.java`
  - 空セル追加時のカテゴリ/商品選択用カタログ。

### 4. Port層（外部境界）
- `spring/src/main/java/com/example/demo/service/port/PosConfigReader.java`
  - Excel -> `PosConfigSource`
- `spring/src/main/java/com/example/demo/service/port/PosConfigExporter.java`
  - `PosConfig` -> Excel bytes
- `spring/src/main/java/com/example/demo/service/port/DraftRepository.java`
  - ドラフト保存・取得

### 5. Adapter/DAO層
- `spring/src/main/java/com/example/demo/dao/PoiPosConfigReader.java`
  - 読み取り対象は `PresetMenuMaster`, `PresetMenuButtonMaster`, `ItemMaster`, `MDHierarchyMaster`, `POSItemMaster`。
  - `ItemCatalog`（カテゴリと商品一覧）を構築して UI 追加ダイアログに利用。
- `spring/src/main/java/com/example/demo/dao/PoiPosConfigExporter.java`
  - 書き込み対象は `PresetMenuMaster`（カテゴリ）, `PresetMenuButtonMaster`（ボタン配置）, `ItemMaster.UnitPrice`（価格変更反映）。
  - 行をクリアした上で再配置し、並び順を安定化。
- `spring/src/main/java/com/example/demo/dao/InMemoryDraftRepository.java`
  - メモリ + `.ser` ファイルへ保存（デフォルト: `${java.io.tmpdir}/flippers-drafts`）。

## Vue構成（`vue/src/App.vue`中心）
現状フロントは `App.vue` に機能を集約しています。

- `state`: `draftId`, `categories`, `page`, `selectedPageNumber`, `loading`, `error`
- `dragState`: ドラッグ中セル
- `addDialog`: 空セル追加ダイアログ
- `priceDialog`: 価格変更ダイアログ
- `categoryDialog`: カテゴリ追加ダイアログ
- Import/Export
- 上段カテゴリタブ切り替え
- 下段ボタンのドラッグ入れ替え
- 空セル追加（緑+）
- 既存ボタン削除（赤×）
- 価格変更（青-）
- カテゴリ追加/削除

## API一覧（実装済み）
- `POST /api/pos/import`
- `GET /api/pos/drafts/{draftId}/pages/{pageNumber}`
- `PATCH /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons/swap`
- `POST /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons`
- `DELETE /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons`
- `PATCH /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons/unit-price`
- `GET /api/pos/drafts/{draftId}/item-categories`
- `POST /api/pos/drafts/{draftId}/categories`
- `DELETE /api/pos/drafts/{draftId}/categories/{pageNumber}`
- `GET /api/pos/drafts/{draftId}/export`

## 凹型レイヤ観点での補足
現状の設計は、依存逆転の方向は概ね成立しています。

- 良い点: ドメイン `PosConfig` に編集ルールが集約され、UI/Excel実装が直接ルールを持たない。
- 良い点: I/O境界が `port` で抽象化されており、Reader/Exporter/Repositoryを交換しやすい。
- 今後改善余地: `GetPageService` が操作種類ごとに肥大化しつつあるため、ユースケース単位クラス分割の余地あり。
- 今後改善余地: フロントが `App.vue` 一枚に集約されているため、将来的には composable/components 分割が有効。

## 起動方法（ローカル）
### Spring
```bash
cd spring
./mvnw spring-boot:run
```

### Vue
```bash
cd vue
npm install
npm run dev
```
