<script setup>
import { computed, reactive, ref } from "vue";

const envApiBase = (import.meta.env.VITE_API_BASE ?? "").trim();
const API_BASE = (envApiBase || "/api/pos").replace(/\/+$/, "");

const state = reactive({
  draftId: "",
  categories: [],
  page: null,
  selectedPageNumber: null,
  loading: false,
  error: "",
});

const fileRef = ref(null);

// (col,row) -> button を引けるMapにしておくと描画が楽
const buttonMap = computed(() => {
  const map = new Map();
  if (!state.page?.buttons) return map;
  for (const b of state.page.buttons) {
    map.set(`${b.col}-${b.row}`, b);
  }
  return map;
});

const gridCells = computed(() => {
  if (!state.page) return [];
  const cells = [];
  for (let r = 1; r <= state.page.rows; r++) {
    for (let c = 1; c <= state.page.cols; c++) {
      const key = `${c}-${r}`;
      cells.push({ col: c, row: r, key, button: buttonMap.value.get(key) ?? null });
    }
  }
  return cells;
});

async function importExcel() {
  state.error = "";
  const f = fileRef.value?.files?.[0];
  if (!f) {
    state.error = "Excelファイルを選択してください";
    return;
  }

  const form = new FormData();
  form.append("file", f);

  state.loading = true;
  try {
    const res = await fetch(`${API_BASE}/import`, { method: "POST", body: form });
    if (!res.ok) throw new Error(`Import failed: ${res.status}`);
    const data = await res.json();

    state.draftId = data.draftId;
    state.categories = data.categories ?? [];
    state.page = data.initialPage ?? null;
    state.selectedPageNumber = state.page?.pageNumber ?? (state.categories[0]?.pageNumber ?? null);
  } catch (e) {
    state.error = String(e);
  } finally {
    state.loading = false;
  }
}

async function loadPage(pageNumber) {
  if (!state.draftId) return;
  state.error = "";
  state.loading = true;
  try {
    const res = await fetch(`${API_BASE}/drafts/${state.draftId}/pages/${pageNumber}`);
    if (!res.ok) throw new Error(`Get page failed: ${res.status}`);
    const page = await res.json();
    state.page = page;
    state.selectedPageNumber = pageNumber;
  } catch (e) {
    state.error = String(e);
  } finally {
    state.loading = false;
  }
}

// styleKeyは今は “表示だけ” にして、まず動かす（後で色マップにする）
function buttonClass(styleKey) {
  return `btn style-${styleKey}`;
}
</script>

<template>
  <div class="wrap">
    <h1>Flippers POS Key Editor (MVP)</h1>

    <section class="panel">
      <input ref="fileRef" type="file" accept=".xlsx" />
      <button @click="importExcel" :disabled="state.loading">Import</button>

      <div class="meta" v-if="state.draftId">
        <div><b>draftId</b>: {{ state.draftId }}</div>
      </div>

      <div class="error" v-if="state.error">{{ state.error }}</div>
    </section>

    <section class="panel" v-if="state.categories.length">
      <div class="tabs">
        <button
          v-for="c in state.categories"
          :key="c.pageNumber"
          class="tab"
          :class="{ active: c.pageNumber === state.selectedPageNumber }"
          @click="loadPage(c.pageNumber)"
        >
          {{ c.name }}
        </button>
      </div>
    </section>

    <section class="panel" v-if="state.page">
      <div class="page-title">
        <b>Page:</b> {{ state.page.pageNumber }} ({{ state.page.cols }} x {{ state.page.rows }})
      </div>

      <div
        class="grid"
        :style="{
          gridTemplateColumns: `repeat(${state.page.cols}, 1fr)`,
          gridTemplateRows: `repeat(${state.page.rows}, 72px)`,
        }"
      >
        <div v-for="cell in gridCells" :key="cell.key" class="cell">
          <button
            v-if="cell.button"
            class="cell-btn"
            :class="buttonClass(cell.button.styleKey)"
            title="(あとで編集ここから)"
          >
            <div class="label">{{ cell.button.label }}</div>
            <div class="sub">#{{ cell.button.itemCode }}</div>
          </button>
          <div v-else class="empty"></div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.wrap { max-width: 1100px; margin: 24px auto; padding: 0 16px; font-family: system-ui, -apple-system, sans-serif; }
.panel { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; margin: 12px 0; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.tab { padding: 8px 10px; border-radius: 999px; border: 1px solid #ddd; background: #fafafa; cursor: pointer; }
.tab.active { border-color: #999; background: #f0f0f0; font-weight: 700; }
.grid { display: grid; gap: 8px; margin-top: 12px; }
.cell { background: #fafafa; border: 1px dashed #e3e3e3; border-radius: 10px; padding: 6px; display: flex; align-items: stretch; justify-content: stretch; }
.empty { width: 100%; height: 100%; }
.cell-btn { width: 100%; border-radius: 10px; border: 1px solid #ddd; background: #fff; cursor: pointer; padding: 8px; text-align: left; }
.label { font-weight: 700; line-height: 1.2; }
.sub { opacity: .7; font-size: 12px; margin-top: 4px; }

/* いったんstyleKeyは軽い差だけ（あとであなたの色仕様に合わせて本実装） */
.style-1 { border-color: #bbb; }
.style-2 { border-color: #999; }
.style-3 { border-color: #777; }
.style-4 { border-color: #555; }
.style-5 { border-color: #333; }
.style-6 { border-color: #111; }

.error { color: #b00020; margin-top: 8px; white-space: pre-wrap; }
.meta { margin-top: 8px; font-size: 13px; }
.page-title { margin-bottom: 8px; }
</style>
