<script setup>
import { computed, ref } from "vue";

const props = defineProps({
  categories: {
    type: Array,
    default: () => [],
  },
  selectedCategoryCode: {
    type: String,
    default: "",
  },
  recentEditedCategoryCodes: {
    type: Array,
    default: () => [],
  },
  recentEditedItemKeys: {
    type: Array,
    default: () => [],
  },
  pendingDeleteCategoryCode: {
    type: String,
    default: "",
  },
  pendingDeleteItemKey: {
    type: String,
    default: "",
  },
  items: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  "select-category",
  "reorder-categories",
  "reorder-items",
  "delete-item",
  "open-add",
  "open-add-category",
  "delete-category",
]);
const dragFromIndex = ref(-1);
const dragInsertBeforeIndex = ref(-1);
const dragFromCategoryIndex = ref(-1);
const dragInsertBeforeCategoryIndex = ref(-1);

function isCategoryDeletePending() {
  return String(props.pendingDeleteCategoryCode ?? "").trim().length > 0;
}

function isItemDeletePending() {
  return String(props.pendingDeleteItemKey ?? "").trim().length > 0;
}

function hasPendingDelete() {
  return isCategoryDeletePending() || isItemDeletePending();
}

function isDeletingCategory(code) {
  const normalizedCode = String(code ?? "").trim();
  return isCategoryDeletePending() && normalizedCode === String(props.pendingDeleteCategoryCode ?? "").trim();
}

function isDeletingItem(item, index) {
  return String(props.pendingDeleteItemKey ?? "").trim() === `${item?.itemCode}-${index}`;
}

function onSelectCategory(code) {
  if (props.loading || hasPendingDelete()) return;
  emit("select-category", code);
}

function onOpenAddCategory() {
  if (props.loading || hasPendingDelete()) return;
  emit("open-add-category");
}

function onDeleteCategory() {
  if (props.loading || hasPendingDelete()) return;
  emit("delete-category", selectedCategory.value?.code);
}

function onOpenAddItem() {
  if (props.loading || hasPendingDelete()) return;
  emit("open-add");
}

const selectedCategory = computed(
  () =>
    props.categories.find((category) => category.code === props.selectedCategoryCode) ??
    props.categories[0] ??
    null,
);

function onCategoryDragStart(index, category, event) {
  if (props.loading || hasPendingDelete()) return;
  if (!Number.isInteger(index) || index < 0) return;
  const code = String(category?.code ?? "").trim();
  if (!code) return;
  if (isDeletingCategory(code)) return;
  dragFromCategoryIndex.value = index;
  dragInsertBeforeCategoryIndex.value = -1;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(index));
  }
}

function onCategoryDragOver(index, category, event) {
  if (props.loading || hasPendingDelete()) return;
  if (dragFromCategoryIndex.value < 0) return;
  const targetCode = String(category?.code ?? "").trim();
  if (!targetCode) return;
  if (isDeletingCategory(targetCode)) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move";
  }
  const insertBeforeIndex = resolveCategoryInsertBeforeIndex(index, event);
  const toIndex = resolveCategoryTargetIndex(dragFromCategoryIndex.value, insertBeforeIndex);
  if (!Number.isInteger(toIndex) || toIndex === dragFromCategoryIndex.value) {
    dragInsertBeforeCategoryIndex.value = -1;
    return;
  }
  dragInsertBeforeCategoryIndex.value = insertBeforeIndex;
}

function onCategoryDragLeave(index) {
  if (dragInsertBeforeCategoryIndex.value === index || dragInsertBeforeCategoryIndex.value === index + 1) {
    dragInsertBeforeCategoryIndex.value = -1;
  }
}

function onCategoryDrop(index, event) {
  if (props.loading || hasPendingDelete()) return;
  event.preventDefault();
  const fromRaw = event.dataTransfer?.getData("text/plain");
  const fromIndex = Number.parseInt(fromRaw || String(dragFromCategoryIndex.value), 10);
  if (!Number.isInteger(fromIndex) || fromIndex < 0) {
    resetCategoryDragState();
    return;
  }

  const insertBeforeIndex =
    dragInsertBeforeCategoryIndex.value >= 0
      ? dragInsertBeforeCategoryIndex.value
      : resolveCategoryInsertBeforeIndex(index, event);
  const toIndex = resolveCategoryTargetIndex(fromIndex, insertBeforeIndex);
  if (Number.isInteger(toIndex) && toIndex >= 0 && toIndex !== fromIndex) {
    emit("reorder-categories", { fromIndex, toIndex });
  }
  resetCategoryDragState();
}

