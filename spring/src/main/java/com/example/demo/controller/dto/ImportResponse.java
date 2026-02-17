package com.example.demo.controller.dto;

import java.util.List;

public class ImportResponse {
    public String draftId;
    public List<CategoryDto> categories;
    public PageDto initialPage;

    public static class CategoryDto {
        public int pageNumber;
        public int cols;
        public int rows;
        public String name;
        public int styleKey;
    }

    public static class PageDto {
        public int pageNumber;
        public int cols;
        public int rows;
        public List<ButtonDto> buttons;
    }

    public static class ButtonDto {
        public int col;
        public int row;
        public String label;
        public int styleKey;
        public String itemCode;
    }
}
