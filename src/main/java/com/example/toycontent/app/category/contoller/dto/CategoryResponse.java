package com.example.toycontent.app.category.contoller.dto;

import com.example.toycontent.app.category.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.Comment;

@Data
@Builder
public class CategoryResponse {

    @Schema(title = "카테고리 아이디")
    private Long categoryId;

    @Schema(title = "카테고리 명")
    private String categoryName;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .build();
    }
}
