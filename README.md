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

## 実装状況（2026-02-18時点）
現在は、業務フロー「Import -> UI編集 -> Export」まで、レジキー・ハンディの両方で主要機能を実装済みです。

1. Excelアップロード (`/api/pos/import`) と `draftId` ベース編集セッション管理
2. モード切替（レジキー / ハンディ）UI
3. Undo / Redo
4. 編集履歴（折りたたみ表示、展開、クリックジャンプ、履歴削除）
5. Excelエクスポート (`/api/pos/drafts/{draftId}/export`)
6. 直前操作の視覚フィードバック（対象カテゴリ/商品/ボタンのハイライト）

### レジキー実装済み
1. POS画面再現（上段カテゴリ + 下段グリッド）
2. ボタン同士の入れ替え（空セルとの入れ替え含む）
3. 空セルへのボタン追加（`MDHierarchyMaster` + `POSItemMaster` 由来カタログから選択）
4. ボタン削除
5. ボタン価格変更（`ItemMaster.UnitPrice` 反映）
6. 上段カテゴリの追加・削除
7. 上段カテゴリ同士の入れ替え（D&D）
8. 上段カテゴリをゴミ箱へD&D削除
9. 下段グリッドサイズ変更（`Page(col x row)` 右側ボタン）
10. ボタン表示に `ItemCode` と価格表示
11. カテゴリ削除は「削除対象を空白ハイライト -> 短時間ロック -> 詰める」演出に対応（削除待機は0.5秒）

### ハンディ実装済み
1. `CategoryMaster` / `ItemCategoryMaster` からカテゴリ・商品を表示
2. 左ペインカテゴリ、右ペイン商品の2カラム表示（それぞれ独立スクロール）
3. 商品並び替え（D&D、入れ替えではなく挿入位置ベースの移動）
4. 商品削除（`ItemCategoryMaster` の該当行削除として反映）
5. 商品追加（カテゴリ絞り込み + 商品選択）
6. カテゴリ追加
7. カテゴリ削除
8. カテゴリ移動（D&D、入れ替えではなく挿入位置ベースの移動）
9. カテゴリ追加時のコード自動採番
10. 自動採番ルール: `10` 以上で未使用の最小数値を `CategoryCode` として採用
11. カテゴリ/商品削除は「削除対象を空白ハイライト -> 短時間ロック -> 詰める」演出に対応（削除待機は0.5秒）
12. カテゴリ削除時の確認ダイアログは廃止（履歴復元前提）

### エクスポート反映（実装済み）
1. `PresetMenuButtonMaster` を `(PageNumber, ButtonColumnNumber, ButtonRowNumber)` 順で再構築
2. `PresetMenuMaster` を再構築
3. `ItemMaster.UnitPrice` をボタン編集結果で更新
4. `CategoryMaster` をハンディカテゴリ順で再構築（追加/削除/並び替え反映）
5. `ItemCategoryMaster` をハンディ編集結果で再構築（追加/削除/商品DisplayLevel反映）

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
  - レジキー編集API群（ページ取得、ボタン編集、カテゴリ編集、グリッド変更、undo/redo、履歴取得・ジャンプ・削除、export）。
- `spring/src/main/java/com/example/demo/controller/HandyDraftController.java`
  - ハンディ編集API群（カテゴリ取得/追加/削除/移動、商品追加/削除/並び替え）。
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
- `spring/src/main/java/com/example/demo/service/ExportPosService.java`
  - ドラフト取得後、`PosConfigExporter` でExcelへ反映。
- `spring/src/main/java/com/example/demo/service/DraftServiceSupport.java`
  - draft/page取得、履歴付き保存、`ItemCatalog` キャッシュ読込などの共通処理。

### 3. Domain層（中心）
- `spring/src/main/java/com/example/demo/model/PosConfig.java`
  - ドメイン集約ルート。
  - 上段カテゴリ (`Category`) と下段ページ (`Page`) を保持。
  - 主要操作は `swapButtons`, `addButton`, `deleteButton`, `updateUnitPrice`, `addCategory`, `deleteCategory`, `swapCategories`, `updateCategoryGrid`。
- `spring/src/main/java/com/example/demo/model/PosDraft.java`
  - `draftId + PosConfig + originalExcelBytes` の編集セッション単位。
  - 差分ベース履歴（`changes`, `historyEntries`, `historyIndex`）を保持し、`undo`, `redo`, `jumpToHistoryIndex`, `clearHistory` を提供。
  - `ItemCatalog` と `HandyCatalog` を保持し、再パース回数を抑制。
  - ハンディ操作の差分 (`Add/Delete/Reorder Category`, `Add/Delete/Reorder Item`) も履歴管理。
- `spring/src/main/java/com/example/demo/model/PosConfigSource.java`
  - ReaderがExcelから抽出した中間データ。
- `spring/src/main/java/com/example/demo/model/ItemCatalog.java`
  - 商品カタログ/ハンディカタログ共通のカテゴリ・商品モデル。

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
  - `ItemCatalog` / `HandyCatalog` を構築してUIへ供給。
- `spring/src/main/java/com/example/demo/dao/PoiPosConfigExporter.java`
  - レジキー書込: `PresetMenuMaster`, `PresetMenuButtonMaster`, `ItemMaster.UnitPrice`。
  - ハンディ書込: `CategoryMaster`, `ItemCategoryMaster`（追加/削除/並び替えを反映）。
  - 行をクリアして再構築し、並び順を安定化。
- `spring/src/main/java/com/example/demo/dao/ExcelSupport.java`
  - Reader/Exporter共通の `ExcelUtil`, `HeaderMap`, `firstExisting` を提供。
- `spring/src/main/java/com/example/demo/dao/InMemoryDraftRepository.java`
  - メモリ + `.ser` ファイルへ保存（デフォルト: `${java.io.tmpdir}/flippers-drafts`）。

## Vue構成（分割後）
現状フロントは、`view + composable + component` に分割済みです。

- `vue/src/composables/usePosDraft.js`
  - 状態管理 + API呼び出し（Import/Export、レジキー編集、ハンディ編集、Undo/Redo、履歴取得、履歴ジャンプ、履歴削除）。
- `vue/src/composables/useDragDrop.js`
  - 下段グリッドのドラッグ&ドロップ制御。
- `vue/src/views/PosEditorView.vue`
  - レジキー編集画面。
- `vue/src/views/HandyView.vue`
  - ハンディ編集画面（カテゴリ列 + 商品列）。
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
- 改善済み: `ItemCatalog` / `HandyCatalog` を `PosDraft` にキャッシュし、操作ごとのExcel再パースを回避。

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
