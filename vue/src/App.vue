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
const catalogState = reactive({
  loaded: false,
  loading: false,
  categories: [],
});
const addDialog = reactive({
  open: false,
  targetCol: 0,
  targetRow: 0,
  categoryCode: "",
  search: "",
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

const selectedAddCategory = computed(() => {
  if (!addDialog.categoryCode) return null;
  return catalogState.categories.find((c) => c.code === addDialog.categoryCode) ?? null;
});

const filteredAddItems = computed(() => {
  const items = selectedAddCategory.value?.items ?? [];
  const keyword = addDialog.search.trim().toLowerCase();
  if (!keyword) return items;

  return items.filter((item) => {
    const code = String(item.itemCode ?? "").toLowerCase();
    const name = String(item.itemName ?? "").toLowerCase();
    return code.includes(keyword) || name.includes(keyword);
  });
});

async function readApiError(res, fallbackMessage) {
  let message = fallbackMessage;
  try {
    const body = await res.json();
    if (body?.message) {
      message = body.message;
    }
  } catch {
    // ignore json parse error and use fallback message
  }
  return message;
}

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
    closeAddDialog();
    resetCatalogState();
    void loadItemCatalog();
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
    closeAddDialog();
  } catch (e) {
    state.error = String(e);
  } finally {
    state.loading = false;
  }
}

function resetCatalogState() {
  catalogState.loaded = false;
  catalogState.loading = false;
  catalogState.categories = [];
}

async function loadItemCatalog() {
  if (!state.draftId || catalogState.loading) return;

  catalogState.loading = true;
  try {
    const res = await fetch(`${API_BASE}/drafts/${state.draftId}/item-categories`);
    if (!res.ok) {
      throw new Error(await readApiError(res, `Get item categories failed: ${res.status}`));
    }
    const data = await res.json();
    const categories = Array.isArray(data?.categories) ? data.categories : [];
    catalogState.categories = categories;
    catalogState.loaded = true;
  } catch (e) {
    catalogState.loaded = false;
    state.error = String(e);
  } finally {
    catalogState.loading = false;
  }
}

