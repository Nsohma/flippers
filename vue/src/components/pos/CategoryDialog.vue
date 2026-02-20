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
  name: {
    type: String,
    default: "",
  },
  cols: {
    type: String,
    default: "5",
  },
  rows: {
    type: String,
    default: "5",
  },
  styleKey: {
    type: String,
    default: "1",
  },
});

const emit = defineEmits([
  "close",
  "submit",
  "update:name",
  "update:cols",
  "update:rows",
  "update:styleKey",
]);

function onInput(eventName, event) {
  emit(eventName, event.target.value);
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click="emit('close')">
    <div class="category-modal" @click.stop>
      <div class="modal-head">
        <h2>カテゴリ追加</h2>
        <button class="close-btn" type="button" @click="emit('close')">×</button>
      </div>

      <label class="field-label" for="category-name-input">カテゴリ名</label>
      <input
        id="category-name-input"
        class="item-search"
        type="text"
        placeholder="例: 新カテゴリ"
        :value="name"
        @input="onInput('update:name', $event)"
      />

      <div class="category-grid-form">
        <label class="field-label">
          列数
          <input
            class="item-search"
            type="number"
            min="1"
            :value="cols"
            @input="onInput('update:cols', $event)"
          />
        </label>
        <label class="field-label">
          行数
          <input
            class="item-search"
            type="number"
            min="1"
            :value="rows"
            @input="onInput('update:rows', $event)"
          />
        </label>
        <label class="field-label">
          styleKey
          <input
            class="item-search"
            type="number"
            min="1"
            :value="styleKey"
            @input="onInput('update:styleKey', $event)"
          />
        </label>
      </div>

      <div class="actions">
        <button class="cancel" type="button" :disabled="loading" @click="emit('close')">キャンセル</button>
        <button class="save" type="button" :disabled="loading" @click="emit('submit')">追加する</button>
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
.category-modal {
  width: min(420px, 100%);
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ddd;
  box-shadow: 0 14px 42px rgba(0, 0, 0, 0.2);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.modal-head h2 {
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
.field-label {
  font-size: 13px;
  font-weight: 600;
}
.item-search {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
}
.category-grid-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.category-grid-form .field-label {
  display: grid;
  gap: 4px;
  min-width: 0;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 2px;
}
.cancel,
.save {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
}
.cancel {
  background: #f6f6f6;
}
.save {
  background: #3f86f6;
  border-color: #2f6ac9;
  color: #fff;
}

@media (max-width: 680px) {
  .category-grid-form {
    grid-template-columns: 1fr;
  }
}
</style>
