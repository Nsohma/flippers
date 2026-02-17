package com.example.demo.service.port;

import com.example.demo.model.PosConfig;

public interface PosConfigExporter {
    byte[] export(byte[] originalExcelBytes, PosConfig config) throws Exception;
}
