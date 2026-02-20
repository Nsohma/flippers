<script setup>
import { computed, inject, onBeforeUnmount, ref } from "vue";
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
const toggleSidebar = inject("toggleSidebar", null);

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
  loadPage,
  buttonClass,
  formatPrice,
  closeAddDialog,
  closePriceDialog,
  closeCategoryDialog,
  closeGridDialog,
  openCategoryDialog,
  submitAddCategory,
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
const DELETE_PENDING_MS = 500;
const recentEditedKeys = ref([]);
let recentEditedTimerId = null;
const recentEditedCategoryPages = ref([]);
let recentEditedCategoryTimerId = null;
const pendingDeleteCategoryPage = ref(null);
let pendingDeleteCategoryTimerId = null;
const selectedCategoryName = computed(() => {
  if (!Number.isInteger(state.selectedPageNumber)) return "";
  return state.categories.find((category) => category.pageNumber === state.selectedPageNumber)?.name ?? "";
});

function markRecentEdited(keys) {
  const normalized = Array.from(new Set(keys.filter((key) => typeof key === "string" && key.trim().length > 0)));
  recentEditedKeys.value = normalized;
  if (recentEditedTimerId != null) {
    window.clearTimeout(recentEditedTimerId);
  }
  if (!normalized.length) {
    recentEditedTimerId = null;
    return;
  }
  recentEditedTimerId = window.setTimeout(() => {
    recentEditedKeys.value = [];
    recentEditedTimerId = null;
  }, 1500);
}

function markRecentEditedCategories(pageNumbers) {
  const normalized = Array.from(
    new Set(pageNumbers.filter((pageNumber) => Number.isInteger(pageNumber) && pageNumber > 0)),
  );
  recentEditedCategoryPages.value = normalized;
  if (recentEditedCategoryTimerId != null) {
    window.clearTimeout(recentEditedCategoryTimerId);
  }
  if (!normalized.length) {
    recentEditedCategoryTimerId = null;
    return;
  }
  recentEditedCategoryTimerId = window.setTimeout(() => {
    recentEditedCategoryPages.value = [];
    recentEditedCategoryTimerId = null;
  }, 1500);
}

async function swapButtonsWithHighlight(fromCol, fromRow, toCol, toRow) {
  const success = await swapButtons(fromCol, fromRow, toCol, toRow);
  if (!success) return;
  markRecentEdited([`${fromCol}-${fromRow}`, `${toCol}-${toRow}`]);
}

async function submitAddCategoryWithHighlight() {
  const success = await submitAddCategory();
  if (!success) return;
  markRecentEditedCategories([state.selectedPageNumber]);
}

function queueCategoryDelete(pageNumber) {
  const targetPageNumber = Number.parseInt(String(pageNumber ?? ""), 10);
  if (!Number.isInteger(targetPageNumber) || targetPageNumber <= 0) return;
  if (pendingDeleteCategoryTimerId != null) return;
  if (!state.categories.some((category) => category.pageNumber === targetPageNumber)) return;

  const requestedDraftId = state.draftId;
  pendingDeleteCategoryPage.value = targetPageNumber;
  markRecentEditedCategories([]);
  pendingDeleteCategoryTimerId = window.setTimeout(async () => {
    pendingDeleteCategoryTimerId = null;
    try {
      if (!requestedDraftId || state.draftId !== requestedDraftId) return;
      await deleteCategoryByPage(targetPageNumber);
    } finally {
      pendingDeleteCategoryPage.value = null;
    }
  }, DELETE_PENDING_MS);
}

function deleteCategoryByPageWithHighlight(pageNumber) {
  queueCategoryDelete(pageNumber);
}

function deleteSelectedCategoryWithHighlight() {
  queueCategoryDelete(state.selectedPageNumber);
}

async function swapCategoriesWithHighlight(fromPageNumber, toPageNumber) {
  const success = await swapCategories(fromPageNumber, toPageNumber);
  if (!success) return;
  markRecentEditedCategories([fromPageNumber, toPageNumber]);
}

async function addButtonWithHighlight(item) {
  const targetKey = `${addDialog.targetCol}-${addDialog.targetRow}`;
  const success = await addButtonFromCatalog(item);
  if (!success) return;
  markRecentEdited([targetKey]);
}

