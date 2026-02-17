package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface DeleteButtonUseCase {
    PosConfig.Page deleteButton(String draftId, int pageNumber, String buttonId);
}
