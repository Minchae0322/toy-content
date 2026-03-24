package com.example.toycontent.app.product.repository.querydsl.impl;

import static com.example.toycontent.app.product.domain.QProduct.product;
import static com.example.toycontent.app.product.domain.QProductAttachmentFile.productAttachmentFile;
import static com.example.toycontent.app.product.domain.QProductReview.productReview;
import static com.example.toycontent.app.category.domain.QCategory.category;

import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.product.repository.querydsl.ProductRepositoryCustom;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final JdbcTemplate jdbcTemplate;  // 추가


  @Override
  public List<ProductList> findBySearchCondition(ProductSearchCondition searchCondition,
      Pageable pageable) {

    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(searchCondition);

    return selectProductList()
        .where(whereClause)
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  @Override
  public Long countBySearchCondition(ProductSearchCondition searchCondition) {
    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(searchCondition);
    return countProducts(whereClause);
  }

  @Override
  public List<ProductList> findByUserIdAndSearchCondition(Long userId,
      ProductSearchCondition condition, Pageable pageable) {

    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(condition);
    whereClause.and(product.creatorId.eq(userId));

    return selectProductList()
        .where(whereClause)
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  @Override
  public Long countByUserIdAndSearchCondition(Long userId, ProductSearchCondition condition) {
    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(condition);
    whereClause.and(product.creatorId.eq(userId));
    return countProducts(whereClause);
  }

  private JPAQuery<ProductList> selectProductList() {
    return queryFactory
        .select(Projections.fields(ProductList.class,
            product.id,
            product.name,
            product.brand,
            product.status,
            product.price,
            product.viewCount,
            product.likeCount,
            product.shareCount,
            product.popularityScore,
            product.rejectReason,
            product.avgRating.as("averageRating"),
            ExpressionUtils.as(
                JPAExpressions
                    .select(productReview.count().intValue())
                    .from(productReview)
                    .where(productReview.product.id.eq(product.id)
                        .and(productReview.status.eq(ReviewStatus.ACTIVE))),
                "reviewCount"
            ),
            product.category.name.as("categoryName"),
            product.releaseDate,
            product.createdAt,
            Projections.fields(AttachmentFileResponse.class,
                productAttachmentFile.id,
                productAttachmentFile.attachFileId,
                productAttachmentFile.orgFileNm,
                productAttachmentFile.fileUrl,
                productAttachmentFile.fileSize,
                productAttachmentFile.fileExplain,
                productAttachmentFile.contentType
            ).as("thumbnailDto")
        ))
        .from(product)
        .leftJoin(product.category, category)
        .leftJoin(product.productReviews, productReview)
        .leftJoin(product.productAttachmentFiles, productAttachmentFile)
        .on(productAttachmentFile.product.id.eq(product.id)
            .and(productAttachmentFile.isPrimary.isTrue()));
  }


  @Override
  public void bulkUpdateScores(Map<Long, Double> scores) {
    executeCaseWhenUpdate(new ArrayList<>(scores.entrySet()));
  }

  private void executeCaseWhenUpdate(List<Map.Entry<Long, Double>> chunk) {
    String caseClause = chunk.stream()
            .map(e -> "WHEN %d THEN %f".formatted(e.getKey(), e.getValue()))
            .collect(Collectors.joining(" "));

    String idList = chunk.stream()
            .map(e -> e.getKey().toString())
            .collect(Collectors.joining(","));

    String sql = """
        UPDATE tb_product
        SET popularity_score = CASE product_id %s END,
            popularity_dirty = false,
            popularity_calculated_at = CURRENT_TIMESTAMP
        WHERE product_id IN (%s)
        """.formatted(caseClause, idList);

    jdbcTemplate.update(sql);
  }

  // 공통 count 쿼리
  private Long countProducts(BooleanBuilder whereClause) {
    Long count = queryFactory
        .select(product.countDistinct())
        .from(product)
        .leftJoin(product.category, category)
        .where(whereClause)
        .fetchOne();

    return count == null ? 0 : count;
  }

  private BooleanBuilder getWhereClauseWithSearchCondition(ProductSearchCondition searchCondition) {
    BooleanBuilder builder = new BooleanBuilder();

    Optional.ofNullable(searchCondition.getKeyword())
        .ifPresent(keyword -> builder.and(product.name.contains(keyword))
            .or(product.brand.contains(keyword)));

    Optional.ofNullable(searchCondition.getCategoryId())
        .ifPresent(id -> {
          BooleanExpression categoryCondition = searchCondition.getCategoryDepth() != null && searchCondition.getCategoryDepth() <= 1
              ? product.category.id.eq(id)
              .or(product.category.parent.id.eq(id))
              : product.category.id.eq(id);
          builder.and(categoryCondition);
        });

    Optional.ofNullable(searchCondition.getStatus())
        .ifPresent(status -> builder.and(product.status.eq(status)));

    //TODO: 나머지 조건 추가
    return builder;
  }


  /**
   * 정렬 조건 변환
   */
  private OrderSpecifier<?>[] getOrderSpecifier(Sort sort) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();

    for (Sort.Order order : sort) {
      Order direction = order.isAscending() ? Order.ASC : Order.DESC;
      String property = order.getProperty();

      switch (property) {
        case "createdAt" -> orders.add(new OrderSpecifier<>(direction, product.createdAt));
        case "updatedAt" -> orders.add(new OrderSpecifier<>(direction, product.updatedAt));
        case "releaseDate" -> orders.add(new OrderSpecifier<>(direction, product.releaseDate));
        case "likeCount" -> orders.add(new OrderSpecifier<>(direction, product.likeCount));
        case "popularityScore" ->
            orders.add(new OrderSpecifier<>(direction, product.popularityScore));
        case "viewCount" -> orders.add(new OrderSpecifier<>(direction, product.viewCount));
        default -> orders.add(new OrderSpecifier<>(Order.DESC, product.createdAt)); // 기본 정렬
      }
    }

    // 정렬 조건이 없으면 기본 정렬 적용
    if (orders.isEmpty()) {
      orders.add(new OrderSpecifier<>(Order.DESC, product.createdAt));
    }

    return orders.toArray(new OrderSpecifier[0]);
  }
}
