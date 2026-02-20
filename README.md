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
バック : Java Spring Framework

フロント : Vue.js

## 実装状況（2026-02-20時点）
現在は、業務フロー「Import -> UI編集 -> Export」に加えて、商品全カタログの閲覧・編集まで実装済みです。

### 共通実装済み
1. `draftId` ベース編集セッション管理
2. Undo / Redo
3. 編集履歴（折りたたみ、展開、クリックジャンプ、履歴削除）
4. 直前操作のハイライト表示（カテゴリ/商品/ボタン）
5. Excelエクスポート（`/api/pos/drafts/{draftId}/export`）
6. 回帰テスト自動化（入力Excel -> 操作 -> 出力Excel のgolden比較）

### 画面構成（Vue Router）
1. トップ画面 (`/`): `Import` と `Export` を集約。ここで読み込んだ `draftId` を編集画面・商品カタログで共有。
2. 編集画面 (`/pos`, `/handy`): レジキー編集とハンディ編集を同一シェル内で切り替え。
3. 商品カタログ画面 (`/catalog`): `ItemMaster` 一覧表示、カテゴリ絞り込み、検索、レコード編集。
4. サイドバー: 画面切り替えナビゲーション + 折りたたみ（☰）対応。

### レジキー実装済み
1. POS画面再現（上段カテゴリ + 下段グリッド）
2. ボタン同士の入れ替え（空セルとの入れ替え含む）
3. 空セルへのボタン追加（`MDHierarchyMaster` + `POSItemMaster` 由来カタログ）
4. ボタン削除
5. ボタン価格変更（`ItemMaster.UnitPrice` 反映）
6. 上段カテゴリの追加・削除
7. 上段カテゴリ同士のD&D並び替え
8. 上段カテゴリをゴミ箱へD&D削除
9. 下段グリッドサイズ変更（`Page(col x row)` 右側ボタン）
10. ボタン表示に `ItemCode` と価格を表示
11. カテゴリ削除演出: 「対象を空白ハイライト -> 0.5秒待機 -> 詰める」

### ハンディ実装済み
1. `CategoryMaster` / `ItemCategoryMaster` からカテゴリ・商品を表示
2. 左ペインカテゴリ、右ペイン商品の2カラム表示（それぞれ独立スクロール）
3. カテゴリ表示順: `CategoryMaster.DisplayLevel` 順
4. 商品表示順: `ItemCategoryMaster.DisplayLevel` 順
5. 商品移動（D&D、スワップではなく挿入位置ベース）
6. 商品削除（`ItemCategoryMaster` の該当行削除として反映）
7. 商品追加（カテゴリ絞り込み + 商品選択）
8. カテゴリ追加（`CategoryCode` は 10 以上の未使用最小値を自動採番）
9. カテゴリ削除
10. カテゴリ移動（D&D、スワップではなく挿入位置ベース）
11. カテゴリ/商品削除演出: 「対象を空白ハイライト -> 0.5秒待機 -> 詰める」

### 商品カタログ実装済み
1. `ItemMaster` から `ItemCode`, `ItemNamePrint`, `UnitPrice`, `CostPrice`, `BasePrice` を表示
2. カテゴリ絞り込み（`/item-categories` 由来）
3. `ItemCode` / `ItemNamePrint` 検索
4. 行クリックで編集ダイアログを開き、5項目を更新
5. 更新内容は Undo/Redo・履歴に統合
6. 更新後はレジキー画面・ハンディ画面の表示にも反映

### エクスポート反映（実装済み）
1. `PresetMenuButtonMaster` を `(PageNumber, ButtonColumnNumber, ButtonRowNumber)` 順で再構築
2. `PresetMenuMaster` を再構築
3. `ItemMaster` の `ItemCode / ItemNamePrint / UnitPrice / CostPrice / BasePrice` を編集結果で更新
4. `CategoryMaster` をハンディカテゴリ順で再構築（追加/削除/並び替え反映）
5. `ItemCategoryMaster` をハンディ編集結果で再構築（追加/削除/並び替え反映）

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
  - レジキー編集API群（ページ取得、ボタン編集、カテゴリ編集、グリッド変更、undo/redo、履歴取得・ジャンプ・削除、商品マスタ取得/更新、export）。
