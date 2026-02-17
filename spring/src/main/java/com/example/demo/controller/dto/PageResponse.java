package com.example.demo.controller.dto;

import java.util.List;

public class PageResponse {
    public int pageNumber;
    public int cols;
    public int rows;
    public List<ButtonDto> buttons;

    public static class ButtonDto {
        public int col;
        public int row;
        public String label;
        public int styleKey;
        public String itemCode;
    }
}
