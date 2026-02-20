<script setup>
import { computed, inject, reactive, watch } from "vue";
import { usePosDraft } from "../composables/usePosDraft";

const envApiBase = (import.meta.env.VITE_API_BASE ?? "").trim();
const API_BASE = (envApiBase || "/api/pos").replace(/\/+$/, "");
const toggleSidebar = inject("toggleSidebar", null);
const { state, updateItemMasterItem } = usePosDraft(API_BASE);

const catalogState = reactive({
  loading: false,
  loaded: false,
  error: "",
  search: "",
  items: [],
});

const filterState = reactive({
  categoryCode: "",
  categories: [],
});

const editDialog = reactive({
  open: false,
  currentItemCode: "",
  itemCode: "",
  itemNamePrint: "",
  unitPrice: "",
  costPrice: "",
  basePrice: "",
  error: "",
});

const selectedFilterCategory = computed(() => {
  if (!filterState.categoryCode) return null;
  return filterState.categories.find((category) => category.code === filterState.categoryCode) ?? null;
});

const filteredItems = computed(() => {
  let scopedItems = catalogState.items;
  if (selectedFilterCategory.value) {
    const categoryItemCodes = new Set(
      (selectedFilterCategory.value.items ?? [])
        .map((item) => String(item?.itemCode ?? "").trim())
        .filter((value) => value.length > 0),
    );
    scopedItems = scopedItems.filter((item) => categoryItemCodes.has(String(item?.itemCode ?? "").trim()));
  }

  const keyword = String(catalogState.search ?? "").trim().toLowerCase();
  if (!keyword) return scopedItems;

  return scopedItems.filter((item) => {
    const itemCode = String(item?.itemCode ?? "").toLowerCase();
    const itemNamePrint = String(item?.itemNamePrint ?? "").toLowerCase();
    return itemCode.includes(keyword) || itemNamePrint.includes(keyword);
  });
});

function onToggleSidebar() {
  if (typeof toggleSidebar === "function") {
    toggleSidebar();
  }
}

function closeEditDialog() {
  editDialog.open = false;
  editDialog.currentItemCode = "";
  editDialog.itemCode = "";
  editDialog.itemNamePrint = "";
  editDialog.unitPrice = "";
  editDialog.costPrice = "";
  editDialog.basePrice = "";
  editDialog.error = "";
}

function openEditDialog(item) {
  if (!item || catalogState.loading || state.loading) return;
  editDialog.currentItemCode = String(item.itemCode ?? "").trim();
  editDialog.itemCode = String(item.itemCode ?? "").trim();
  editDialog.itemNamePrint = String(item.itemNamePrint ?? "").trim();
  editDialog.unitPrice = String(item.unitPrice ?? "").trim();
  editDialog.costPrice = String(item.costPrice ?? "").trim();
  editDialog.basePrice = String(item.basePrice ?? "").trim();
  editDialog.error = "";
  editDialog.open = true;
}

function normalizeRequiredInput(value, fieldName) {
  const normalized = String(value ?? "").trim();
  if (!normalized) {
    throw new Error(`${fieldName}を入力してください`);
  }
  return normalized;
}

function normalizeOptionalNumericInput(value, fieldName, required = false) {
  const normalized = String(value ?? "").trim().replace(/,/g, "");
  if (!normalized) {
    if (required) {
      throw new Error(`${fieldName}を入力してください`);
    }
    return "";
  }
  if (!/^\d+(\.\d+)?$/.test(normalized)) {
    throw new Error(`${fieldName}は数値で入力してください`);
  }
  return normalized;
}

async function submitItemMasterEdit() {
  if (!editDialog.open || state.loading || catalogState.loading || !state.draftId) return;

  let payload = null;
  try {
    payload = {
      itemCode: normalizeRequiredInput(editDialog.itemCode, "ItemCode"),
      itemNamePrint: normalizeRequiredInput(editDialog.itemNamePrint, "ItemNamePrint"),
      unitPrice: normalizeOptionalNumericInput(editDialog.unitPrice, "UnitPrice", true),
      costPrice: normalizeOptionalNumericInput(editDialog.costPrice, "CostPrice"),
      basePrice: normalizeOptionalNumericInput(editDialog.basePrice, "BasePrice"),
    };
  } catch (error) {
    editDialog.error = String(error?.message ?? error);
    return;
  }

  editDialog.error = "";
  const success = await updateItemMasterItem(editDialog.currentItemCode, payload);
  if (!success) {
    editDialog.error = state.error || "更新に失敗しました";
    return;
  }
  closeEditDialog();
  await loadCatalogData();
}

