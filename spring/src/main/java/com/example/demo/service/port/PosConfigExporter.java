package com.example.demo.service.port;

import com.example.demo.model.ItemCatalog;
import com.example.demo.model.PosConfig;

public interface PosConfigExporter {
    byte[] export(byte[] originalExcelBytes, PosConfig config) throws Exception;

    default byte[] export(byte[] originalExcelBytes, PosConfig config, ItemCatalog handyCatalog) throws Exception {
        return export(originalExcelBytes, config);
    }
}
