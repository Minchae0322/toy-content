package com.example.toycontent.app.Product.controller.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class ProductSearchCondition {

  private Long categoryId;

  private List<String> searchTags;
}
