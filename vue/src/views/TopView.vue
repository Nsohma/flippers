<script setup>
import { inject, ref } from "vue";
import { RouterLink } from "vue-router";
import { usePosDraft } from "../composables/usePosDraft";

const envApiBase = (import.meta.env.VITE_API_BASE ?? "").trim();
const API_BASE = (envApiBase || "/api/pos").replace(/\/+$/, "");
const toggleSidebar = inject("toggleSidebar", null);
const fileRef = ref(null);
const { state, importExcel, exportExcel } = usePosDraft(API_BASE);

async function onImportClick() {
  const file = fileRef.value?.files?.[0];
  await importExcel(file);
}

function onToggleSidebar() {
  if (typeof toggleSidebar === "function") {
    toggleSidebar();
  }
}
</script>

<template>
  <div class="wrap">
    <section class="panel">
      <div class="title-row">
        <button type="button" class="menu-toggle" aria-label="サイドバーを開閉" @click="onToggleSidebar">☰</button>
        <h1>Excel インポート・エクスポート</h1>
      </div>

      <p class="hint">ここでExcelをImportすると、編集画面と商品カタログの両方で同じデータを利用できます。</p>

      <div class="import-row">
        <input ref="fileRef" type="file" accept=".xlsx" />
        <button :disabled="state.loading" @click="onImportClick">Import</button>
      </div>

      <div class="export-row">
        <button class="export-btn" :disabled="state.loading || !state.draftId" @click="exportExcel">Export</button>
      </div>

      <div v-if="state.draftId" class="meta"><b>draftId</b>: {{ state.draftId }}</div>
      <div v-if="state.error" class="error">{{ state.error }}</div>

      <div class="jump-links">
        <RouterLink class="jump-link" to="/pos">レジキー編集へ</RouterLink>
        <RouterLink class="jump-link" to="/handy">ハンディ編集へ</RouterLink>
        <RouterLink class="jump-link" to="/catalog">商品カタログへ</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 1100px;
  margin: 24px auto;
  padding: 0 16px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e9f2;
  border-radius: 12px;
  padding: 18px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

h1 {
  margin: 0;
  font-size: 28px;
}

.menu-toggle {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #c7d1e4;
  background: #fff;
  color: #344862;
  font-size: 18px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.menu-toggle:hover {
  background: #f5f8ff;
}

.lead {
  margin: 12px 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #334760;
}

.hint {
  margin: 0 0 12px;
  color: #4a5a70;
  font-size: 14px;
}

.import-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.import-row button {
  border: 1px solid #d1d8e5;
  border-radius: 8px;
  background: #fff;
  color: #234;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
}

.import-row button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.export-row {
  margin-top: 10px;
}

.export-btn {
  min-width: 120px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid #1f3f6b;
  background: #244a80;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  padding: 0 16px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(36, 74, 128, 0.28);
}

.export-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}

.meta {
  margin-top: 10px;
  font-size: 13px;
}

.error {
  margin-top: 10px;
  color: #b00020;
  white-space: pre-wrap;
}

.jump-links {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.jump-link {
  text-decoration: none;
  border: 1px solid #d2dcef;
  border-radius: 999px;
  padding: 5px 10px;
  color: #2a3d59;
  background: #f7faff;
  font-size: 13px;
}

.jump-link:hover {
  border-color: #9fb3d8;
}
</style>
