package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface GetPageUseCase {
    PosConfig.Page getPage(String draftId, int pageNumber);
}
