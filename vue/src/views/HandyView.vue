<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from "vue";
import { usePosDraft } from "../composables/usePosDraft";
import HandyCatalogPanel from "../components/pos/HandyCatalogPanel.vue";
import HandyAddDialog from "../components/pos/HandyAddDialog.vue";
import HandyCategoryDialog from "../components/pos/HandyCategoryDialog.vue";
import EditHistoryPanel from "../components/pos/EditHistoryPanel.vue";
import ModeSwitch from "../components/ModeSwitch.vue";

const envApiBase = (import.meta.env.VITE_API_BASE ?? "").trim();
const API_BASE = (envApiBase || "/api/pos").replace(/\/+$/, "");
const DELETE_PENDING_MS = 500;
const fileRef = ref(null);
const recentEditedCategoryCodes = ref([]);
const recentEditedItemKeys = ref([]);
const pendingDeleteCategoryCode = ref("");
const pendingDeleteItemKey = ref("");
let recentEditedCategoryTimerId = null;
let recentEditedItemTimerId = null;
let pendingDeleteCategoryTimerId = null;
let pendingDeleteItemTimerId = null;

const {
  state,
  catalogState,
  handyCatalogState,
  handyAddDialog,
  handyCategoryDialog,
  selectedHandyCategory,
  handyItems,
  filteredHandyAddItems,
  importExcel,
  exportExcel,
  formatPrice,
  undo,
  redo,
  jumpToHistory,
  clearHistory,
  loadHandyCatalog,
  selectHandyCategory,
  reorderHandyCategories,
  reorderHandyItems,
  deleteHandyItem,
  openHandyAddDialog,
  closeHandyAddDialog,
  addHandyItemFromCatalog,
  openHandyCategoryDialog,
  closeHandyCategoryDialog,
  submitAddHandyCategory,
  deleteHandyCategory,
} = usePosDraft(API_BASE);

async function onImportClick() {
  clearPendingCategoryDelete();
  clearPendingItemDelete();
  const file = fileRef.value?.files?.[0];
  await importExcel(file);
}

function markRecentHandyCategories(codes) {
  const normalized = Array.from(new Set(codes.filter((code) => typeof code === "string" && code.trim().length > 0)));
  recentEditedCategoryCodes.value = normalized;
  if (recentEditedCategoryTimerId != null) {
    window.clearTimeout(recentEditedCategoryTimerId);
  }
  if (!normalized.length) {
    recentEditedCategoryTimerId = null;
    return;
  }
  recentEditedCategoryTimerId = window.setTimeout(() => {
    recentEditedCategoryCodes.value = [];
    recentEditedCategoryTimerId = null;
  }, 1500);
}

function itemKey(item, index) {
  if (!item) return "";
  return `${item.itemCode}-${index}`;
}

function markRecentHandyItems(keys) {
  const normalized = Array.from(new Set(keys.filter((key) => typeof key === "string" && key.trim().length > 0)));
  recentEditedItemKeys.value = normalized;
  if (recentEditedItemTimerId != null) {
    window.clearTimeout(recentEditedItemTimerId);
  }
  if (!normalized.length) {
    recentEditedItemTimerId = null;
    return;
  }
  recentEditedItemTimerId = window.setTimeout(() => {
    recentEditedItemKeys.value = [];
    recentEditedItemTimerId = null;
  }, 1500);
}

function sameItem(a, b) {
  return (
    String(a?.itemCode ?? "") === String(b?.itemCode ?? "") &&
    String(a?.itemName ?? "") === String(b?.itemName ?? "") &&
    String(a?.unitPrice ?? "") === String(b?.unitPrice ?? "")
  );
}

function findInsertedIndex(beforeItems, afterItems) {
  if (afterItems.length !== beforeItems.length + 1) return -1;
  let beforeIndex = 0;
  let afterIndex = 0;
  while (beforeIndex < beforeItems.length && afterIndex < afterItems.length) {
    if (sameItem(beforeItems[beforeIndex], afterItems[afterIndex])) {
      beforeIndex += 1;
      afterIndex += 1;
      continue;
    }
    return afterIndex;
  }
  return afterItems.length - 1;
}

function hasPendingDelete() {
  return Boolean(pendingDeleteCategoryCode.value) || Boolean(pendingDeleteItemKey.value);
}

onMounted(() => {
  if (state.draftId && !handyCatalogState.loading) {
    void loadHandyCatalog();
  }
});

watch(
  () => state.draftId,
  (draftId) => {
    if (draftId && !handyCatalogState.loading) {
      void loadHandyCatalog();
    }
  },
);

