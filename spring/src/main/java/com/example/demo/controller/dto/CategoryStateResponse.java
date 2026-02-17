package com.example.demo.controller.dto;

import java.util.List;

public class CategoryStateResponse {
    public List<CategoryDto> categories;
    public PageResponse page;

    public static class CategoryDto {
        public int pageNumber;
        public int cols;
        public int rows;
        public String name;
        public int styleKey;
    }
}
