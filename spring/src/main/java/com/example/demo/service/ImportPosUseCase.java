package com.example.demo.service;

import com.example.demo.model.PosDraft;
import com.example.demo.service.command.ImportPosCommand;

public interface ImportPosUseCase {
    PosDraft importExcel(ImportPosCommand command);
}
