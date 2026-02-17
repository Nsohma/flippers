<script setup>
defineProps({
  page: {
    type: Object,
    default: null,
  },
  gridCells: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  dragState: {
    type: Object,
    required: true,
  },
  buttonClass: {
    type: Function,
    required: true,
  },
  formatPrice: {
    type: Function,
    required: true,
  },
});

const emit = defineEmits([
  "edit-grid",
  "open-add",
  "delete-button",
  "open-price",
  "button-drag-start",
  "cell-drag-over",
  "cell-drag-leave",
  "cell-drop",
  "button-drag-end",
]);
</script>

<template>
  <section v-if="page" class="panel">
    <div class="page-head">
      <div class="page-title"><b>Page:</b> {{ page.pageNumber }} ({{ page.cols }} x {{ page.rows }})</div>
      <button class="grid-edit-btn" type="button" :disabled="loading" @click="emit('edit-grid')">
        □ グリッド変更
      </button>
    </div>

    <div
      class="grid"
      :style="{
        gridTemplateColumns: `repeat(${page.cols}, 1fr)`,
        gridTemplateRows: `repeat(${page.rows}, 72px)`,
      }"
    >
      <div
        v-for="cell in gridCells"
        :key="cell.key"
        class="cell"
        :class="{
          'is-empty': !cell.button,
          'has-button': !!cell.button,
          'drag-source': dragState.sourceKey === cell.key,
          'drag-over': dragState.overKey === cell.key,
        }"
        @dragover="emit('cell-drag-over', cell, $event)"
        @dragleave="emit('cell-drag-leave', cell)"
        @drop="emit('cell-drop', cell, $event)"
      >
        <button
          v-if="cell.button"
          class="cell-btn"
          :class="buttonClass(cell.button.styleKey)"
          title="ドラッグして他のボタン位置にドロップで入れ替え"
          :draggable="!loading"
          @dragstart="emit('button-drag-start', cell, $event)"
          @dragend="emit('button-drag-end')"
        >
          <div class="label">{{ cell.button.label }}</div>
          <div class="sub">
            <span class="item-code">#{{ cell.button.itemCode }}</span>
            <span v-if="cell.button.unitPrice" class="unit-price">
              {{ formatPrice(cell.button.unitPrice) }}
            </span>
          </div>
        </button>

        <button
          v-if="cell.button"
          class="delete-trigger"
          title="このボタンを削除"
          :disabled="loading"
          @click.stop="emit('delete-button', cell.button)"
        >
          ×
        </button>

        <button
          v-if="cell.button"
          class="price-trigger"
          title="この商品の価格を変更"
          :disabled="loading"
          @click.stop="emit('open-price', cell.button)"
        >
          -
        </button>

        <div v-else class="empty">
          <button
            class="add-trigger"
            title="この空セルに商品を追加"
            :disabled="loading"
            @click.stop="emit('open-add', cell)"
          >
            +
          </button>
        </div>
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
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.page-title {
  margin: 0;
}
.grid-edit-btn {
  border: 1px solid #9ec1ef;
  border-radius: 999px;
  padding: 7px 12px;
  background: #f2f8ff;
  color: #1d4e89;
  font-size: 13px;
  cursor: pointer;
}
.grid-edit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.grid {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}
.cell {
  background: #fafafa;
  border: 1px dashed #e3e3e3;
  border-radius: 10px;
  padding: 6px;
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  position: relative;
}
.empty {
  width: 100%;
  height: 100%;
}
.cell-btn {
  width: 100%;
  border-radius: 10px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
  padding: 8px;
  text-align: left;
}
.add-trigger {
  position: absolute;
  right: 8px;
  top: 8px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 1px solid #2e9f4b;
  background: #37b85a;
  color: #fff;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.12s ease, transform 0.12s ease;
  transform: scale(0.9);
}
.is-empty:hover .add-trigger {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}
.add-trigger:disabled {
  opacity: 0.3;
  pointer-events: none;
}
.delete-trigger {
  position: absolute;
  right: 8px;
  top: 8px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 1px solid #c43a3a;
  background: #e24f4f;
  color: #fff;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.12s ease, transform 0.12s ease;
  transform: scale(0.9);
  z-index: 2;
}
.has-button:hover .delete-trigger {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}
.delete-trigger:disabled {
  opacity: 0.3;
  pointer-events: none;
}
.price-trigger {
  position: absolute;
  right: 8px;
  top: 34px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  border: 1px solid #2f6ac9;
  background: #3f86f6;
  color: #fff;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.12s ease, transform 0.12s ease;
  transform: scale(0.9);
  z-index: 2;
}
.has-button:hover .price-trigger {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}
.price-trigger:disabled {
  opacity: 0.3;
  pointer-events: none;
}
.drag-source {
  outline: 2px solid #6c8cff;
  outline-offset: 1px;
}
.drag-over {
  background: #eef3ff;
  border-color: #9cb1ff;
}
.label {
  font-weight: 700;
  line-height: 1.2;
}
.sub {
  opacity: 0.7;
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.item-code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unit-price {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.style-1 {
  border-color: #bbb;
}
.style-2 {
  border-color: #999;
}
.style-3 {
  border-color: #777;
}
.style-4 {
  border-color: #555;
}
.style-5 {
  border-color: #333;
}
.style-6 {
  border-color: #111;
}

@media (max-width: 680px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
