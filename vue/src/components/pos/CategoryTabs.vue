<script setup>
import { ref } from "vue";

const props = defineProps({
  categories: {
    type: Array,
    default: () => [],
  },
  selectedPageNumber: {
    type: Number,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  "select-page",
  "add-category",
  "delete-category",
  "delete-category-page",
  "swap-categories",
]);
const dragSourcePageNumber = ref(null);
const dragOverPageNumber = ref(null);
const trashOver = ref(false);

function onTabDragStart(category, event) {
  if (props.loading) return;
  dragSourcePageNumber.value = category.pageNumber;
  dragOverPageNumber.value = null;
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(category.pageNumber));
  }
}

function onTabDragOver(category, event) {
  if (props.loading) return;
  if (dragSourcePageNumber.value == null) return;
  if (dragSourcePageNumber.value === category.pageNumber) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move";
  }
  dragOverPageNumber.value = category.pageNumber;
}

function onTabDrop(category, event) {
  if (props.loading) return;
  event.preventDefault();

  const fromRaw = event.dataTransfer?.getData("text/plain");
  const fromPageNumber = Number.parseInt(fromRaw || String(dragSourcePageNumber.value ?? ""), 10);
  if (!Number.isInteger(fromPageNumber)) {
    resetDragState();
    return;
  }
  if (fromPageNumber === category.pageNumber) {
    resetDragState();
    return;
  }
  emit("swap-categories", {
    fromPageNumber,
    toPageNumber: category.pageNumber,
  });
  resetDragState();
}

function onTabDragLeave(category) {
  if (dragOverPageNumber.value === category.pageNumber) {
    dragOverPageNumber.value = null;
  }
}

function onTabDragEnd() {
  resetDragState();
}

function onTrashDragOver(event) {
  if (props.loading) return;
  if (dragSourcePageNumber.value == null) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = "move";
  }
  trashOver.value = true;
}

function onTrashDragLeave() {
  trashOver.value = false;
}

function onTrashDrop(event) {
  if (props.loading) return;
  event.preventDefault();
  const fromRaw = event.dataTransfer?.getData("text/plain");
  const fromPageNumber = Number.parseInt(fromRaw || String(dragSourcePageNumber.value ?? ""), 10);
  if (!Number.isInteger(fromPageNumber)) {
    resetDragState();
    return;
  }
  emit("delete-category-page", { pageNumber: fromPageNumber });
  resetDragState();
}

function resetDragState() {
  dragSourcePageNumber.value = null;
  dragOverPageNumber.value = null;
  trashOver.value = false;
}
</script>

<template>
  <section class="panel">
    <div class="tabs-row">
      <div class="tabs-left">
        <button
          v-for="category in categories"
          :key="category.pageNumber"
          class="tab"
          :class="{
            active: category.pageNumber === selectedPageNumber,
            'drag-source': category.pageNumber === dragSourcePageNumber,
            'drag-over': category.pageNumber === dragOverPageNumber,
          }"
          :draggable="!loading"
          @click="emit('select-page', category.pageNumber)"
          @dragstart="onTabDragStart(category, $event)"
          @dragover="onTabDragOver(category, $event)"
          @dragleave="onTabDragLeave(category)"
          @drop="onTabDrop(category, $event)"
          @dragend="onTabDragEnd"
        >
          {{ category.name }}
        </button>
      </div>

      <div class="tabs-right">
        <button
          class="trash-drop"
          type="button"
          :class="{ 'trash-over': trashOver }"
          :disabled="loading"
          title="カテゴリをここにドロップして削除"
          @click="emit('delete-category')"
          @dragover="onTrashDragOver"
          @dragleave="onTrashDragLeave"
          @drop="onTrashDrop"
        >
          🗑
        </button>

        <button class="category-add-btn" :disabled="loading" @click="emit('add-category')">
          + カテゴリ追加
        </button>
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
.tabs-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tabs-left {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
  flex: 1 1 auto;
  min-width: 0;
}
.tabs-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
}
.tab {
  padding: 8px 10px;
  border-radius: 999px;
  border: 1px solid #ddd;
  background: #fafafa;
  cursor: pointer;
}
.tab.active {
  border-color: #999;
  background: #f0f0f0;
  font-weight: 700;
}
.tab.drag-source {
  opacity: 0.6;
}
.tab.drag-over {
  border-color: #4a76c8;
  background: #edf3ff;
}
.category-add-btn {
  border: 1px solid #ccc;
  border-radius: 999px;
  padding: 7px 12px;
  cursor: pointer;
  font-size: 13px;
}
.category-add-btn {
  background: #f3fbf4;
  border-color: #95d3a0;
  color: #165e25;
}
.category-add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.trash-drop {
  margin-left: 4px;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid #c8c8c8;
  background: #f7f7f7;
  color: #555;
  font-size: 18px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.trash-drop.trash-over {
  border-color: #6c8cff;
  background: #eef3ff;
}
.trash-drop:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .tabs-row {
    flex-direction: column;
    align-items: stretch;
  }
  .tabs-right {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>
