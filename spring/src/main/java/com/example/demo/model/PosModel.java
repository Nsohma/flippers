package com.example.demo.model;

import java.util.List;

public class PosModel {
    private List<String> sheets;

    public PosModel() {}

    public PosModel(List<String> sheets) {
        this.sheets = sheets;
    }

    public static PosModel forDebug(List<String> sheets) {
        return new PosModel(sheets);
    }

    public List<String> getSheets() { return sheets; }
    public void setSheets(List<String> sheets) { this.sheets = sheets; }
}
