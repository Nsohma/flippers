package com.example.demo.model;

import java.io.Serializable;
import java.util.Objects;

public class PosDraft implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String draftId;
    private final PosConfig config;
    private final byte[] originalExcelBytes;

    public PosDraft(String draftId, PosConfig config, byte[] originalExcelBytes) {
        this.draftId = Objects.requireNonNull(draftId);
        this.config = Objects.requireNonNull(config);
        this.originalExcelBytes = Objects.requireNonNull(originalExcelBytes).clone();
    }

    public String getDraftId() { return draftId; }
    public PosConfig getConfig() { return config; }
    public byte[] getOriginalExcelBytes() { return originalExcelBytes.clone(); }
}