async function onReorderItems({ fromIndex, toIndex }) {
  if (hasPendingDelete()) return;
  const categoryCode = selectedHandyCategory.value?.code ?? handyCatalogState.selectedCategoryCode;
  if (!categoryCode) return;
  const beforeItems = selectedHandyCategory.value?.items ?? [];
  const movedItemCode = String(beforeItems?.[fromIndex]?.itemCode ?? "").trim();
  const success = await reorderHandyItems(categoryCode, fromIndex, toIndex);
  if (!success) return;
  const afterItems = selectedHandyCategory.value?.items ?? [];
  if (!movedItemCode) {
    markRecentHandyItems([]);
    return;
  }
  const movedIndex = afterItems.findIndex((item) => String(item?.itemCode ?? "").trim() === movedItemCode);
  if (movedIndex < 0) {
    markRecentHandyItems([]);
    return;
  }
  markRecentHandyItems([itemKey(afterItems[movedIndex], movedIndex)]);
}

async function onReorderCategories({ fromIndex, toIndex }) {
  if (hasPendingDelete()) return;
  if (!Number.isInteger(fromIndex) || !Number.isInteger(toIndex)) return;
  const movedCategoryCode = String(handyCatalogState.categories?.[fromIndex]?.code ?? "").trim();
  const success = await reorderHandyCategories(fromIndex, toIndex);
  if (!success) return;
  if (!movedCategoryCode) return;
  markRecentHandyCategories([movedCategoryCode]);
}

async function onDeleteItem({ index }) {
  if (hasPendingDelete()) return;
  const categoryCode = selectedHandyCategory.value?.code ?? handyCatalogState.selectedCategoryCode;
  const itemIndex = Number.parseInt(String(index ?? ""), 10);
  if (!categoryCode || !Number.isInteger(itemIndex)) return;
  queueHandyItemDelete(categoryCode, itemIndex);
}

async function onAddItem(item) {
  if (hasPendingDelete()) return;
  const categoryCode = selectedHandyCategory.value?.code ?? handyCatalogState.selectedCategoryCode;
  if (!categoryCode) return;
  const beforeItems = (selectedHandyCategory.value?.items ?? []).map((entry) => ({ ...entry }));
  const success = await addHandyItemFromCatalog(categoryCode, item);
  if (!success) return;
  const afterItems = selectedHandyCategory.value?.items ?? [];
  let insertedIndex = findInsertedIndex(beforeItems, afterItems);
  if (insertedIndex < 0) {
    insertedIndex = afterItems.findIndex((entry) => String(entry?.itemCode ?? "") === String(item?.itemCode ?? ""));
  }
  if (insertedIndex < 0) return;
  markRecentHandyItems([itemKey(afterItems[insertedIndex], insertedIndex)]);
}

async function submitAddHandyCategoryWithHighlight() {
  if (hasPendingDelete()) return;
  const success = await submitAddHandyCategory();
  if (!success) return;
  markRecentHandyCategories([handyCatalogState.selectedCategoryCode]);
}

function clearPendingCategoryDelete() {
  if (pendingDeleteCategoryTimerId != null) {
    window.clearTimeout(pendingDeleteCategoryTimerId);
    pendingDeleteCategoryTimerId = null;
  }
  pendingDeleteCategoryCode.value = "";
}

function clearPendingItemDelete() {
  if (pendingDeleteItemTimerId != null) {
    window.clearTimeout(pendingDeleteItemTimerId);
    pendingDeleteItemTimerId = null;
  }
  pendingDeleteItemKey.value = "";
}

function queueHandyCategoryDelete(categoryCode) {
  const code = String(categoryCode ?? "").trim();
  if (!code) return;
  if (pendingDeleteCategoryTimerId != null) return;
  if (!handyCatalogState.categories.some((category) => String(category?.code ?? "").trim() === code)) return;

  const requestedDraftId = state.draftId;
  pendingDeleteCategoryCode.value = code;
  markRecentHandyCategories([]);
  pendingDeleteCategoryTimerId = window.setTimeout(async () => {
    pendingDeleteCategoryTimerId = null;
    try {
      if (!requestedDraftId || state.draftId !== requestedDraftId) return;
      await deleteHandyCategory(code);
    } finally {
      pendingDeleteCategoryCode.value = "";
    }
  }, DELETE_PENDING_MS);
}

function queueHandyItemDelete(categoryCode, itemIndex) {
  const code = String(categoryCode ?? "").trim();
  if (!code) return;
  if (!Number.isInteger(itemIndex) || itemIndex < 0) return;
  if (pendingDeleteItemTimerId != null) return;

  const targetCategory = handyCatalogState.categories.find(
    (category) => String(category?.code ?? "").trim() === code,
  );
  const items = Array.isArray(targetCategory?.items) ? targetCategory.items : [];
  if (itemIndex >= items.length) return;
  const targetKey = itemKey(items[itemIndex], itemIndex);
  if (!targetKey) return;

  const requestedDraftId = state.draftId;
  pendingDeleteItemKey.value = targetKey;
  markRecentHandyItems([]);
  pendingDeleteItemTimerId = window.setTimeout(async () => {
    pendingDeleteItemTimerId = null;
    try {
      if (!requestedDraftId || state.draftId !== requestedDraftId) return;
      await deleteHandyItem(code, itemIndex);
    } finally {
      pendingDeleteItemKey.value = "";
    }
  }, DELETE_PENDING_MS);
}

