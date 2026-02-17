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
const dragState = reactive({
  sourceKey: "",
  overKey: "",
});

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

async function exportExcel() {
  if (!state.draftId) {
    state.error = "draftIdがありません";
    return;
  }

  state.error = "";
  state.loading = true;
  try {
    const res = await fetch(`${API_BASE}/drafts/${state.draftId}/export`);
    if (!res.ok) {
      let message = `Export failed: ${res.status}`;
      try {
        const body = await res.json();
        if (body?.message) message = body.message;
      } catch {
        // ignore json parse error and use default message
      }
      throw new Error(message);
    }

    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `${state.draftId}_edited.xlsx`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
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

function formatPrice(unitPrice) {
  const raw = String(unitPrice ?? "").trim();
  if (!raw) return "";

  const normalized = raw.replace(/,/g, "");
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) {
    return raw;
  }

  const number = Number(normalized);
  if (!Number.isFinite(number)) {
    return raw;
  }

  if (Number.isInteger(number)) {
    return `¥${number.toLocaleString("ja-JP")}`;
  }
  return `¥${number.toLocaleString("ja-JP", { maximumFractionDigits: 2 })}`;
}

function resetDragState() {
  dragState.sourceKey = "";
  dragState.overKey = "";
}

function onButtonDragStart(cell, event) {
  if (!cell?.button || state.loading) return;
  dragState.sourceKey = cell.key;
  dragState.overKey = "";
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", cell.key);
  }
}

function onCellDragOver(cell, event) {
  if (state.loading) return;
  if (!dragState.sourceKey) return;
  if (!cell?.button) return;
  if (cell.key === dragState.sourceKey) return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = "move";
  dragState.overKey = cell.key;
}

function onCellDragLeave(cell) {
  if (dragState.overKey === cell.key) {
    dragState.overKey = "";
  }
}

async function onCellDrop(cell, event) {
  if (state.loading) return;
  const sourceKey = event.dataTransfer?.getData("text/plain") || dragState.sourceKey;
  if (!sourceKey) return;
  event.preventDefault();

  if (!state.draftId || !state.page) {
    resetDragState();
    return;
  }

  if (!cell?.button || cell.key === sourceKey) {
    resetDragState();
    return;
  }

  const sourceButton = buttonMap.value.get(sourceKey);
  const targetButton = buttonMap.value.get(cell.key);
  if (!sourceButton || !targetButton) {
    resetDragState();
    return;
  }

  state.error = "";
  state.loading = true;
  try {
    const res = await fetch(
      `${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons/swap`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fromCol: sourceButton.col,
          fromRow: sourceButton.row,
          toCol: targetButton.col,
          toRow: targetButton.row,
        }),
      },
    );
    if (!res.ok) {
      let message = `Swap failed: ${res.status}`;
      try {
        const body = await res.json();
        if (body?.message) message = body.message;
      } catch {
        // ignore json parse error and use default message
      }
      throw new Error(message);
    }

    const page = await res.json();
    state.page = page;
  } catch (e) {
    state.error = String(e);
  } finally {
    state.loading = false;
    resetDragState();
  }
}

function onButtonDragEnd() {
  resetDragState();
}
</script>

<template>
  <div class="wrap">
    <h1>Flippers POS Key Editor (MVP)</h1>

    <section class="panel">
      <input ref="fileRef" type="file" accept=".xlsx" />
      <button @click="importExcel" :disabled="state.loading">Import</button>
      <button @click="exportExcel" :disabled="state.loading || !state.draftId">Export</button>

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
        <div
          v-for="cell in gridCells"
          :key="cell.key"
          class="cell"
          :class="{
            'drag-source': dragState.sourceKey === cell.key,
            'drag-over': dragState.overKey === cell.key && cell.button,
          }"
          @dragover="onCellDragOver(cell, $event)"
          @dragleave="onCellDragLeave(cell)"
          @drop="onCellDrop(cell, $event)"
        >
          <button
            v-if="cell.button"
            class="cell-btn"
            :class="buttonClass(cell.button.styleKey)"
            title="ドラッグして他のボタン位置にドロップで入れ替え"
            :draggable="!state.loading"
            @dragstart="onButtonDragStart(cell, $event)"
            @dragend="onButtonDragEnd"
          >
            <div class="label">{{ cell.button.label }}</div>
            <div class="sub">
              <span class="item-code">#{{ cell.button.itemCode }}</span>
              <span v-if="cell.button.unitPrice" class="unit-price">
                {{ formatPrice(cell.button.unitPrice) }}
              </span>
            </div>
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
.drag-source { outline: 2px solid #6c8cff; outline-offset: 1px; }
.drag-over { background: #eef3ff; border-color: #9cb1ff; }
.label { font-weight: 700; line-height: 1.2; }
.sub { opacity: .7; font-size: 12px; margin-top: 4px; display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.item-code { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.unit-price { font-variant-numeric: tabular-nums; white-space: nowrap; }

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