- `spring/src/main/java/com/example/demo/controller/HandyDraftController.java`
  - ハンディ編集API群（カテゴリ取得/追加/削除/移動、商品追加/削除/並び替え）。
- `spring/src/main/java/com/example/demo/controller/SpaForwardController.java`
  - SPA直アクセス (`/`, `/pos`, `/handy`, `/catalog`) を `index.html` へフォワード。
- `spring/src/main/java/com/example/demo/controller/GlobalExceptionHandler.java`
  - 例外を `ErrorResponse` へ統一変換（400/404/500）。
- `spring/src/main/java/com/example/demo/controller/CorsConfig.java`
  - Vue開発環境 (`http://localhost:5173`) 向け CORS 設定。

### 2. UseCase/Service層
- `spring/src/main/java/com/example/demo/service/ImportPosService.java`
  - Excel読込、`PosConfig` 生成、`PosDraft` 保存、`draftId` 発行。
- `spring/src/main/java/com/example/demo/service/GetPageService.java`
  - ページ取得専用（`getPage` のみ）。
- `spring/src/main/java/com/example/demo/service/SwapButtonsService.java`
- `spring/src/main/java/com/example/demo/service/AddButtonService.java`
- `spring/src/main/java/com/example/demo/service/DeleteButtonService.java`
- `spring/src/main/java/com/example/demo/service/UpdateUnitPriceService.java`
- `spring/src/main/java/com/example/demo/service/AddCategoryService.java`
- `spring/src/main/java/com/example/demo/service/DeleteCategoryService.java`
- `spring/src/main/java/com/example/demo/service/SwapCategoriesService.java`
- `spring/src/main/java/com/example/demo/service/UpdateCategoryGridService.java`
  - それぞれの編集操作をユースケース単位で実行し、更新後ドラフトを保存（SRP分割済み）。
- `spring/src/main/java/com/example/demo/service/GetHandyCatalogService.java`
- `spring/src/main/java/com/example/demo/service/AddHandyCategoryService.java`
- `spring/src/main/java/com/example/demo/service/DeleteHandyCategoryService.java`
- `spring/src/main/java/com/example/demo/service/ReorderHandyCategoriesService.java`
- `spring/src/main/java/com/example/demo/service/ReorderHandyItemsService.java`
- `spring/src/main/java/com/example/demo/service/AddHandyItemService.java`
- `spring/src/main/java/com/example/demo/service/DeleteHandyItemService.java`
  - ハンディ編集操作をユースケース単位で実行し、更新後ドラフトを保存。
- `spring/src/main/java/com/example/demo/service/SwapHandyCategoriesService.java`
  - 互換用のカテゴリ入れ替えユースケース（現行UIは `reorder` を使用）。
- `spring/src/main/java/com/example/demo/service/UndoDraftService.java`
- `spring/src/main/java/com/example/demo/service/RedoDraftService.java`
- `spring/src/main/java/com/example/demo/service/GetDraftHistoryService.java`
- `spring/src/main/java/com/example/demo/service/JumpDraftHistoryService.java`
- `spring/src/main/java/com/example/demo/service/ClearDraftHistoryService.java`
  - Undo/Redo/履歴取得/履歴ジャンプ/履歴削除の編集状態制御。
- `spring/src/main/java/com/example/demo/service/GetItemCatalogService.java`
  - 空セル追加用の商品カタログを返却。
- `spring/src/main/java/com/example/demo/service/GetItemMasterCatalogService.java`
  - 商品カタログ画面向けに `ItemMasterCatalog` を返却。
- `spring/src/main/java/com/example/demo/service/UpdateItemMasterService.java`
  - `ItemMaster` の1レコード更新（ItemCode/ItemNamePrint/UnitPrice/CostPrice/BasePrice）。
- `spring/src/main/java/com/example/demo/service/ExportPosService.java`
  - ドラフト取得後、`PosConfigExporter` でExcelへ反映。
