package com.example.demo.controller.dto;

import java.util.List;

public class DraftHistoryResponse {
    public List<EntryDto> entries;
    public int currentIndex;
    public boolean canUndo;
    public boolean canRedo;

    public static class EntryDto {
        public int index;
        public String action;
        public String timestamp;
    }
}
