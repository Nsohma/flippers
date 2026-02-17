package com.example.demo.controller.dto;

public record ErrorResponse(
        String error,
        String message,
        String path,
        String timestamp
) {
}