- `spring/src/main/java/com/example/demo/service/DraftServiceSupport.java`
  - draft/page取得、履歴付き保存、`ItemCatalog`/`HandyCatalog`/`ItemMasterCatalog` の相互キャッシュ読込などの共通処理。

### 3. Domain層（中心）
- `spring/src/main/java/com/example/demo/model/PosConfig.java`
  - ドメイン集約ルート。
  - 上段カテゴリ (`Category`) と下段ページ (`Page`) を保持。
  - 主要操作は `swapButtons`, `addButton`, `deleteButton`, `updateUnitPrice`, `addCategory`, `deleteCategory`, `swapCategories`, `updateCategoryGrid`。
- `spring/src/main/java/com/example/demo/model/PosDraft.java`
  - `draftId + PosConfig + originalExcelBytes` の編集セッション単位。
  - 差分ベース履歴（`changes`, `historyEntries`, `historyIndex`）を保持し、`undo`, `redo`, `jumpToHistoryIndex`, `clearHistory` を提供。
  - `ItemCatalog` / `HandyCatalog` / `ItemMasterCatalog` を保持し、Excel再パース回数を抑制。
  - レジキー・ハンディ・商品マスタ編集の差分を同一履歴で管理。
- `spring/src/main/java/com/example/demo/model/PosConfigSource.java`
  - ReaderがExcelから抽出した中間データ。
- `spring/src/main/java/com/example/demo/model/ItemCatalog.java`
  - 商品カタログ/ハンディカタログ共通のカテゴリ・商品モデル。
- `spring/src/main/java/com/example/demo/model/ItemMasterCatalog.java`
  - 商品カタログ画面向けの `ItemMaster` モデル。

### 4. Port層（外部境界）
- `spring/src/main/java/com/example/demo/service/port/PosConfigReader.java`
  - Excel -> `PosConfigSource`
- `spring/src/main/java/com/example/demo/service/port/PosConfigExporter.java`
  - `PosConfig` -> Excel bytes
- `spring/src/main/java/com/example/demo/service/port/DraftRepository.java`
  - ドラフト保存・取得

### 5. Adapter/DAO層
- `spring/src/main/java/com/example/demo/dao/PoiPosConfigReader.java`
  - レジキー読取: `PresetMenuMaster`, `PresetMenuButtonMaster`, `ItemMaster`, `MDHierarchyMaster`, `POSItemMaster`。
  - ハンディ読取: `CategoryMaster`, `ItemCategoryMaster`（カテゴリは `DisplayLevel` 順、商品は `DisplayLevel` 順）。
  - `ItemCatalog` / `HandyCatalog` / `ItemMasterCatalog` を構築してUIへ供給。
- `spring/src/main/java/com/example/demo/dao/PoiPosConfigExporter.java`
  - レジキー書込: `PresetMenuMaster`, `PresetMenuButtonMaster`。
  - 商品マスタ書込: `ItemMaster.ItemCode`, `ItemNamePrint`, `UnitPrice`, `CostPrice`, `BasePrice`。
  - ハンディ書込: `CategoryMaster`, `ItemCategoryMaster`（追加/削除/並び替えを反映）。
  - 行をクリアして再構築し、並び順を安定化。
- `spring/src/main/java/com/example/demo/dao/ExcelSupport.java`
  - Reader/Exporter共通の `ExcelUtil`, `HeaderMap`, `firstExisting` を提供。
- `spring/src/main/java/com/example/demo/dao/InMemoryDraftRepository.java`
  - メモリ + `.ser` ファイルへ保存（デフォルト: `${java.io.tmpdir}/flippers-drafts`）。

## Vue構成（分割後）
現状フロントは、`view + composable + component` に分割済みです。

- `vue/src/App.vue`
  - サイドバー付きアプリシェル。画面遷移ナビゲーションと折りたたみ制御を担当。
- `vue/src/composables/usePosDraft.js`
  - 状態管理 + API呼び出し（Import/Export、レジキー編集、ハンディ編集、商品マスタ編集、Undo/Redo、履歴取得、履歴ジャンプ、履歴削除）。