async function ensureItemCatalogLoaded() {
  if (catalogState.loaded && catalogState.categories.length > 0) return true;
  await loadItemCatalog();
  return catalogState.loaded && catalogState.categories.length > 0;
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
      throw new Error(await readApiError(res, `Export failed: ${res.status}`));
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

function closeAddDialog() {
  addDialog.open = false;
  addDialog.targetCol = 0;
  addDialog.targetRow = 0;
  addDialog.categoryCode = "";
  addDialog.search = "";
}

async function openAddDialog(cell) {
  if (state.loading) return;
  if (!cell || cell.button) return;
  if (!state.draftId || !state.page) return;

  state.error = "";
  const ready = await ensureItemCatalogLoaded();
  if (!ready) {
    state.error = "カテゴリまたは商品情報が見つかりません";
    return;
  }

  addDialog.targetCol = cell.col;
  addDialog.targetRow = cell.row;
  addDialog.categoryCode = catalogState.categories[0]?.code ?? "";
  addDialog.search = "";
  addDialog.open = true;
}

async function addButtonFromCatalog(item) {
  if (state.loading) return;
  if (!addDialog.open || !state.page || !state.draftId) return;
  if (!item?.itemCode) return;
  if (!addDialog.categoryCode) {
    state.error = "カテゴリを選択してください";
    return;
  }

  state.error = "";
  state.loading = true;
  try {
    const res = await fetch(
      `${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          col: addDialog.targetCol,
          row: addDialog.targetRow,
          categoryCode: addDialog.categoryCode,
          itemCode: item.itemCode,
        }),
      },
    );
    if (!res.ok) {
      throw new Error(await readApiError(res, `Add button failed: ${res.status}`));
    }
    state.page = await res.json();
    closeAddDialog();
  } catch (e) {
    state.error = String(e);
  } finally {
    state.loading = false;
  }
}

async function deleteButton(button) {
  if (state.loading) return;
  if (!button?.buttonId) {
    state.error = "buttonIdが見つかりません";
    return;
  }
  if (!state.page || !state.draftId) return;

  state.error = "";
  state.loading = true;
  try {
    const res = await fetch(
      `${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons`,
      {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ buttonId: button.buttonId }),
      },
    );
    if (!res.ok) {
      throw new Error(await readApiError(res, `Delete button failed: ${res.status}`));
    }

    state.page = await res.json();
  } catch (e) {
    state.error = String(e);
  } finally {
    state.loading = false;
  }
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
  if (!cell) return;
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

  if (!cell || cell.key === sourceKey) {
    resetDragState();
    return;
  }

  const sourceButton = buttonMap.value.get(sourceKey);
  if (!sourceButton) {
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
          toCol: cell.col,
          toRow: cell.row,
        }),
      },
    );
    if (!res.ok) {
      throw new Error(await readApiError(res, `Swap failed: ${res.status}`));
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
            'is-empty': !cell.button,
            'has-button': !!cell.button,
            'drag-source': dragState.sourceKey === cell.key,
            'drag-over': dragState.overKey === cell.key,
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
          <button
            v-if="cell.button"
            class="delete-trigger"
            title="このボタンを削除"
            :disabled="state.loading"
            @click.stop="deleteButton(cell.button)"
          >
            ×
          </button>
          <div v-else class="empty">
            <button
              class="add-trigger"
              title="この空セルに商品を追加"
              :disabled="state.loading"
              @click.stop="openAddDialog(cell)"
            >
              +
            </button>
          </div>
        </div>
      </div>
    </section>

    <div v-if="addDialog.open" class="modal-backdrop" @click="closeAddDialog">
      <div class="add-modal" @click.stop>
        <div class="add-modal-head">
          <h2>空セルへ商品を追加</h2>
          <button class="close-btn" type="button" @click="closeAddDialog">×</button>
        </div>

        <div class="add-target">
          追加先セル: ({{ addDialog.targetCol }}, {{ addDialog.targetRow }})
        </div>

        <label class="field-label" for="add-category-select">カテゴリ</label>
        <select id="add-category-select" v-model="addDialog.categoryCode" class="category-select">
          <option
            v-for="category in catalogState.categories"
            :key="category.code"
            :value="category.code"
          >
            {{ category.description }} ({{ category.code }})
          </option>
        </select>

        <input
          v-model="addDialog.search"
          class="item-search"
          type="text"
          placeholder="商品名 / ItemCode で検索"
        />

        <div class="item-list">
          <button
            v-for="item in filteredAddItems"
            :key="item.itemCode"
            class="item-option"
            type="button"
            @click="addButtonFromCatalog(item)"
          >
            <span class="item-option-name">{{ item.itemName }}</span>
            <span class="item-option-sub">
              #{{ item.itemCode }}
              <span v-if="item.unitPrice">{{ formatPrice(item.unitPrice) }}</span>
            </span>
          </button>
          <div v-if="!filteredAddItems.length" class="item-empty">該当商品がありません</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap { max-width: 1100px; margin: 24px auto; padding: 0 16px; font-family: system-ui, -apple-system, sans-serif; }
.panel { background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; margin: 12px 0; }
.tabs { display: flex; flex-wrap: wrap; gap: 8px; }
.tab { padding: 8px 10px; border-radius: 999px; border: 1px solid #ddd; background: #fafafa; cursor: pointer; }
.tab.active { border-color: #999; background: #f0f0f0; font-weight: 700; }
.grid { display: grid; gap: 8px; margin-top: 12px; }
.cell { background: #fafafa; border: 1px dashed #e3e3e3; border-radius: 10px; padding: 6px; display: flex; align-items: stretch; justify-content: stretch; position: relative; }
.empty { width: 100%; height: 100%; }
.cell-btn { width: 100%; border-radius: 10px; border: 1px solid #ddd; background: #fff; cursor: pointer; padding: 8px; text-align: left; }
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
  transition: opacity .12s ease, transform .12s ease;
  transform: scale(0.9);
}
.is-empty:hover .add-trigger {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}
.add-trigger:disabled { opacity: 0.3; pointer-events: none; }
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
  transition: opacity .12s ease, transform .12s ease;
  transform: scale(0.9);
  z-index: 2;
}
.has-button:hover .delete-trigger {
  opacity: 1;
  pointer-events: auto;
  transform: scale(1);
}
.delete-trigger:disabled { opacity: 0.3; pointer-events: none; }
.drag-source { outline: 2px solid #6c8cff; outline-offset: 1px; }
.drag-over { background: #eef3ff; border-color: #9cb1ff; }
.label { font-weight: 700; line-height: 1.2; }
.sub { opacity: .7; font-size: 12px; margin-top: 4px; display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.item-code { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.unit-price { font-variant-numeric: tabular-nums; white-space: nowrap; }

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
.add-modal-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.add-modal-head h2 { margin: 0; font-size: 18px; }
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
.add-target { font-size: 13px; color: #555; }
.field-label { font-size: 13px; font-weight: 600; }
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
.item-option:hover { border-color: #9ab7ff; background: #f5f8ff; }
.item-option-name { font-weight: 600; }
.item-option-sub {
  font-size: 12px;
  color: #666;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.item-empty { color: #666; font-size: 13px; padding: 8px 4px; }

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
