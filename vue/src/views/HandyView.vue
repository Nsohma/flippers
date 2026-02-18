<script setup>
import { onMounted, ref, watch } from "vue";
import { usePosDraft } from "../composables/usePosDraft";
import HandyCatalogPanel from "../components/pos/HandyCatalogPanel.vue";
import HandyAddDialog from "../components/pos/HandyAddDialog.vue";
import EditHistoryPanel from "../components/pos/EditHistoryPanel.vue";
import ModeSwitch from "../components/ModeSwitch.vue";

const envApiBase = (import.meta.env.VITE_API_BASE ?? "").trim();
const API_BASE = (envApiBase || "/api/pos").replace(/\/+$/, "");
const fileRef = ref(null);

const {
  state,
  catalogState,
  handyCatalogState,
  handyAddDialog,
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
  reorderHandyItems,
  deleteHandyItem,
  openHandyAddDialog,
  closeHandyAddDialog,
  addHandyItemFromCatalog,
} = usePosDraft(API_BASE);

async function onImportClick() {
  const file = fileRef.value?.files?.[0];
  await importExcel(file);
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
  const categoryCode = selectedHandyCategory.value?.code ?? handyCatalogState.selectedCategoryCode;
  if (!categoryCode) return;
  await reorderHandyItems(categoryCode, fromIndex, toIndex);
}

async function onDeleteItem({ index }) {
  const categoryCode = selectedHandyCategory.value?.code ?? handyCatalogState.selectedCategoryCode;
  if (!categoryCode) return;
  await deleteHandyItem(categoryCode, index);
}

async function onAddItem(item) {
  const categoryCode = selectedHandyCategory.value?.code ?? handyCatalogState.selectedCategoryCode;
  if (!categoryCode) return;
  await addHandyItemFromCatalog(categoryCode, item);
}
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
      :items="handyItems"
      :loading="state.loading || handyCatalogState.loading"
      @select-category="selectHandyCategory"
      @reorder-items="onReorderItems"
      @delete-item="onDeleteItem"
      @open-add="openHandyAddDialog"
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
