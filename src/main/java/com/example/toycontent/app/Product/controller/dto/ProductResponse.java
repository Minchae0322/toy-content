package com.example.toycontent.app.Product.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


public abstract class ProductResponse {

    @Data
    public static class ProductList {
        @Schema(title = "상품 명", example = "건담 로봇")
        private String name;
    }


}
