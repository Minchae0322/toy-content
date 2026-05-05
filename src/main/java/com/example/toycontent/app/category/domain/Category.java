package com.example.toycontent.app.category.domain;


import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.category.contoller.dto.CategoryRequest;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.CategoryType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import jakarta.persistence.*;
import java.util.ArrayList;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@Builder
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false)
    private Long id;

    @Column(nullable = false, length = 100)
    @Comment("카테고리 명")
    private String name;

    @Column(length = 500)
    @Comment("카테고리 설명")
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Comment("정렬 순서")
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Comment("활성화 여부")
    private Boolean isActive = true;

    @Column(name = "keywords", length = 1000)
    @Comment("키워드 (콤마로 구분)")
    private String keywords;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Comment("부모 카테고리")
    private Category parent;

    // 자식 카테고리들
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(name = "depth", nullable = false)
    @Comment("카테고리 depth (0: 최상위)")
    @Builder.Default
    private Integer depth = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @ColumnDefault("'PRODUCT'")
    @Comment("카테고리 타입 (PRODUCT: 상품/배틀 공용, FEED: 피드 전용)")
    @Builder.Default
    private CategoryType type = CategoryType.PRODUCT;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;

    public void update(CategoryRequest.Update updateDto) {
        if (StringUtils.hasText(updateDto.getCategoryName())) {
            this.name = updateDto.getCategoryName();
        }
        if (StringUtils.hasText(updateDto.getDescription())) {
            this.description = updateDto.getDescription();
        }
        if (updateDto.getKeywords() != null) {
            this.keywords = convertKeywordsToString(updateDto.getKeywords());
        }
    }

    public static String convertKeywordsToString(List<String> keywordList) {
        if (keywordList == null || keywordList.isEmpty()) {
            return null;
        }
        return keywordList.stream()
                .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    public void toggleStatus() {
        this.isActive = !this.isActive;
    }

    public void setSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder < 1) {
            throw new RestApiException(CategoryErrorCode.INVALID_SORT_ORDER);
        }
        this.sortOrder = sortOrder;
    }

    public void setParent(Category parent) {
        this.parent = parent;
        this.depth = parent.getDepth() + 1;
    }


}

