package com.example.demo.service.command;

public record ImportPosCommand(byte[] excelBytes) {
    public ImportPosCommand {
        if (excelBytes == null || excelBytes.length == 0) {
            throw new IllegalArgumentException("excelBytes is empty");
        }
        excelBytes = excelBytes.clone();
    }

    @Override
    public byte[] excelBytes() {
        return excelBytes.clone();
    }
}