async function readApiError(res, fallbackMessage) {
  let message = fallbackMessage;
  try {
    const body = await res.json();
    if (body?.message) {
      message = body.message;
    }
  } catch {
    // ignore
  }
  return message;
}

async function loadCatalogData() {
  if (!state.draftId) {
    catalogState.loaded = false;
    catalogState.error = "";
    catalogState.search = "";
    catalogState.items = [];
    filterState.categoryCode = "";
    filterState.categories = [];
    return;
  }

  catalogState.loading = true;
  catalogState.error = "";
  try {
    const [masterRes, categoryRes] = await Promise.all([
      fetch(`${API_BASE}/drafts/${state.draftId}/item-master`),
      fetch(`${API_BASE}/drafts/${state.draftId}/item-categories`),
    ]);

    if (!masterRes.ok) {
      throw new Error(await readApiError(masterRes, `Get item master catalog failed: ${masterRes.status}`));
    }
    if (!categoryRes.ok) {
      throw new Error(await readApiError(categoryRes, `Get item categories failed: ${categoryRes.status}`));
    }

    const [masterData, categoryData] = await Promise.all([masterRes.json(), categoryRes.json()]);
    catalogState.items = Array.isArray(masterData?.items) ? masterData.items : [];
    filterState.categories = Array.isArray(categoryData?.categories) ? categoryData.categories : [];
    if (!filterState.categories.some((category) => category.code === filterState.categoryCode)) {
      filterState.categoryCode = "";
    }
    catalogState.loaded = true;
  } catch (error) {
    catalogState.error = String(error);
  } finally {
    catalogState.loading = false;
  }
}

watch(
  () => state.draftId,
  () => {
    void loadCatalogData();
  },
  { immediate: true },
);

function formatPriceValue(value) {
  const raw = String(value ?? "").trim();
  if (!raw) return "";

  const normalized = raw.replace(/,/g, "");
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) {
    return raw;
  }

  const number = Number(normalized);
  if (!Number.isFinite(number)) {
    return raw;
  }

  return Number.isInteger(number)
    ? number.toLocaleString("ja-JP")
    : number.toLocaleString("ja-JP", { maximumFractionDigits: 2 });
}
</script>

<template>
  <div class="wrap">
    <section class="panel">
      <div class="title-row">
        <button type="button" class="menu-toggle" aria-label="サイドバーを開閉" @click="onToggleSidebar">☰</button>
        <h1>商品カタログ</h1>
      </div>

      <p v-if="!state.draftId" class="hint">先にトップ画面でExcelをImportしてください。</p>

      <template v-else>
        <div class="meta-row">
          <div class="meta"><b>draftId</b>: {{ state.draftId }}</div>
          <button type="button" class="reload-btn" :disabled="catalogState.loading" @click="loadCatalogData">
            再読み込み
          </button>
        </div>

        <div class="filter-row">
          <label class="field-label" for="catalog-category-filter">カテゴリ</label>
          <select id="catalog-category-filter" v-model="filterState.categoryCode" class="category-select">
            <option value="">すべてのカテゴリ</option>
            <option v-for="category in filterState.categories" :key="category.code" :value="category.code">
              {{ category.description }} ({{ Array.isArray(category.items) ? category.items.length : 0 }})
            </option>
          </select>
        </div>

        <div class="search-row">
          <input
            v-model="catalogState.search"
            type="text"
            class="search-input"
            placeholder="ItemCode / ItemNamePrint で検索"
          />
        </div>

        <div v-if="catalogState.error" class="error">{{ catalogState.error }}</div>
        <div v-else-if="catalogState.loading" class="hint">読み込み中...</div>

        <div v-else class="table-wrap">
          <table class="catalog-table">
            <thead>
              <tr>
                <th>ItemCode</th>
                <th>ItemNamePrint</th>
                <th class="num-head">UnitPrice</th>
                <th class="num-head">CostPrice</th>
                <th class="num-head">BasePrice</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(item, index) in filteredItems"
                :key="`${item.itemCode}-${index}`"
                class="clickable-row"
                @click="openEditDialog(item)"
              >
                <td class="mono">{{ item.itemCode }}</td>
                <td>{{ item.itemNamePrint }}</td>
                <td class="num">{{ formatPriceValue(item.unitPrice) }}</td>
                <td class="num">{{ formatPriceValue(item.costPrice) }}</td>
                <td class="num">{{ formatPriceValue(item.basePrice) }}</td>
              </tr>
              <tr v-if="!filteredItems.length">
                <td colspan="5" class="empty">表示できる商品がありません</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p class="count">全 {{ filteredItems.length }} 件 / 元データ {{ catalogState.items.length }} 件</p>

        <div v-if="editDialog.open" class="modal-backdrop" @click="closeEditDialog">
          <div class="edit-modal" @click.stop>
            <div class="modal-head">
              <h2>商品マスタ編集</h2>
              <button class="close-btn" type="button" :disabled="state.loading" @click="closeEditDialog">×</button>
            </div>

            <label class="field-label" for="edit-item-code">ItemCode</label>
            <input id="edit-item-code" v-model="editDialog.itemCode" class="item-input" type="text" />

            <label class="field-label" for="edit-item-name-print">ItemNamePrint</label>
            <input id="edit-item-name-print" v-model="editDialog.itemNamePrint" class="item-input" type="text" />

            <div class="price-grid">
              <label class="field-label" for="edit-unit-price">
                UnitPrice
                <input id="edit-unit-price" v-model="editDialog.unitPrice" class="item-input" type="text" />
              </label>
              <label class="field-label" for="edit-cost-price">
                CostPrice
                <input id="edit-cost-price" v-model="editDialog.costPrice" class="item-input" type="text" />
              </label>
              <label class="field-label" for="edit-base-price">
                BasePrice
                <input id="edit-base-price" v-model="editDialog.basePrice" class="item-input" type="text" />
              </label>
            </div>

            <div v-if="editDialog.error" class="modal-error">{{ editDialog.error }}</div>

            <div class="actions">
              <button class="cancel-btn" type="button" :disabled="state.loading" @click="closeEditDialog">キャンセル</button>
              <button class="save-btn" type="button" :disabled="state.loading" @click="submitItemMasterEdit">
                保存
              </button>
            </div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 1240px;
  margin: 24px auto;
  padding: 0 16px;
}

