<script setup>
import { computed, ref } from "vue";
import { usePosDraft } from "../composables/usePosDraft";
import { useDragDrop } from "../composables/useDragDrop";
import CategoryTabs from "../components/pos/CategoryTabs.vue";
import ButtonGrid from "../components/pos/ButtonGrid.vue";
import AddDialog from "../components/pos/AddDialog.vue";
import PriceDialog from "../components/pos/PriceDialog.vue";
import CategoryDialog from "../components/pos/CategoryDialog.vue";
import GridDialog from "../components/pos/GridDialog.vue";
import EditHistoryPanel from "../components/pos/EditHistoryPanel.vue";
import ModeSwitch from "../components/ModeSwitch.vue";

const envApiBase = (import.meta.env.VITE_API_BASE ?? "").trim();
const API_BASE = (envApiBase || "/api/pos").replace(/\/+$/, "");
const fileRef = ref(null);

const {
  state,
  catalogState,
  addDialog,
  priceDialog,
  categoryDialog,
  gridDialog,
  buttonMap,
  gridCells,
  filteredAddItems,
  importExcel,
  loadPage,
  exportExcel,
  buttonClass,
  formatPrice,
  closeAddDialog,
  closePriceDialog,
  closeCategoryDialog,
  closeGridDialog,
  openCategoryDialog,
  submitAddCategory,
  deleteSelectedCategory,
  deleteCategoryByPage,
  swapCategories,
  openGridDialog,
  submitGridUpdate,
  openPriceDialog,
  submitUnitPriceUpdate,
  openAddDialog,
  addButtonFromCatalog,
  deleteButton,
  swapButtons,
  undo,
  redo,
  jumpToHistory,
  clearHistory,
} = usePosDraft(API_BASE);

const loadingRef = computed(() => state.loading);
const selectedCategoryName = computed(() => {
  if (!Number.isInteger(state.selectedPageNumber)) return "";
  return state.categories.find((category) => category.pageNumber === state.selectedPageNumber)?.name ?? "";
});

const {
  dragState,
  onButtonDragStart,
  onCellDragOver,
  onCellDragLeave,
  onCellDrop,
  onButtonDragEnd,
} = useDragDrop({
  loadingRef,
  getSourceButton: (key) => buttonMap.value.get(key),
  onSwap: ({ fromCol, fromRow, toCol, toRow }) => swapButtons(fromCol, fromRow, toCol, toRow),
});

async function onImportClick() {
  const file = fileRef.value?.files?.[0];
  await importExcel(file);
}
</script>

<template>
  <div class="wrap">
    <h1>Flippers POS Key Editor (MVP)</h1>

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

    <CategoryTabs
      v-if="state.draftId"
      :categories="state.categories"
      :selected-page-number="state.selectedPageNumber"
      :loading="state.loading"
      @select-page="loadPage"
      @swap-categories="({ fromPageNumber, toPageNumber }) => swapCategories(fromPageNumber, toPageNumber)"
      @delete-category-page="({ pageNumber }) => deleteCategoryByPage(pageNumber)"
      @add-category="openCategoryDialog"
      @delete-category="deleteSelectedCategory"
    />

    <ButtonGrid
      :page="state.page"
      :grid-cells="gridCells"
      :loading="state.loading"
      :drag-state="dragState"
      :button-class="buttonClass"
      :format-price="formatPrice"
      @edit-grid="openGridDialog"
      @open-add="openAddDialog"
      @delete-button="deleteButton"
      @open-price="openPriceDialog"
      @button-drag-start="onButtonDragStart"
      @cell-drag-over="onCellDragOver"
      @cell-drag-leave="onCellDragLeave"
      @cell-drop="onCellDrop"
      @button-drag-end="onButtonDragEnd"
    />

    <AddDialog
      :open="addDialog.open"
      :loading="state.loading"
      :target-col="addDialog.targetCol"
      :target-row="addDialog.targetRow"
      :categories="catalogState.categories"
      :category-code="addDialog.categoryCode"
      :search="addDialog.search"
      :items="filteredAddItems"
      :format-price="formatPrice"
      @close="closeAddDialog"
      @update:category-code="addDialog.categoryCode = $event"
      @update:search="addDialog.search = $event"
      @select-item="addButtonFromCatalog"
    />

    <PriceDialog
      :open="priceDialog.open"
      :loading="state.loading"
      :label="priceDialog.label"
      :item-code="priceDialog.itemCode"
      :unit-price="priceDialog.unitPrice"
      @close="closePriceDialog"
      @submit="submitUnitPriceUpdate"
      @update:unit-price="priceDialog.unitPrice = $event"
    />

    <CategoryDialog
      :open="categoryDialog.open"
      :loading="state.loading"
      :name="categoryDialog.name"
      :cols="categoryDialog.cols"
      :rows="categoryDialog.rows"
      :style-key="categoryDialog.styleKey"
      @close="closeCategoryDialog"
      @submit="submitAddCategory"
      @update:name="categoryDialog.name = $event"
      @update:cols="categoryDialog.cols = $event"
      @update:rows="categoryDialog.rows = $event"
      @update:style-key="categoryDialog.styleKey = $event"
    />

    <GridDialog
      :open="gridDialog.open"
      :loading="state.loading"
      :category-name="selectedCategoryName"
      :cols="gridDialog.cols"
      :rows="gridDialog.rows"
      @close="closeGridDialog"
      @submit="submitGridUpdate"
      @update:cols="gridDialog.cols = $event"
      @update:rows="gridDialog.rows = $event"
    />
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
</style>
