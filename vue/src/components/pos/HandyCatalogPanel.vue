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
  "swap-categories",
  "reorder-items",
  "delete-item",
  "open-add",
  "open-add-category",
  "delete-category",
]);
const dragFromIndex = ref(-1);
const dragOverIndex = ref(-1);
const dragSourceCategoryCode = ref("");
const dragOverCategoryCode = ref("");

const selectedCategory = computed(
  () =>
    props.categories.find((category) => category.code === props.selectedCategoryCode) ??
    props.categories[0] ??
    null,
);

function onCategoryDragStart(category, event) {
  if (props.loading) return;
  const code = String(category?.code ?? "").trim();
  if (!code) return;
  dragSourceCategoryCode.value = code;
  dragOverCategoryCode.value = "";
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", code);
  }
}

function onCategoryDragOver(category, event) {
  if (props.loading) return;
  const targetCode = String(category?.code ?? "").trim();
  if (!targetCode) return;
  if (!dragSourceCategoryCode.value || dragSourceCategoryCode.value === targetCode) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move";
  }
  dragOverCategoryCode.value = targetCode;
}

function onCategoryDragLeave(category) {
  const targetCode = String(category?.code ?? "").trim();
  if (targetCode && dragOverCategoryCode.value === targetCode) {
    dragOverCategoryCode.value = "";
  }
}

function onCategoryDrop(category, event) {
  if (props.loading) return;
  event.preventDefault();
  const targetCode = String(category?.code ?? "").trim();
  if (!targetCode) {
    resetCategoryDragState();
    return;
  }

  const fromRaw = event.dataTransfer?.getData("text/plain");
  const fromCode = String(fromRaw || dragSourceCategoryCode.value || "").trim();
  if (!fromCode || fromCode === targetCode) {
    resetCategoryDragState();
    return;
  }
  emit("swap-categories", { fromCategoryCode: fromCode, toCategoryCode: targetCode });
  resetCategoryDragState();
}

function onCategoryDragEnd() {
  resetCategoryDragState();
}

function onItemDragStart(index, event) {
  if (props.loading) return;
  dragFromIndex.value = index;
  dragOverIndex.value = -1;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(index));
  }
}

function onItemDragOver(index, event) {
  if (props.loading) return;
  if (dragFromIndex.value < 0) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move";
  }
  dragOverIndex.value = index;
}

function onItemDragLeave(index) {
  if (dragOverIndex.value === index) {
    dragOverIndex.value = -1;
  }
}

function onItemDrop(index, event) {
  if (props.loading) return;
  event.preventDefault();
  const fromRaw = event.dataTransfer?.getData("text/plain");
  const fromIndex = Number.parseInt(fromRaw || String(dragFromIndex.value), 10);
  if (!Number.isInteger(fromIndex) || fromIndex < 0) {
    resetDragState();
    return;
  }
  if (fromIndex !== index) {
    emit("reorder-items", { fromIndex, toIndex: index });
  }
  resetDragState();
}

function onItemDragEnd() {
  resetDragState();
}

function onDeleteItem(index) {
  if (props.loading) return;
  if (!Number.isInteger(index) || index < 0) return;
  emit("delete-item", { index });
}

function resetDragState() {
  dragFromIndex.value = -1;
  dragOverIndex.value = -1;
}

function resetCategoryDragState() {
  dragSourceCategoryCode.value = "";
  dragOverCategoryCode.value = "";
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
              :disabled="loading"
              title="カテゴリ追加"
              @click="emit('open-add-category')"
            >
              +
            </button>
            <button
              class="category-delete-btn"
              type="button"
              :disabled="loading || !selectedCategory?.code"
              title="選択カテゴリ削除"
              @click="emit('delete-category', selectedCategory?.code)"
            >
              ×
            </button>
          </div>
        </div>
        <ul class="category-list">
          <li v-for="category in categories" :key="category.code">
            <button
              class="category-btn"
              :class="{
                active: category.code === selectedCategoryCode,
                'drag-source': category.code === dragSourceCategoryCode,
                'drag-over': category.code === dragOverCategoryCode,
              }"
              :disabled="loading"
              :draggable="!loading"
              @click="emit('select-category', category.code)"
              @dragstart="onCategoryDragStart(category, $event)"
              @dragover="onCategoryDragOver(category, $event)"
              @dragleave="onCategoryDragLeave(category)"
              @drop="onCategoryDrop(category, $event)"
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
            :disabled="loading"
            title="商品追加"
            @click="emit('open-add')"
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
              'drag-over': index === dragOverIndex,
            }"
            :draggable="!loading"
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
              :disabled="loading"
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
}
.category-btn.active {
  border-color: #7496d4;
  background: #eaf1ff;
  font-weight: 700;
}
.category-btn.drag-source {
  opacity: 0.6;
}
.category-btn.drag-over {
  border-color: #4a76c8;
  background: #edf3ff;
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
}
.item-row:last-child {
  border-bottom: none;
}
.item-row.drag-source {
  opacity: 0.55;
}
.item-row.drag-over {
  background: #ecf3ff;
  border-radius: 6px;
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