async function deleteButtonWithHighlight(button) {
  const targetKey = `${button?.col}-${button?.row}`;
  const success = await deleteButton(button);
  if (!success) return;
  markRecentEdited([targetKey]);
}

async function submitUnitPriceUpdateWithHighlight() {
  const targetButton = state.page?.buttons?.find((button) => button.buttonId === priceDialog.buttonId) ?? null;
  const targetKey = targetButton ? `${targetButton.col}-${targetButton.row}` : "";
  const success = await submitUnitPriceUpdate();
  if (!success || !targetKey) return;
  markRecentEdited([targetKey]);
}

onBeforeUnmount(() => {
  if (recentEditedTimerId != null) {
    window.clearTimeout(recentEditedTimerId);
    recentEditedTimerId = null;
  }
  if (recentEditedCategoryTimerId != null) {
    window.clearTimeout(recentEditedCategoryTimerId);
    recentEditedCategoryTimerId = null;
  }
  if (pendingDeleteCategoryTimerId != null) {
    window.clearTimeout(pendingDeleteCategoryTimerId);
    pendingDeleteCategoryTimerId = null;
  }
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
  onSwap: ({ fromCol, fromRow, toCol, toRow }) => swapButtonsWithHighlight(fromCol, fromRow, toCol, toRow),
});

function onToggleSidebar() {
  if (typeof toggleSidebar === "function") {
    toggleSidebar();
  }
}
</script>

<template>
  <div class="wrap">
    <div class="title-row">
      <button type="button" class="menu-toggle" aria-label="サイドバーを開閉" @click="onToggleSidebar">☰</button>
      <h1>Flippers POS Editor</h1>
    </div>

    <section v-if="state.draftId || state.error" class="panel">
      <div v-if="state.draftId" class="meta">
        <div><b>draftId</b>: {{ state.draftId }}</div>
      </div>

      <div v-if="state.error" class="error">{{ state.error }}</div>
    </section>

    <div class="mode-row">
      <ModeSwitch />
    </div>
    <p v-if="!state.draftId" class="hint">
      トップ画面でExcelをImportすると、CategoryMaster / ItemCategoryMaster から画面を表示します。
    </p>

    <EditHistoryPanel
      v-if="state.draftId"
      :entries="state.historyEntries"
      :current-index="state.historyIndex"
      :loading="state.loading"
      :can-undo="state.canUndo"
      :can-redo="state.canRedo"
      @jump="jumpToHistory"
      @clear="clearHistory"
      @undo="undo"
      @redo="redo"
    />

    <CategoryTabs
      v-if="state.draftId"
      :categories="state.categories"
      :recent-edited-page-numbers="recentEditedCategoryPages"
      :pending-delete-page-number="pendingDeleteCategoryPage"
      :selected-page-number="state.selectedPageNumber"
      :loading="state.loading"
      @select-page="loadPage"
      @swap-categories="({ fromPageNumber, toPageNumber }) => swapCategoriesWithHighlight(fromPageNumber, toPageNumber)"
      @delete-category-page="({ pageNumber }) => deleteCategoryByPageWithHighlight(pageNumber)"
      @add-category="openCategoryDialog"
      @delete-category="deleteSelectedCategoryWithHighlight"
    />

    <ButtonGrid
      :page="state.page"
      :grid-cells="gridCells"
      :recent-edited-keys="recentEditedKeys"
      :loading="state.loading"
      :drag-state="dragState"
      :button-class="buttonClass"
      :format-price="formatPrice"
      @edit-grid="openGridDialog"
      @open-add="openAddDialog"
      @delete-button="deleteButtonWithHighlight"
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
      @select-item="addButtonWithHighlight"
    />

    <PriceDialog
      :open="priceDialog.open"
      :loading="state.loading"
      :label="priceDialog.label"
      :item-code="priceDialog.itemCode"
      :unit-price="priceDialog.unitPrice"
      @close="closePriceDialog"
      @submit="submitUnitPriceUpdateWithHighlight"
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
      @submit="submitAddCategoryWithHighlight"
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

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

h1 {
  margin: 0;
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

.panel {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  margin: 12px 0;
}
.mode-row {
  margin: 8px 0 12px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
}
.mode-row :deep(.mode-switch) {
  margin: 0;
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
