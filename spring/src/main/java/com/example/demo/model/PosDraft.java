package com.example.demo.model;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PosDraft implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int HISTORY_LIMIT = 100;

    private final String draftId;
    private final PosConfig config;
    private final byte[] originalExcelBytes;
    private final ItemCatalog itemCatalog;
    private final List<PosConfig> history;
    private final List<HistoryEntry> historyEntries;
    private final int historyIndex;

    public PosDraft(String draftId, PosConfig config, byte[] originalExcelBytes) {
        this(draftId, config, originalExcelBytes, null, "インポート");
    }

    public PosDraft(String draftId, PosConfig config, byte[] originalExcelBytes, ItemCatalog itemCatalog) {
        this(draftId, config, originalExcelBytes, itemCatalog, "インポート");
    }

    public PosDraft(
            String draftId,
            PosConfig config,
            byte[] originalExcelBytes,
            ItemCatalog itemCatalog,
            String initialAction
    ) {
        this(draftId, config, originalExcelBytes, itemCatalog, null, null, -1, initialAction);
    }

    private PosDraft(
            String draftId,
            PosConfig config,
            byte[] originalExcelBytes,
            ItemCatalog itemCatalog,
            List<PosConfig> history,
            List<HistoryEntry> historyEntries,
            int historyIndex,
            String fallbackAction
    ) {
        this.draftId = Objects.requireNonNull(draftId);
        this.originalExcelBytes = Objects.requireNonNull(originalExcelBytes).clone();
        this.itemCatalog = itemCatalog;
        Objects.requireNonNull(config);

        List<PosConfig> normalizedHistory = normalizeHistory(config, history);
        List<HistoryEntry> normalizedHistoryEntries = normalizeHistoryEntries(
                normalizedHistory,
                historyEntries,
                fallbackAction
        );
        int normalizedIndex = normalizeHistoryIndex(historyIndex, normalizedHistory.size());

        this.history = normalizedHistory;
        this.historyEntries = normalizedHistoryEntries;
        this.historyIndex = normalizedIndex;
        this.config = normalizedHistory.get(normalizedIndex);
    }

    public String getDraftId() { return draftId; }
    public PosConfig getConfig() { return config; }
    public byte[] getOriginalExcelBytes() { return originalExcelBytes.clone(); }
    public ItemCatalog getItemCatalogOrNull() { return itemCatalog; }
    public List<HistoryEntry> getHistoryEntries() { return historyEntries; }
    public int getHistoryIndex() { return historyIndex; }

    public boolean canUndo() {
        return historyIndex > 0;
    }

    public boolean canRedo() {
        return historyIndex + 1 < history.size();
    }

    public PosDraft applyNewConfig(PosConfig nextConfig) {
        return applyNewConfig(nextConfig, "編集");
    }

    public PosDraft applyNewConfig(PosConfig nextConfig, String action) {
        Objects.requireNonNull(nextConfig);

        List<PosConfig> nextHistory = new ArrayList<>(history.subList(0, historyIndex + 1));
        List<HistoryEntry> nextHistoryEntries = new ArrayList<>(historyEntries.subList(0, historyIndex + 1));
        nextHistory.add(nextConfig);
        nextHistoryEntries.add(HistoryEntry.of(action));

        if (nextHistory.size() > HISTORY_LIMIT) {
            int removeCount = nextHistory.size() - HISTORY_LIMIT;
            nextHistory = new ArrayList<>(nextHistory.subList(removeCount, nextHistory.size()));
            nextHistoryEntries = new ArrayList<>(nextHistoryEntries.subList(removeCount, nextHistoryEntries.size()));
        }

        int nextIndex = nextHistory.size() - 1;
        return new PosDraft(
                draftId,
                nextConfig,
                originalExcelBytes,
                itemCatalog,
                nextHistory,
                nextHistoryEntries,
                nextIndex,
                action
        );
    }

    public PosDraft undo() {
        if (!canUndo()) {
            throw new IllegalArgumentException("undo not available");
        }
        int nextIndex = historyIndex - 1;
        PosConfig nextConfig = history.get(nextIndex);
        return new PosDraft(
                draftId,
                nextConfig,
                originalExcelBytes,
                itemCatalog,
                history,
                historyEntries,
                nextIndex,
                null
        );
    }

    public PosDraft redo() {
        if (!canRedo()) {
            throw new IllegalArgumentException("redo not available");
        }
        int nextIndex = historyIndex + 1;
        PosConfig nextConfig = history.get(nextIndex);
        return new PosDraft(
                draftId,
                nextConfig,
                originalExcelBytes,
                itemCatalog,
                history,
                historyEntries,
                nextIndex,
                null
        );
    }

    public PosDraft jumpToHistoryIndex(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= history.size()) {
            throw new IllegalArgumentException("history index out of range: " + targetIndex);
        }
        if (targetIndex == historyIndex) {
            return this;
        }
        PosConfig targetConfig = history.get(targetIndex);
        return new PosDraft(
                draftId,
                targetConfig,
                originalExcelBytes,
                itemCatalog,
                history,
                historyEntries,
                targetIndex,
                null
        );
    }

    public PosDraft withItemCatalog(ItemCatalog nextItemCatalog) {
        if (nextItemCatalog == itemCatalog) {
            return this;
        }
        return new PosDraft(
                draftId,
                config,
                originalExcelBytes,
                nextItemCatalog,
                history,
                historyEntries,
                historyIndex,
                null
        );
    }

    private Object readResolve() throws ObjectStreamException {
        List<PosConfig> normalizedHistory = normalizeHistory(config, history);
        List<HistoryEntry> normalizedHistoryEntries = normalizeHistoryEntries(
                normalizedHistory,
                historyEntries,
                "復元"
        );
        int normalizedIndex = normalizeHistoryIndex(historyIndex, normalizedHistory.size());
        if (normalizedHistory == history
                && normalizedHistoryEntries == historyEntries
                && normalizedIndex == historyIndex
                && config == normalizedHistory.get(normalizedIndex)) {
            return this;
        }
        return new PosDraft(
                draftId,
                normalizedHistory.get(normalizedIndex),
                originalExcelBytes,
                itemCatalog,
                normalizedHistory,
                normalizedHistoryEntries,
                normalizedIndex,
                null
        );
    }

    private static List<PosConfig> normalizeHistory(PosConfig config, List<PosConfig> history) {
        if (history == null || history.isEmpty()) {
            return List.of(config);
        }
        return List.copyOf(history);
    }

    private static List<HistoryEntry> normalizeHistoryEntries(
            List<PosConfig> normalizedHistory,
            List<HistoryEntry> historyEntries,
            String fallbackAction
    ) {
        if (historyEntries == null || historyEntries.isEmpty()) {
            return fillFallbackHistoryEntries(normalizedHistory.size(), fallbackAction);
        }
        if (historyEntries.size() == normalizedHistory.size()) {
            return List.copyOf(historyEntries);
        }
        int targetSize = normalizedHistory.size();
        List<HistoryEntry> adjusted = new ArrayList<>(targetSize);
        int offset = Math.max(0, historyEntries.size() - targetSize);
        for (int i = 0; i < targetSize; i++) {
            int sourceIndex = i + offset;
            if (sourceIndex >= 0 && sourceIndex < historyEntries.size()) {
                adjusted.add(historyEntries.get(sourceIndex));
            } else {
                adjusted.add(HistoryEntry.of(fallbackAction));
            }
        }
        return List.copyOf(adjusted);
    }

    private static List<HistoryEntry> fillFallbackHistoryEntries(int size, String fallbackAction) {
        int targetSize = Math.max(1, size);
        List<HistoryEntry> entries = new ArrayList<>(targetSize);
        for (int i = 0; i < targetSize; i++) {
            entries.add(HistoryEntry.of(fallbackAction));
        }
        return List.copyOf(entries);
    }

    private static int normalizeHistoryIndex(int historyIndex, int historySize) {
        if (historySize <= 0) {
            return 0;
        }
        if (historyIndex < 0 || historyIndex >= historySize) {
            return historySize - 1;
        }
        return historyIndex;
    }

    public static class HistoryEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String action;
        private final String timestamp;

        public HistoryEntry(String action, String timestamp) {
            this.action = action == null || action.isBlank() ? "編集" : action;
            this.timestamp = timestamp == null ? "" : timestamp;
        }

        public String getAction() {
            return action;
        }

        public String getTimestamp() {
            return timestamp;
        }

        static HistoryEntry of(String action) {
            return new HistoryEntry(action, OffsetDateTime.now().toString());
        }
    }
}