- `vue/src/composables/useDragDrop.js`
  - 下段グリッドのドラッグ&ドロップ制御。
- `vue/src/views/TopView.vue`
  - Import/Export集約画面。`draftId` 表示と各画面への導線を提供。
- `vue/src/views/PosEditorView.vue`
  - レジキー編集画面。
- `vue/src/views/HandyView.vue`
  - ハンディ編集画面（カテゴリ列 + 商品列）。
- `vue/src/views/CatalogView.vue`
  - 商品カタログ画面（一覧、カテゴリ絞り込み、検索、商品マスタ編集ダイアログ）。
- `vue/src/components/ModeSwitch.vue`
  - レジキー/ハンディ切替。
- `vue/src/components/pos/CategoryTabs.vue`
  - 上段カテゴリ表示、カテゴリ追加、カテゴリD&D入れ替え、ゴミ箱へのD&D削除。
- `vue/src/components/pos/ButtonGrid.vue`
  - 下段グリッド表示、ボタン追加/削除/価格変更/ドラッグ、`Page (col x row)` 右側のグリッド変更ボタン。
- `vue/src/components/pos/AddDialog.vue`
  - 空セル追加ダイアログ（カテゴリ絞り込み + 商品選択）。
- `vue/src/components/pos/PriceDialog.vue`
  - 価格変更ダイアログ。
- `vue/src/components/pos/CategoryDialog.vue`
  - カテゴリ追加ダイアログ。
- `vue/src/components/pos/GridDialog.vue`
  - グリッド（列/行）変更ダイアログ。
- `vue/src/components/pos/EditHistoryPanel.vue`
  - 編集履歴表示（折りたたみ、展開、現在位置表示、クリックジャンプ、履歴削除）。
- `vue/src/components/pos/HandyCatalogPanel.vue`
  - ハンディカテゴリ・商品表示、カテゴリ/商品の挿入位置ベースD&D移動、追加/削除操作の起点UI。
  - D&D中は「行そのもの」ではなく「行間（挿入位置）」に青ラインを表示。
- `vue/src/components/pos/HandyAddDialog.vue`
  - ハンディ商品追加ダイアログ（カテゴリ絞り込み + 商品選択）。
- `vue/src/components/pos/HandyCategoryDialog.vue`
  - ハンディカテゴリ追加ダイアログ（コードは自動採番）。

## API一覧（実装済み）
- `POST /api/pos/import`

### 共通
- `POST /api/pos/drafts/{draftId}/undo`
- `POST /api/pos/drafts/{draftId}/redo`
- `GET /api/pos/drafts/{draftId}/history`
- `POST /api/pos/drafts/{draftId}/history/jump?index={historyIndex}`
- `DELETE /api/pos/drafts/{draftId}/history`
- `GET /api/pos/drafts/{draftId}/export`

### レジキー
- `GET /api/pos/drafts/{draftId}/pages/{pageNumber}`
- `PATCH /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons/swap`
- `POST /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons`
- `DELETE /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons`
- `PATCH /api/pos/drafts/{draftId}/pages/{pageNumber}/buttons/unit-price`
- `GET /api/pos/drafts/{draftId}/item-categories`
- `GET /api/pos/drafts/{draftId}/item-master`
- `PATCH /api/pos/drafts/{draftId}/item-master/{currentItemCode}`
- `POST /api/pos/drafts/{draftId}/categories`
- `DELETE /api/pos/drafts/{draftId}/categories/{pageNumber}`
- `PATCH /api/pos/drafts/{draftId}/categories/swap`
- `PATCH /api/pos/drafts/{draftId}/categories/{pageNumber}/grid`

### ハンディ
- `GET /api/pos/drafts/{draftId}/handy-categories`
- `POST /api/pos/drafts/{draftId}/handy-categories`
- `DELETE /api/pos/drafts/{draftId}/handy-categories/{categoryCode}`
- `PATCH /api/pos/drafts/{draftId}/handy-categories/reorder`
- `PATCH /api/pos/drafts/{draftId}/handy-categories/{categoryCode}/items/reorder`
- `POST /api/pos/drafts/{draftId}/handy-categories/{categoryCode}/items`
- `DELETE /api/pos/drafts/{draftId}/handy-categories/{categoryCode}/items/{itemIndex}`
- `PATCH /api/pos/drafts/{draftId}/handy-categories/swap`（互換用・現行UI未使用）

