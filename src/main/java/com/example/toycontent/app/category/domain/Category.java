package com.example.toycontent.app.category.domain;


import ch.qos.logback.core.util.StringUtil;
import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.category.contoller.dto.CategoryRequest;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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


}

