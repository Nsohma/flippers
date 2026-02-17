package com.example.demo.model;

public class PosDraft {
    private final String draftId;
    private final PosConfig config;

    public PosDraft(String draftId, PosConfig config) {
        this.draftId = draftId;
        this.config = config;
    }

    public String getDraftId() { return draftId; }
    public PosConfig getConfig() { return config; }
}
