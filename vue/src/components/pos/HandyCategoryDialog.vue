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
  description: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["close", "submit", "update:description"]);

function onDescriptionInput(event) {
  emit("update:description", event.target.value);
}

function onSubmit() {
  emit("submit");
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click="emit('close')">
    <div class="dialog" @click.stop>
      <div class="dialog-head">
        <h2>ハンディカテゴリ追加</h2>
        <button class="close-btn" type="button" :disabled="loading" @click="emit('close')">×</button>
      </div>

      <p class="note">カテゴリコードは10以上の未使用最小値が自動採番されます。</p>

      <label class="field-label" for="handy-category-description">カテゴリ名</label>
      <input
        id="handy-category-description"
        class="field"
        type="text"
        :value="description"
        :disabled="loading"
        placeholder="例: スイーツ"
        @input="onDescriptionInput"
      />

      <div class="actions">
        <button class="submit-btn" type="button" :disabled="loading" @click="onSubmit">追加</button>
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
.dialog {
  width: min(480px, 100%);
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ddd;
  box-shadow: 0 14px 42px rgba(0, 0, 0, 0.2);
  padding: 14px;
  display: grid;
  gap: 10px;
}
.dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.dialog-head h2 {
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
.note {
  margin: 0;
  font-size: 12px;
  color: #556;
}
.field {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
}
.actions {
  display: flex;
  justify-content: flex-end;
}
.submit-btn {
  border: 1px solid #6fce7e;
  border-radius: 8px;
  background: #e9fbe9;
  color: #1f8b2b;
  font-weight: 700;
  padding: 7px 14px;
  cursor: pointer;
}
.submit-btn:disabled,
.close-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
