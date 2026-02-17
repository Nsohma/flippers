<script setup>
defineProps({
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

const emit = defineEmits(["select-page", "add-category", "delete-category"]);
</script>

<template>
  <section class="panel">
    <div class="tabs tabs-row">
      <button
        v-for="category in categories"
        :key="category.pageNumber"
        class="tab"
        :class="{ active: category.pageNumber === selectedPageNumber }"
        @click="emit('select-page', category.pageNumber)"
      >
        {{ category.name }}
      </button>

      <div class="category-actions">
        <button class="category-add-btn" :disabled="loading" @click="emit('add-category')">
          + カテゴリ追加
        </button>
        <button
          class="category-delete-btn"
          :disabled="loading || !selectedPageNumber"
          @click="emit('delete-category')"
        >
          × 選択カテゴリ削除
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
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tabs-row {
  align-items: center;
  justify-content: space-between;
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
.category-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
.category-add-btn,
.category-delete-btn {
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
.category-delete-btn {
  background: #fff3f3;
  border-color: #e2a3a3;
  color: #8c2020;
}
.category-add-btn:disabled,
.category-delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .tabs-row {
    justify-content: flex-start;
  }
  .category-actions {
    margin-left: 0;
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
