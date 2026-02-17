import { computed, reactive } from "vue";

function normalizeApiBase(rawApiBase) {
  const value = String(rawApiBase ?? "").trim();
  return (value || "/api/pos").replace(/\/+$/, "");
}

export function usePosDraft(rawApiBase) {
  const API_BASE = normalizeApiBase(rawApiBase);

  const state = reactive({
    draftId: "",
    categories: [],
    page: null,
    selectedPageNumber: null,
    historyEntries: [],
    historyIndex: -1,
    canUndo: false,
    canRedo: false,
    loading: false,
    error: "",
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

  const priceDialog = reactive({
    open: false,
    buttonId: "",
    label: "",
    itemCode: "",
    unitPrice: "",
  });

  const categoryDialog = reactive({
    open: false,
    name: "",
    cols: "5",
    rows: "5",
    styleKey: "1",
  });

  const gridDialog = reactive({
    open: false,
    cols: "5",
    rows: "5",
  });

  const buttonMap = computed(() => {
    const map = new Map();
    if (!state.page?.buttons) return map;
    for (const button of state.page.buttons) {
      map.set(`${button.col}-${button.row}`, button);
    }
    return map;
  });

  const gridCells = computed(() => {
    if (!state.page) return [];
    const cells = [];
    for (let row = 1; row <= state.page.rows; row++) {
      for (let col = 1; col <= state.page.cols; col++) {
        const key = `${col}-${row}`;
        cells.push({ col, row, key, button: buttonMap.value.get(key) ?? null });
      }
    }
    return cells;
  });

  const selectedAddCategory = computed(() => {
    if (!addDialog.categoryCode) return null;
    return catalogState.categories.find((category) => category.code === addDialog.categoryCode) ?? null;
  });

  const selectedCategory = computed(() => {
    if (!state.selectedPageNumber) return null;
    return state.categories.find((category) => category.pageNumber === state.selectedPageNumber) ?? null;
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

  function resetCatalogState() {
    catalogState.loaded = false;
    catalogState.loading = false;
    catalogState.categories = [];
  }

  function closeAddDialog() {
    addDialog.open = false;
    addDialog.targetCol = 0;
    addDialog.targetRow = 0;
    addDialog.categoryCode = "";
    addDialog.search = "";
  }

  function closePriceDialog() {
    priceDialog.open = false;
    priceDialog.buttonId = "";
    priceDialog.label = "";
    priceDialog.itemCode = "";
    priceDialog.unitPrice = "";
  }

  function closeCategoryDialog() {
    categoryDialog.open = false;
    categoryDialog.name = "";
  }

  function closeGridDialog() {
    gridDialog.open = false;
  }

  function closeAllDialogs() {
    closeAddDialog();
    closePriceDialog();
    closeCategoryDialog();
    closeGridDialog();
  }

  function markHistoryMutated() {
    state.canUndo = true;
    state.canRedo = false;
  }

  function applyHistoryFlags(payload, fallbackCanUndo = state.canUndo, fallbackCanRedo = state.canRedo) {
    state.canUndo = typeof payload?.canUndo === "boolean" ? payload.canUndo : fallbackCanUndo;
    state.canRedo = typeof payload?.canRedo === "boolean" ? payload.canRedo : fallbackCanRedo;
  }

  function applyCategoryState(payload) {
    state.categories = payload?.categories ?? [];
    state.page = payload?.page ?? null;
    state.selectedPageNumber = state.page?.pageNumber ?? (state.categories[0]?.pageNumber ?? null);
  }

  function applyHistoryState(payload) {
    state.historyEntries = Array.isArray(payload?.entries) ? payload.entries : [];
    state.historyIndex = Number.isInteger(payload?.currentIndex) ? payload.currentIndex : -1;
    state.canUndo = Boolean(payload?.canUndo);
    state.canRedo = Boolean(payload?.canRedo);
  }

  async function loadHistory(silent = false) {
    if (!state.draftId) return;

    if (!silent) {
      state.error = "";
      state.loading = true;
    }

    try {
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/history`);
      if (!res.ok) {
        throw new Error(await readApiError(res, `Get history failed: ${res.status}`));
      }
      const data = await res.json();
      applyHistoryState(data);
    } catch (error) {
      if (!silent) {
        state.error = String(error);
      }
    } finally {
      if (!silent) {
        state.loading = false;
      }
    }
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
    } catch (error) {
      catalogState.loaded = false;
      state.error = String(error);
    } finally {
      catalogState.loading = false;
    }
  }

  async function ensureItemCatalogLoaded() {
    if (catalogState.loaded && catalogState.categories.length > 0) return true;
    await loadItemCatalog();
    return catalogState.loaded && catalogState.categories.length > 0;
  }

  async function importExcel(file) {
    state.error = "";
    if (!file) {
      state.error = "Excelファイルを選択してください";
      return;
    }

    const form = new FormData();
    form.append("file", file);

    state.loading = true;
    try {
      const res = await fetch(`${API_BASE}/import`, { method: "POST", body: form });
      if (!res.ok) throw new Error(`Import failed: ${res.status}`);

      const data = await res.json();
      state.draftId = data.draftId;
      state.categories = data.categories ?? [];
      state.page = data.initialPage ?? null;
      state.selectedPageNumber = state.page?.pageNumber ?? (state.categories[0]?.pageNumber ?? null);
      state.historyEntries = [];
      state.historyIndex = -1;
      state.canUndo = Boolean(data?.canUndo);
      state.canRedo = Boolean(data?.canRedo);
      closeAllDialogs();
      resetCatalogState();
      await loadHistory(true);
      void loadItemCatalog();
    } catch (error) {
      state.error = String(error);
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
      state.page = await res.json();
      state.selectedPageNumber = pageNumber;
      closeAllDialogs();
    } catch (error) {
      state.error = String(error);
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
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

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

  function openCategoryDialog() {
    if (state.loading || !state.draftId) return;

    closeAddDialog();
    closePriceDialog();
    closeGridDialog();

    const base = selectedCategory.value ?? state.categories[0] ?? null;
    categoryDialog.name = "";
    categoryDialog.cols = String(base?.cols ?? 5);
    categoryDialog.rows = String(base?.rows ?? 5);
    categoryDialog.styleKey = String(base?.styleKey ?? 1);
    categoryDialog.open = true;
  }

  async function submitAddCategory() {
    if (state.loading || !state.draftId) return;
    if (!categoryDialog.open) return;

    const name = String(categoryDialog.name ?? "").trim();
    if (!name) {
      state.error = "カテゴリ名を入力してください";
      return;
    }

    const cols = Number.parseInt(String(categoryDialog.cols ?? "").trim(), 10);
    const rows = Number.parseInt(String(categoryDialog.rows ?? "").trim(), 10);
    const styleKey = Number.parseInt(String(categoryDialog.styleKey ?? "").trim(), 10);

    if (!Number.isInteger(cols) || cols <= 0) {
      state.error = "列数は1以上の整数で入力してください";
      return;
    }
    if (!Number.isInteger(rows) || rows <= 0) {
      state.error = "行数は1以上の整数で入力してください";
      return;
    }
    if (!Number.isInteger(styleKey) || styleKey <= 0) {
      state.error = "styleKeyは1以上の整数で入力してください";
      return;
    }

    state.error = "";
    state.loading = true;
    try {
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/categories`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, cols, rows, styleKey }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Add category failed: ${res.status}`));
      }

      const data = await res.json();
      applyCategoryState(data);
      applyHistoryFlags(data, true, false);
      closeCategoryDialog();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  async function deleteSelectedCategory() {
    if (state.loading || !state.draftId) return;
    const pageNumber = state.selectedPageNumber;
    if (!pageNumber) return;

    state.error = "";
    state.loading = true;
    try {
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/categories/${pageNumber}`, {
        method: "DELETE",
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Delete category failed: ${res.status}`));
      }

      const data = await res.json();
      applyCategoryState(data);
      applyHistoryFlags(data, true, false);
      closeAllDialogs();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  function openGridDialog() {
    if (state.loading || !state.draftId) return;
    if (!state.selectedPageNumber) return;

    closeAddDialog();
    closePriceDialog();
    closeCategoryDialog();
    state.error = "";

    const base = state.page ?? selectedCategory.value ?? null;
    gridDialog.cols = String(base?.cols ?? 5);
    gridDialog.rows = String(base?.rows ?? 5);
    gridDialog.open = true;
  }

  async function submitGridUpdate() {
    if (state.loading || !state.draftId || !gridDialog.open) return;
    const pageNumber = state.selectedPageNumber;
    if (!pageNumber) return;

    const cols = Number.parseInt(String(gridDialog.cols ?? "").trim(), 10);
    const rows = Number.parseInt(String(gridDialog.rows ?? "").trim(), 10);
    if (!Number.isInteger(cols) || cols <= 0) {
      state.error = "列数は1以上の整数で入力してください";
      return;
    }
    if (!Number.isInteger(rows) || rows <= 0) {
      state.error = "行数は1以上の整数で入力してください";
      return;
    }

    state.error = "";
    state.loading = true;
    try {
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/categories/${pageNumber}/grid`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ cols, rows }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Update grid failed: ${res.status}`));
      }

      const data = await res.json();
      applyCategoryState(data);
      applyHistoryFlags(data, true, false);
      closeGridDialog();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  function openPriceDialog(button) {
    if (state.loading) return;
    if (!button?.buttonId) return;

    closeAddDialog();
    closeCategoryDialog();
    closeGridDialog();
    state.error = "";
    priceDialog.buttonId = button.buttonId;
    priceDialog.label = button.label ?? "";
    priceDialog.itemCode = button.itemCode ?? "";
    priceDialog.unitPrice = String(button.unitPrice ?? "").trim();
    priceDialog.open = true;
  }

  async function submitUnitPriceUpdate() {
    if (state.loading) return;
    if (!priceDialog.open || !state.page || !state.draftId) return;

    const normalized = String(priceDialog.unitPrice ?? "").trim().replace(/,/g, "");
    if (!normalized) {
      state.error = "価格を入力してください";
      return;
    }
    if (!/^\d+(\.\d+)?$/.test(normalized)) {
      state.error = "価格は数字で入力してください";
      return;
    }

    state.error = "";
    state.loading = true;
    try {
      const res = await fetch(
        `${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons/unit-price`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            buttonId: priceDialog.buttonId,
            unitPrice: normalized,
          }),
        },
      );
      if (!res.ok) {
        throw new Error(await readApiError(res, `Update unit price failed: ${res.status}`));
      }

      state.page = await res.json();
      closePriceDialog();
      markHistoryMutated();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  async function openAddDialog(cell) {
    if (state.loading) return;
    if (!cell || cell.button) return;
    if (!state.draftId || !state.page) return;

    closePriceDialog();
    closeCategoryDialog();
    closeGridDialog();
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
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          col: addDialog.targetCol,
          row: addDialog.targetRow,
          categoryCode: addDialog.categoryCode,
          itemCode: item.itemCode,
        }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Add button failed: ${res.status}`));
      }

      state.page = await res.json();
      closeAddDialog();
      markHistoryMutated();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
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
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ buttonId: button.buttonId }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Delete button failed: ${res.status}`));
      }

      state.page = await res.json();
      if (priceDialog.open && priceDialog.buttonId === button.buttonId) {
        closePriceDialog();
      }
      markHistoryMutated();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  async function swapButtons(fromCol, fromRow, toCol, toRow) {
    if (state.loading) return;
    if (!state.page || !state.draftId) return;

    state.error = "";
    state.loading = true;
    try {
      const res = await fetch(
        `${API_BASE}/drafts/${state.draftId}/pages/${state.page.pageNumber}/buttons/swap`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ fromCol, fromRow, toCol, toRow }),
        },
      );
      if (!res.ok) {
        throw new Error(await readApiError(res, `Swap failed: ${res.status}`));
      }

      state.page = await res.json();
      markHistoryMutated();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  async function undo() {
    if (state.loading || !state.draftId || !state.canUndo) return;

    state.error = "";
    state.loading = true;
    try {
      const query = Number.isInteger(state.selectedPageNumber)
        ? `?selectedPageNumber=${state.selectedPageNumber}`
        : "";
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/undo${query}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Undo failed: ${res.status}`));
      }

      const data = await res.json();
      applyCategoryState(data);
      applyHistoryFlags(data, false, false);
      closeAllDialogs();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  async function redo() {
    if (state.loading || !state.draftId || !state.canRedo) return;

    state.error = "";
    state.loading = true;
    try {
      const query = Number.isInteger(state.selectedPageNumber)
        ? `?selectedPageNumber=${state.selectedPageNumber}`
        : "";
      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/redo${query}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Redo failed: ${res.status}`));
      }

      const data = await res.json();
      applyCategoryState(data);
      applyHistoryFlags(data, false, false);
      closeAllDialogs();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  async function jumpToHistory(index) {
    if (state.loading || !state.draftId) return;
    if (!Number.isInteger(index)) return;
    if (index === state.historyIndex) return;

    state.error = "";
    state.loading = true;
    try {
      const query = new URLSearchParams({ index: String(index) });
      if (Number.isInteger(state.selectedPageNumber)) {
        query.set("selectedPageNumber", String(state.selectedPageNumber));
      }

      const res = await fetch(`${API_BASE}/drafts/${state.draftId}/history/jump?${query.toString()}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `Jump history failed: ${res.status}`));
      }

      const data = await res.json();
      applyCategoryState(data);
      applyHistoryFlags(data, false, false);
      closeAllDialogs();
      await loadHistory(true);
    } catch (error) {
      state.error = String(error);
    } finally {
      state.loading = false;
    }
  }

  return {
    state,
    catalogState,
    addDialog,
    priceDialog,
    categoryDialog,
    gridDialog,
    buttonMap,
    gridCells,
    filteredAddItems,
    importExcel,
    loadPage,
    exportExcel,
    buttonClass,
    formatPrice,
    closeAddDialog,
    closePriceDialog,
    closeCategoryDialog,
    closeGridDialog,
    openCategoryDialog,
    submitAddCategory,
    deleteSelectedCategory,
    openGridDialog,
    submitGridUpdate,
    openPriceDialog,
    submitUnitPriceUpdate,
    openAddDialog,
    addButtonFromCatalog,
    deleteButton,
    swapButtons,
    undo,
    redo,
    jumpToHistory,
  };
}
