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
  categoryName: {
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
});

const emit = defineEmits(["close", "submit", "update:cols", "update:rows"]);

function onInput(eventName, event) {
  emit(eventName, event.target.value);
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click="emit('close')">
    <div class="grid-modal" @click.stop>
      <div class="modal-head">
        <h2>グリッド変更</h2>
        <button class="close-btn" type="button" @click="emit('close')">×</button>
      </div>

      <p class="target-name">対象カテゴリ: {{ categoryName || "-" }}</p>

      <div class="grid-form">
        <label class="field-label">
          列数
          <input
            class="number-input"
            type="number"
            min="1"
            :value="cols"
            @input="onInput('update:cols', $event)"
          />
        </label>

        <label class="field-label">
          行数
          <input
            class="number-input"
            type="number"
            min="1"
            :value="rows"
            @input="onInput('update:rows', $event)"
          />
        </label>
      </div>

      <div class="actions">
        <button class="cancel" type="button" :disabled="loading" @click="emit('close')">キャンセル</button>
        <button class="save" type="button" :disabled="loading" @click="emit('submit')">変更する</button>
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
.grid-modal {
  width: min(380px, 100%);
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
.target-name {
  margin: 0;
  font-size: 13px;
  color: #555;
}
.field-label {
  font-size: 13px;
  font-weight: 600;
  display: grid;
  gap: 4px;
}
.grid-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.number-input {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
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
  .grid-form {
    grid-template-columns: 1fr;
  }
}
</style>
