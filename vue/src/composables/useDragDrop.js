import { reactive } from "vue";

export function useDragDrop({ loadingRef, getSourceButton, onSwap }) {
  const dragState = reactive({
    sourceKey: "",
    overKey: "",
  });

  function resetDragState() {
    dragState.sourceKey = "";
    dragState.overKey = "";
  }

  function onButtonDragStart(cell, event) {
    if (!cell?.button || loadingRef.value) return;

    dragState.sourceKey = cell.key;
    dragState.overKey = "";

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", cell.key);
    }
  }

  function onCellDragOver(cell, event) {
    if (loadingRef.value) return;
    if (!dragState.sourceKey) return;
    if (!cell) return;
    if (cell.key === dragState.sourceKey) return;

    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = "move";
    }
    dragState.overKey = cell.key;
  }

  function onCellDragLeave(cell) {
    if (dragState.overKey === cell.key) {
      dragState.overKey = "";
    }
  }

  async function onCellDrop(cell, event) {
    if (loadingRef.value) return;

    const sourceKey = event.dataTransfer?.getData("text/plain") || dragState.sourceKey;
    if (!sourceKey) return;
    event.preventDefault();

    if (!cell || cell.key === sourceKey) {
      resetDragState();
      return;
    }

    const sourceButton = getSourceButton(sourceKey);
    if (!sourceButton) {
      resetDragState();
      return;
    }

    try {
      await onSwap({
        fromCol: sourceButton.col,
        fromRow: sourceButton.row,
        toCol: cell.col,
        toRow: cell.row,
      });
    } finally {
      resetDragState();
    }
  }

  function onButtonDragEnd() {
    resetDragState();
  }

  return {
    dragState,
    onButtonDragStart,
    onCellDragOver,
    onCellDragLeave,
    onCellDrop,
    onButtonDragEnd,
  };
}
