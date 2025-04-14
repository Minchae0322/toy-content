package com.example.toycontent.app.Product.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProductResponse {

    @Schema(title = "상품 명", example = "건담 로봇")
    private String name;
}
