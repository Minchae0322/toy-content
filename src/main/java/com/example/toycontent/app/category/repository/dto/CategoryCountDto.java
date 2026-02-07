package com.example.toycontent.app.category.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCountDto {
  private Long categoryId;
  private Long contentCount;
}
