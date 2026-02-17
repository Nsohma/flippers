<script setup>
defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  targetCol: {
    type: Number,
    default: 0,
  },
  targetRow: {
    type: Number,
    default: 0,
  },
  categories: {
    type: Array,
    default: () => [],
  },
  categoryCode: {
    type: String,
    default: "",
  },
  search: {
    type: String,
    default: "",
  },
  items: {
    type: Array,
    default: () => [],
  },
  formatPrice: {
    type: Function,
    required: true,
  },
});

const emit = defineEmits(["close", "update:categoryCode", "update:search", "select-item"]);

function onCategoryChange(event) {
  emit("update:categoryCode", event.target.value);
}

function onSearchInput(event) {
  emit("update:search", event.target.value);
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click="emit('close')">
    <div class="add-modal" @click.stop>
      <div class="add-modal-head">
        <h2>空セルへ商品を追加</h2>
        <button class="close-btn" type="button" @click="emit('close')">×</button>
      </div>

      <div class="add-target">追加先セル: ({{ targetCol }}, {{ targetRow }})</div>

      <label class="field-label" for="add-category-select">カテゴリ</label>
      <select
        id="add-category-select"
        class="category-select"
        :value="categoryCode"
        @change="onCategoryChange"
      >
        <option v-for="category in categories" :key="category.code" :value="category.code">
          {{ category.description }} ({{ category.code }})
        </option>
      </select>

      <input
        class="item-search"
        type="text"
        placeholder="商品名 / ItemCode で検索"
        :value="search"
        @input="onSearchInput"
      />

      <div class="item-list">
        <button
          v-for="item in items"
          :key="item.itemCode"
          class="item-option"
          type="button"
          :disabled="loading"
          @click="emit('select-item', item)"
        >
          <span class="item-option-name">{{ item.itemName }}</span>
          <span class="item-option-sub">
            #{{ item.itemCode }}
            <span v-if="item.unitPrice">{{ formatPrice(item.unitPrice) }}</span>
          </span>
        </button>
        <div v-if="!items.length" class="item-empty">該当商品がありません</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 20;
}
.add-modal {
  width: min(720px, 100%);
  max-height: min(82vh, 720px);
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ddd;
  box-shadow: 0 14px 42px rgba(0, 0, 0, 0.2);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.add-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.add-modal-head h2 {
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
.add-target {
  font-size: 13px;
  color: #555;
}
.field-label {
  font-size: 13px;
  font-weight: 600;
}
.category-select,
.item-search {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
}
.item-list {
  border: 1px solid #ececec;
  border-radius: 10px;
  max-height: 50vh;
  overflow: auto;
  padding: 8px;
  display: grid;
  gap: 6px;
  background: #fbfbfb;
}
.item-option {
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.item-option:hover {
  border-color: #9ab7ff;
  background: #f5f8ff;
}
.item-option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.item-option-name {
  font-weight: 600;
}
.item-option-sub {
  font-size: 12px;
  color: #666;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.item-empty {
  color: #666;
  font-size: 13px;
  padding: 8px 4px;
}
</style>
