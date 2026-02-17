package com.example.demo.service;

import com.example.demo.model.PosDraft;

import java.io.InputStream;

public interface ImportPosUseCase {
    PosDraft importExcel(InputStream in) throws Exception;
}