async function onDeleteCategory(categoryCode) {
  const code = String(categoryCode ?? "").trim();
  if (!code || hasPendingDelete() || pendingDeleteCategoryTimerId != null) return;
  queueHandyCategoryDelete(code);
}

onBeforeUnmount(() => {
  if (recentEditedCategoryTimerId != null) {
    window.clearTimeout(recentEditedCategoryTimerId);
    recentEditedCategoryTimerId = null;
  }
  if (recentEditedItemTimerId != null) {
    window.clearTimeout(recentEditedItemTimerId);
    recentEditedItemTimerId = null;
  }
  if (pendingDeleteCategoryTimerId != null) {
    window.clearTimeout(pendingDeleteCategoryTimerId);
    pendingDeleteCategoryTimerId = null;
  }
  if (pendingDeleteItemTimerId != null) {
    window.clearTimeout(pendingDeleteItemTimerId);
    pendingDeleteItemTimerId = null;
  }
});
</script>

<template>
  <div class="wrap">
    <h1>Flippers Handy Viewer (MVP)</h1>

    <section class="panel">
      <input ref="fileRef" type="file" accept=".xlsx" />
      <button :disabled="state.loading" @click="onImportClick">Import</button>
      <button :disabled="state.loading || !state.draftId" @click="exportExcel">Export</button>
      <button :disabled="state.loading || !state.draftId || !state.canUndo" @click="undo">Undo</button>
      <button :disabled="state.loading || !state.draftId || !state.canRedo" @click="redo">Redo</button>
      <div v-if="state.draftId" class="meta">
        <div><b>draftId</b>: {{ state.draftId }}</div>
      </div>
      <div v-if="state.error" class="error">{{ state.error }}</div>
    </section>

    <ModeSwitch />

    <EditHistoryPanel
      v-if="state.draftId"
      :entries="state.historyEntries"
      :current-index="state.historyIndex"
      :loading="state.loading"
      @jump="jumpToHistory"
      @clear="clearHistory"
    />

    <p v-if="!state.draftId" class="hint">
      ExcelをImportすると、CategoryMaster / ItemCategoryMaster からハンディカテゴリを表示します。
    </p>

    <HandyCatalogPanel
      v-if="state.draftId && handyCatalogState.categories.length"
      :categories="handyCatalogState.categories"
      :selected-category-code="selectedHandyCategory?.code ?? handyCatalogState.selectedCategoryCode"
      :recent-edited-category-codes="recentEditedCategoryCodes"
      :recent-edited-item-keys="recentEditedItemKeys"
      :pending-delete-category-code="pendingDeleteCategoryCode"
      :pending-delete-item-key="pendingDeleteItemKey"
      :items="handyItems"
      :loading="state.loading || handyCatalogState.loading"
      @select-category="selectHandyCategory"
      @reorder-categories="onReorderCategories"
      @reorder-items="onReorderItems"
      @delete-item="onDeleteItem"
      @open-add="openHandyAddDialog"
      @open-add-category="openHandyCategoryDialog"
      @delete-category="onDeleteCategory"
    />

    <HandyAddDialog
      :open="handyAddDialog.open"
      :loading="state.loading"
      :handy-category-name="
        selectedHandyCategory?.description || selectedHandyCategory?.code || handyCatalogState.selectedCategoryCode
      "
      :categories="catalogState.categories"
      :category-code="handyAddDialog.categoryCode"
      :search="handyAddDialog.search"
      :items="filteredHandyAddItems"
      :format-price="formatPrice"
      @close="closeHandyAddDialog"
      @update:category-code="handyAddDialog.categoryCode = $event"
      @update:search="handyAddDialog.search = $event"
      @select-item="onAddItem"
    />

    <HandyCategoryDialog
      :open="handyCategoryDialog.open"
      :loading="state.loading"
      :description="handyCategoryDialog.description"
      @close="closeHandyCategoryDialog"
      @submit="submitAddHandyCategoryWithHighlight"
      @update:description="handyCategoryDialog.description = $event"
    />

    <p v-if="state.draftId && !handyCatalogState.loading && !handyCatalogState.categories.length" class="hint">
      ハンディ表示用のカテゴリデータが見つかりませんでした。
    </p>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
  font-family: system-ui, -apple-system, sans-serif;
}
.panel {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  margin: 12px 0;
}
.error {
  color: #b00020;
  margin-top: 8px;
  white-space: pre-wrap;
}
.meta {
  margin-top: 8px;
  font-size: 13px;
}
.hint {
  margin-top: 10px;
  color: #445;
  font-size: 13px;
}
</style>