## 凹型レイヤ観点での補足
現状の設計は、依存逆転の方向は概ね成立しています。

- 良い点: ドメイン `PosConfig` に編集ルールが集約され、UI/Excel実装が直接ルールを持たない。
- 良い点: I/O境界が `port` で抽象化されており、Reader/Exporter/Repositoryを交換しやすい。
- 改善済み: `GetPageService` に集約されていた編集処理はユースケース単位へ分割済み。
- 改善済み: フロントは `usePosDraft/useDragDrop` + `components/pos/*` へ分割済み。
- 改善済み: `ItemCatalog` / `HandyCatalog` / `ItemMasterCatalog` を `PosDraft` にキャッシュし、操作ごとのExcel再パースを回避。

## 回帰テスト（golden比較）
`spring/src/test/java/com/example/demo/ExcelGoldenRegressionTest.java` で、以下3シナリオのExcelスナップショット比較を自動化しています。

1. no-edit export
2. POS編集シナリオ（価格変更 + ボタン移動）
3. ハンディ編集シナリオ（カテゴリ移動 + 商品並び替え + 商品削除）

goldenファイル:
- `spring/src/test/resources/golden/no-edits.snap.txt`
- `spring/src/test/resources/golden/pos-edits.snap.txt`
- `spring/src/test/resources/golden/handy-edits.snap.txt`

実行:
```bash
cd spring
./mvnw -Dtest=ExcelGoldenRegressionTest test
```

golden更新（仕様変更時のみ）:
```bash
cd spring
./mvnw -Dtest=ExcelGoldenRegressionTest -DupdateGolden=true test
```

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

## 同梱ビルド自動化（Windows配布 / npm不要運用）
このリポジトリには、フロントをSpringへ同梱して配布物を作るスクリプトを追加しています。

- `scripts/build-release.sh`（macOS/Linux）
- `scripts/build-release.bat`（Windows）
- `scripts/build-release-with-jre.sh`（macOS/Linux, JRE ZIP指定）
- `scripts/build-release-with-jre.bat`（Windows, JRE ZIP指定）

実行内容:
1. `vue` をビルド
2. `vue/dist` を `spring/src/main/resources/static` にコピー
3. Spring Boot JAR を作成（`-DskipTests`）
4. 配布物を `spring/target/release/windows` に出力
   - `flippers.jar`
   - `run.bat`
   - `README.txt`

### 実行例（macOS/Linux）
```bash
./scripts/build-release.sh
```

### 実行例（Windows）
```bat
scripts\build-release.bat
```

### JRE同梱ビルド（ZIP指定）
JREを同梱した配布物を作る場合は、JREのZIPファイルを引数で指定します。  
出力先は同じく `spring/target/release/windows` で、`jre` フォルダが自動配置されます。

実行例（macOS/Linux）:
```bash
./scripts/build-release-with-jre.sh /path/to/windows-jre.zip
```

実行例（Windows）:
```bat
scripts\build-release-with-jre.bat C:\path\to\windows-jre.zip
```

補足:
- macOS/Linux版は `unzip` コマンドが必要です。
- Windows版は PowerShell の `Expand-Archive` を使用します。

### 配布先（npmなし端末）での起動
`spring/target/release/windows` を丸ごとコピーして、`run.bat` を実行します。  
Java同梱運用にしたい場合は `jre` フォルダを同じ階層に置いてください（`run.bat` が優先して利用）。

## SPAフォールバック設定
Vue Router は `createWebHistory` を使用しているため、Spring側で以下の直アクセスを `index.html` へフォワードします。

- `/`
- `/pos`
- `/handy`
- `/catalog`

実装: `spring/src/main/java/com/example/demo/controller/SpaForwardController.java`
