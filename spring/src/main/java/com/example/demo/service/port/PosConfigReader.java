package com.example.demo.service.port;

import com.example.demo.model.PosConfig;

import java.io.InputStream;

public interface PosConfigReader {
    PosConfig read(InputStream in) throws Exception;
}
