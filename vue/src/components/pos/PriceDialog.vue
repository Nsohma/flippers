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
  label: {
    type: String,
    default: "",
  },
  itemCode: {
    type: String,
    default: "",
  },
  unitPrice: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["close", "submit", "update:unitPrice"]);

function onInput(event) {
  emit("update:unitPrice", event.target.value);
}
</script>

<template>
  <div v-if="open" class="modal-backdrop" @click="emit('close')">
    <div class="price-modal" @click.stop>
      <div class="modal-head">
        <h2>価格変更</h2>
        <button class="close-btn" type="button" @click="emit('close')">×</button>
      </div>

      <div class="target">{{ label }}</div>
      <div class="target">#{{ itemCode }}</div>

      <label class="field-label" for="unit-price-input">新しい価格</label>
      <input
        id="unit-price-input"
        class="item-search"
        type="text"
        inputmode="decimal"
        placeholder="例: 1280"
        :value="unitPrice"
        @input="onInput"
        @keydown.enter.prevent="emit('submit')"
      />

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
.price-modal {
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
.target {
  font-size: 13px;
  color: #555;
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
</style>
