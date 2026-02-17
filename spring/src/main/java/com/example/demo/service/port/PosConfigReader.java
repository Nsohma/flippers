package com.example.demo.service.port;

import com.example.demo.model.PosConfigSource;

import java.io.InputStream;

public interface PosConfigReader {
    PosConfigSource read(InputStream in) throws Exception;
}
