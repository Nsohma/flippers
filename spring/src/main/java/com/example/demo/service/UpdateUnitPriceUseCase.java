package com.example.demo.service;

import com.example.demo.model.PosConfig;

public interface UpdateUnitPriceUseCase {
    PosConfig.Page updateUnitPrice(String draftId, int pageNumber, String buttonId, String unitPrice);
}