function onCategoryDragEnd() {
  resetCategoryDragState();
}

function onItemDragStart(index, event) {
  if (props.loading || hasPendingDelete()) return;
  dragFromIndex.value = index;
  dragInsertBeforeIndex.value = -1;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(index));
  }
}

function onItemDragOver(index, event) {
  if (props.loading || hasPendingDelete()) return;
  if (dragFromIndex.value < 0) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move";
  }
  const insertBeforeIndex = resolveInsertBeforeIndex(index, event);
  const toIndex = resolveTargetIndex(dragFromIndex.value, insertBeforeIndex);
  if (!Number.isInteger(toIndex) || toIndex === dragFromIndex.value) {
    dragInsertBeforeIndex.value = -1;
    return;
  }
  dragInsertBeforeIndex.value = insertBeforeIndex;
}

function onItemDragLeave(index) {
  if (dragInsertBeforeIndex.value === index || dragInsertBeforeIndex.value === index + 1) {
    dragInsertBeforeIndex.value = -1;
  }
}

function onItemDrop(index, event) {
  if (props.loading || hasPendingDelete()) return;
  event.preventDefault();
  const fromRaw = event.dataTransfer?.getData("text/plain");
  const fromIndex = Number.parseInt(fromRaw || String(dragFromIndex.value), 10);
  if (!Number.isInteger(fromIndex) || fromIndex < 0) {
    resetDragState();
    return;
  }
  const insertBeforeIndex =
    dragInsertBeforeIndex.value >= 0 ? dragInsertBeforeIndex.value : resolveInsertBeforeIndex(index, event);
  const toIndex = resolveTargetIndex(fromIndex, insertBeforeIndex);
  if (Number.isInteger(toIndex) && toIndex >= 0 && toIndex !== fromIndex) {
    emit("reorder-items", { fromIndex, toIndex });
  }
  resetDragState();
}

function onItemDragEnd() {
  resetDragState();
}

function onDeleteItem(index) {
  if (props.loading || hasPendingDelete()) return;
  if (!Number.isInteger(index) || index < 0) return;
  emit("delete-item", { index });
}

function resolveCategoryInsertBeforeIndex(index, event) {
  const categoryCount = Array.isArray(props.categories) ? props.categories.length : 0;
  if (!Number.isInteger(index) || index < 0 || categoryCount <= 0) {
    return -1;
  }

  const rowElement = event?.currentTarget;
  const rect = rowElement?.getBoundingClientRect?.();
  if (!rect) {
    return Math.min(index + 1, categoryCount);
  }

  const midpoint = rect.top + rect.height / 2;
  const pointerY = Number(event?.clientY);
  const insertBefore = Number.isFinite(pointerY) && pointerY < midpoint ? index : index + 1;
  return Math.min(Math.max(insertBefore, 0), categoryCount);
}

function resolveCategoryTargetIndex(fromIndex, insertBeforeIndex) {
  if (!Number.isInteger(fromIndex) || fromIndex < 0) return -1;
  if (!Number.isInteger(insertBeforeIndex) || insertBeforeIndex < 0) return -1;
  let toIndex = insertBeforeIndex;
  if (insertBeforeIndex > fromIndex) {
    toIndex -= 1;
  }
  return toIndex;
}

function resolveInsertBeforeIndex(index, event) {
  const itemCount = Array.isArray(props.items) ? props.items.length : 0;
  if (!Number.isInteger(index) || index < 0 || itemCount <= 0) {
    return -1;
  }

  const rowElement = event?.currentTarget;
  const rect = rowElement?.getBoundingClientRect?.();
  if (!rect) {
    return Math.min(index + 1, itemCount);
  }

  const midpoint = rect.top + rect.height / 2;
  const pointerY = Number(event?.clientY);
  const insertBefore = Number.isFinite(pointerY) && pointerY < midpoint ? index : index + 1;
  return Math.min(Math.max(insertBefore, 0), itemCount);
}

