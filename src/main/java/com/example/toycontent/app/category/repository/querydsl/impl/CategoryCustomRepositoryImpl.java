package com.example.toycontent.app.category.repository.querydsl.impl;

import com.example.toycontent.app.category.contoller.dto.CategorySearchCondition;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.querydsl.CategoryCustomRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.toycontent.app.category.domain.QCategory.category;

@RequiredArgsConstructor
@Repository
public class CategoryCustomRepositoryImpl implements CategoryCustomRepository {

    private final JPAQueryFactory queryFactory;
    @Override
    public List<Category> findCategoriesWithSearchCondition(Pageable pageable, CategorySearchCondition condition) {
        BooleanBuilder builder = createSearchCondition(condition);

        return queryFactory
                .selectFrom(category)
                .where(builder)
                .orderBy(getOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public Long countCategoriesWithSearchCondition(CategorySearchCondition condition) {
        BooleanBuilder builder = createSearchCondition(condition);

        return queryFactory
                .select(category.count())
                .from(category)
                .where(builder)
                .fetchOne();
    }

    @Override
    public List<Category> findCategoriesWithSearchCondition(CategorySearchCondition condition) {
        BooleanBuilder builder = createSearchCondition(condition);

        return queryFactory
                .selectFrom(category)
                .where(builder)
                .orderBy(getDefaultOrderSpecifiers())
                .fetch();
    }



    private BooleanBuilder createSearchCondition(CategorySearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition == null) {
            return builder;
        }

        Optional.ofNullable(condition.getIsActive())
                .ifPresent(status -> builder.and(category.isActive.eq(status)));

        return builder;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        if (sort.isEmpty()) {
            return getDefaultOrderSpecifiers();
        }

        return sort.stream()
                .map(order -> {
                    return switch (order.getProperty()) {
                        case "categoryName" -> order.isAscending() ? category.name.asc() : category.name.desc();
                        case "sortOrder" -> order.isAscending() ? category.sortOrder.asc() : category.sortOrder.desc();
                        case "createdAt" -> order.isAscending() ? category.createdAt.asc() : category.createdAt.desc();
                        case "updatedAt" -> order.isAscending() ? category.updatedAt.asc() : category.updatedAt.desc();
                        default -> order.isAscending() ? category.id.asc() : category.id.desc();
                    };
                })
                .toArray(OrderSpecifier[]::new);
    }

    private OrderSpecifier<?>[] getDefaultOrderSpecifiers() {
        return new OrderSpecifier[]{
                category.sortOrder.asc(),
                category.name.asc()
        };
    }
}