.panel {
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  background: #fff;
  padding: 18px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

h1 {
  margin: 0;
  font-size: 28px;
}

.menu-toggle {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #c7d1e4;
  background: #fff;
  color: #344862;
  font-size: 18px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.menu-toggle:hover {
  background: #f5f8ff;
}

.hint {
  margin: 10px 0 0;
  color: #445;
  font-size: 13px;
}

.meta-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta {
  font-size: 13px;
}

.reload-btn {
  border: 1px solid #d1d8e5;
  border-radius: 999px;
  background: #fff;
  color: #234;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
}

.reload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.search-row {
  margin-top: 10px;
}

.filter-row {
  margin-top: 10px;
}

.field-label {
  display: block;
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #33445f;
}

.category-select {
  width: min(460px, 100%);
  border: 1px solid #d0d9ea;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
  background: #fff;
}

.search-input {
  width: min(460px, 100%);
  border: 1px solid #d0d9ea;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
}

.error {
  color: #b00020;
  margin-top: 10px;
  white-space: pre-wrap;
}

.table-wrap {
  margin-top: 12px;
  border: 1px solid #e6ebf4;
  border-radius: 10px;
  overflow: auto;
  max-height: calc(100dvh - 260px);
}

.catalog-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
}

.catalog-table th,
.catalog-table td {
  border-bottom: 1px solid #edf1f7;
  padding: 8px 10px;
  font-size: 13px;
}

.catalog-table thead th {
  position: sticky;
  top: 0;
  background: #f8fbff;
  text-align: left;
  z-index: 1;
}

.catalog-table thead th.num-head {
  text-align: center;
}

.catalog-table tbody tr:hover {
  background: #f7fbff;
}

.clickable-row {
  cursor: pointer;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.num {
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.empty {
  text-align: center;
  color: #69758a;
}

.count {
  margin: 10px 0 0;
  font-size: 12px;
  color: #5e6a7e;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 24;
}

.edit-modal {
  width: min(560px, 100%);
  background: #fff;
  border: 1px solid #d8e0ef;
  border-radius: 12px;
  box-shadow: 0 14px 42px rgba(0, 0, 0, 0.2);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.modal-head h2 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  border: 1px solid #ccc;
  background: #f8f8f8;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.item-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #cdd7e7;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
}

.price-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.price-grid .field-label {
  display: grid;
  gap: 4px;
}

.modal-error {
  color: #b00020;
  font-size: 13px;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.cancel-btn,
.save-btn {
  border: 1px solid #cfd6e4;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  cursor: pointer;
}

.cancel-btn {
  background: #f6f7fa;
  color: #334;
}

.save-btn {
  background: #3f86f6;
  border-color: #2f6ac9;
  color: #fff;
}

.cancel-btn:disabled,
.save-btn:disabled,
.close-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .price-grid {
    grid-template-columns: 1fr;
  }
}
</style>