function resolveTargetIndex(fromIndex, insertBeforeIndex) {
  if (!Number.isInteger(fromIndex) || fromIndex < 0) return -1;
  if (!Number.isInteger(insertBeforeIndex) || insertBeforeIndex < 0) return -1;
  let toIndex = insertBeforeIndex;
  if (insertBeforeIndex > fromIndex) {
    toIndex -= 1;
  }
  return toIndex;
}

function resetDragState() {
  dragFromIndex.value = -1;
  dragInsertBeforeIndex.value = -1;
}

function resetCategoryDragState() {
  dragFromCategoryIndex.value = -1;
  dragInsertBeforeCategoryIndex.value = -1;
}
</script>

<template>
  <section v-if="categories.length" class="panel">
    <h2 class="title">ハンディカテゴリ</h2>
    <div class="layout">
      <aside class="category-pane">
        <div class="category-head">
          <span class="category-head-label">カテゴリ</span>
          <div class="category-head-actions">
            <button
              class="category-add-btn"
              type="button"
              :disabled="loading || hasPendingDelete()"
              title="カテゴリ追加"
              @click="onOpenAddCategory"
            >
              +
            </button>
            <button
              class="category-delete-btn"
              type="button"
              :disabled="loading || hasPendingDelete() || !selectedCategory?.code"
              title="選択カテゴリ削除"
              @click="onDeleteCategory"
            >
              ×
            </button>
          </div>
        </div>
        <ul class="category-list">
          <li v-for="(category, index) in categories" :key="category.code">
            <button
              class="category-btn"
              :class="{
                active: category.code === selectedCategoryCode,
                'drag-source': index === dragFromCategoryIndex,
                'drop-before': dragInsertBeforeCategoryIndex === index,
                'drop-after-last':
                  dragInsertBeforeCategoryIndex === categories.length && index === categories.length - 1,
                'recent-edited': recentEditedCategoryCodes.includes(category.code),
                'pending-delete': isDeletingCategory(category.code),
              }"
              :disabled="loading || hasPendingDelete() || isDeletingCategory(category.code)"
              :draggable="!loading && !hasPendingDelete() && !isDeletingCategory(category.code)"
              @click="onSelectCategory(category.code)"
              @dragstart="onCategoryDragStart(index, category, $event)"
              @dragover="onCategoryDragOver(index, category, $event)"
              @dragleave="onCategoryDragLeave(index)"
              @drop="onCategoryDrop(index, $event)"
              @dragend="onCategoryDragEnd"
            >
              {{ category.description || category.code }}
            </button>
          </li>
        </ul>
      </aside>

      <div class="items-pane">
        <div class="items-head">
          <h3 class="items-title">
            {{ selectedCategory?.description || selectedCategory?.code || "-" }}
          </h3>
          <button
            class="add-btn"
            type="button"
            :disabled="loading || hasPendingDelete()"
            title="商品追加"
            @click="onOpenAddItem"
          >
            +
          </button>
        </div>

        <ul v-if="items.length" class="items">
          <li
            v-for="(item, index) in items"
            :key="`${item.itemCode}-${index}`"
            class="item-row"
            :class="{
              'drag-source': index === dragFromIndex,
              'drop-before': dragInsertBeforeIndex === index,
              'drop-after-last': dragInsertBeforeIndex === items.length && index === items.length - 1,
              'recent-edited': recentEditedItemKeys.includes(`${item.itemCode}-${index}`),
              'pending-delete': isDeletingItem(item, index),
            }"
            :draggable="!loading && !hasPendingDelete() && !isDeletingItem(item, index)"
            @dragstart="onItemDragStart(index, $event)"
            @dragover="onItemDragOver(index, $event)"
            @dragleave="onItemDragLeave(index)"
            @drop="onItemDrop(index, $event)"
            @dragend="onItemDragEnd"
          >
            <span class="item-code">{{ item.itemCode }}</span>
            <span class="item-name">{{ item.itemName || item.itemCode }}</span>
            <button
              class="remove-btn"
              type="button"
              title="商品を削除"
              :disabled="loading || hasPendingDelete() || isDeletingItem(item, index)"
              draggable="false"
              @dragstart.prevent
              @click.stop="onDeleteItem(index)"
            >
              ×
            </button>
          </li>
        </ul>
        <p v-else class="empty">このカテゴリに商品はありません</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  margin: 12px 0;
}
.title {
  margin: 0 0 10px;
  font-size: 16px;
}
.layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}
.category-pane {
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 8px;
  max-height: 560px;
  overflow: auto;
}
.category-list {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  display: grid;
  gap: 6px;
}
.category-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.category-head-label {
  font-size: 13px;
  font-weight: 700;
  color: #334;
}
.category-head-actions {
  display: inline-flex;
  gap: 6px;
}
.category-add-btn,
.category-delete-btn {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  line-height: 1;
  cursor: pointer;
}
.category-add-btn {
  border: 1px solid #6fce7e;
  background: #e9fbe9;
  color: #1f8b2b;
  font-weight: 700;
}
.category-delete-btn {
  border: 1px solid #f0b8b8;
  background: #fff5f5;
  color: #a22;
  font-size: 13px;
}
.category-add-btn:disabled,
.category-delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.category-btn {
  width: 100%;
  text-align: left;
  border: 1px solid #d6d9e0;
  border-radius: 999px;
  background: #f9fafc;
  color: #223;
  padding: 7px 12px;
  font-size: 13px;
  cursor: pointer;
  position: relative;
}
.category-btn.active {
  border-color: #7496d4;
  background: #eaf1ff;
  font-weight: 700;
}
.category-btn.drag-source {
  opacity: 0.6;
}
.category-btn.drop-before::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: -2px;
  height: 3px;
  background: #4a76c8;
  border-radius: 999px;
}
.category-btn.drop-after-last::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 3px;
  background: #4a76c8;
  border-radius: 999px;
}
.category-btn.recent-edited {
  border-color: #e7c66a;
  background: #fff9ea;
  box-shadow: inset 0 0 0 1px rgba(230, 188, 73, 0.35);
}
.category-btn.pending-delete {
  border-color: #e7c66a;
  background: #fff9ea;
  color: transparent;
  box-shadow: inset 0 0 0 1px rgba(230, 188, 73, 0.45);
}
.category-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.items-pane {
  border: 1px solid #edf0f5;
  border-radius: 10px;
  padding: 10px;
  min-height: 240px;
  max-height: 560px;
  display: flex;
  flex-direction: column;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.items-title {
  margin: 0 0 8px;
  font-size: 14px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f2f4f8;
  flex: 1 1 auto;
  flex-shrink: 0;
}
.add-btn {
  width: 24px;
  height: 24px;
  border: 1px solid #6fce7e;
  border-radius: 999px;
  background: #e9fbe9;
  color: #1f8b2b;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
}
.add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.items {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
  overflow: auto;
  min-height: 0;
  overscroll-behavior: contain;
  flex: 1 1 auto;
}
.item-row {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid #f2f4f8;
  align-items: center;
  cursor: grab;
  position: relative;
}
.item-row:last-child {
  border-bottom: none;
}
.item-row.drag-source {
  opacity: 0.55;
}
.item-row.drop-before::before {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: -2px;
  height: 3px;
  background: #4a76c8;
  border-radius: 999px;
}
.item-row.drop-after-last::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 3px;
  background: #4a76c8;
  border-radius: 999px;
}
.item-row.recent-edited {
  background: #fff9ea;
  border-radius: 6px;
}
.item-row.pending-delete {
  background: #fff9ea;
  border-radius: 6px;
}
.item-row.pending-delete .item-code,
.item-row.pending-delete .item-name {
  color: transparent;
}
.item-row.pending-delete .remove-btn {
  visibility: hidden;
}
.item-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: #334;
}
.item-name {
  color: #223;
}
.remove-btn {
  width: 22px;
  height: 22px;
  border: 1px solid #f0b8b8;
  border-radius: 999px;
  background: #fff5f5;
  color: #a22;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.12s ease;
}
.item-row:hover .remove-btn,
.item-row:focus-within .remove-btn {
  opacity: 1;
}
.remove-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.empty {
  margin: 8px 0 0;
  color: #667;
  font-size: 13px;
  overflow: auto;
  min-height: 0;
  overscroll-behavior: contain;
  flex: 1 1 auto;
}

@media (max-width: 680px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .category-pane {
    max-height: 240px;
  }
  .items-pane {
    max-height: 380px;
  }
  .item-row {
    grid-template-columns: 96px minmax(0, 1fr) auto;
    gap: 2px;
  }
}
</style>
