package com.example.toycontent.app.category.repository.querydsl.impl;

import com.example.toycontent.app.battle.domain.QBattle;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.ListView;
import com.example.toycontent.app.category.contoller.dto.CategorySearchCondition;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.domain.QCategory;
import com.example.toycontent.app.category.repository.dto.CategoryCountDto;
import com.example.toycontent.app.category.repository.querydsl.CategoryCustomRepository;
import com.example.toycontent.app.feed.domain.QFeed;
import com.example.toycontent.app.product.domain.QProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.toycontent.app.battle.domain.QBattle.battle;
import static com.example.toycontent.app.category.domain.QCategory.category;
import static com.example.toycontent.app.feed.domain.QFeed.feed;
import static com.example.toycontent.app.product.domain.QProduct.product;

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
        BooleanBuilder conditionBuilder = createSearchCondition(condition);

        return queryFactory
            .selectFrom(category)
            .where(conditionBuilder,
                category.depth.eq(0))
            .orderBy(getDefaultOrderSpecifiers())
            .fetch();
    }

   /**
    * 배틀(Battle) 기준 인기 카테고리 조회
    *
         * 지정된 기간 내 배틀 수가 많은 카테고리를 내림차순으로 페이징 조회한다.
        * 하위 카테고리는 상위 카테고리로 그룹핑되며, 상위가 없으면 자기 자신이 그룹 키가 된다.
     *
         * @param pageable  페이징 정보 (offset, size)
     * @param condition 조회 조건 (기간, 활성 상태 등)
     * @return 카테고리별 배틀 수 DTO 페이지
     */
    @Override
    public Page<CategoryCountDto> findPopularByBattle(Pageable pageable,
        CategorySearchCondition.PopularSearch condition) {

        BooleanBuilder dateWhere = createDateCondition(battle.createdAt, condition);
        BooleanBuilder where = createSearchCondition(condition);

        // 하위 카테고리 → 상위 카테고리로 그룹핑, 상위가 없으면(=부모 카테고리) 자기 자신 ID 사용
        NumberExpression<Long> groupKey = category.parent.id.coalesce(category.id);

        List<CategoryCountDto> content = queryFactory
            .select(Projections.constructor(CategoryCountDto.class,
                groupKey,
                battle.id.count()
            ))
            .from(battle)
            .innerJoin(category).on(category.id.eq(battle.category.id))
            .where(dateWhere, where)
            .groupBy(groupKey)
            .orderBy(battle.id.count().desc()) // 배틀 수 내림차순 정렬
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 전체 그룹 수 조회 (페이징 totalCount용)
        Long total = queryFactory
            .select(groupKey.countDistinct())
            .from(battle)
            .innerJoin(category).on(category.id.eq(battle.category.id))
            .where(dateWhere, where)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 피드(Feed) 기준 인기 카테고리 조회
     *
     * 지정된 기간 내 피드 수가 많은 카테고리를 내림차순으로 페이징 조회한다.
     * 그룹핑 로직은 {@link #findPopularByBattle}과 동일하다.
     *
     * @param pageable  페이징 정보
     * @param condition 조회 조건
     * @return 카테고리별 피드 수 DTO 페이지
     */
    @Override
    public Page<CategoryCountDto> findPopularByFeed(Pageable pageable,
        CategorySearchCondition.PopularSearch condition) {

        BooleanBuilder dateWhere = createDateCondition(feed.createdAt, condition);
        BooleanBuilder where = createSearchCondition(condition);

        NumberExpression<Long> groupKey = category.parent.id.coalesce(category.id);

        List<CategoryCountDto> content = queryFactory
            .select(Projections.constructor(CategoryCountDto.class,
                groupKey,
                feed.id.count()
            ))
            .from(feed)
            .innerJoin(category).on(category.id.eq(feed.category.id))
            .where(dateWhere, where)
            .groupBy(groupKey)
            .orderBy(feed.id.count().desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(groupKey.countDistinct())
            .from(feed)
            .innerJoin(category).on(category.id.eq(feed.category.id))
            .where(dateWhere, where)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 상품(Product) 기준 인기 카테고리 조회
     *
     * 지정된 기간 내 상품 수가 많은 카테고리를 내림차순으로 페이징 조회한다.
     * 그룹핑 로직은 {@link #findPopularByBattle}과 동일하다.
     *
     * @param pageable  페이징 정보
     * @param condition 조회 조건
     * @return 카테고리별 상품 수 DTO 페이지
     */
    @Override
    public Page<CategoryCountDto> findPopularByProduct(Pageable pageable,
        CategorySearchCondition.PopularSearch condition) {

        BooleanBuilder dateWhere = createDateCondition(product.createdAt, condition);
        BooleanBuilder where = createSearchCondition(condition);

        NumberExpression<Long> groupKey = category.parent.id.coalesce(category.id);

        List<CategoryCountDto> content = queryFactory
            .select(Projections.constructor(CategoryCountDto.class,
                groupKey,
                product.id.count()
            ))
            .from(product)
            .innerJoin(category).on(category.id.eq(product.category.id))
            .where(dateWhere, where)
            .groupBy(groupKey)
            .orderBy(product.id.count().desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(groupKey.countDistinct())
            .from(product)
            .innerJoin(category).on(category.id.eq(product.category.id))
            .where(dateWhere, where)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 날짜 범위 조건 생성
     *
     * condition의 startDate ~ endDate 범위로 createdAt 필터를 구성한다.
     * - startDate: 해당 일 00:00:00 이상 (inclusive)
     * - endDate:   다음 일 00:00:00 미만 (exclusive) → endDate 당일까지 포함
     *
     * @param createdAt 대상 엔티티의 createdAt 경로
     * @param condition 시작일/종료일을 포함한 조회 조건
     * @return 날짜 범위 BooleanBuilder
     */
    private BooleanBuilder createDateCondition(DateTimePath<LocalDateTime> createdAt,
        CategorySearchCondition.PopularSearch condition) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(createdAt.goe(condition.getStartDate().atStartOfDay()));
        builder.and(createdAt.lt(condition.getEndDate().plusDays(1).atStartOfDay()));
        return builder;
    }

    /**
     * 공통 검색 조건 생성
     *
     * 카테고리 활성 상태(isActive) 필터를 조건에 추가한다.
     * condition이 null이거나 isActive가 null이면 해당 조건은 생략된다.
     *
     * @param condition 검색 조건 (isActive 등)
     * @return 검색 조건 BooleanBuilder
     */
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
