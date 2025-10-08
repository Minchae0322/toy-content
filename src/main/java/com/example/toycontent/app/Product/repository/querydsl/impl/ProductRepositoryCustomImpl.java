package com.example.toycontent.app.Product.repository.querydsl.impl;

import static com.example.toycontent.app.Product.domain.QProduct.product;
import static com.example.toycontent.app.Product.domain.QProductAttachmentFile.productAttachmentFile;
import static com.example.toycontent.app.Product.domain.QProductReview.productReview;
import static com.example.toycontent.app.category.domain.QCategory.category;
import static com.example.toycontent.app.file.domain.QAttachmentFile.attachmentFile;

import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.Product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.Product.domain.QProductAttachmentFile;
import com.example.toycontent.app.Product.repository.querydsl.ProductRepositoryCustom;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.file.domain.dto.AttachmentFileDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

  private final JPAQueryFactory queryFactory;


  @Override
  public List<ProductList> findBySearchCondition(ProductSearchCondition searchCondition,
      Pageable pageable) {

    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(searchCondition);

    return queryFactory
        .select(Projections.fields(ProductList.class,
            product.id,
            product.name,
            product.brand,
            product.status,
            product.price,
            product.viewCount,
            product.likeCount,
            product.avgRating.as("averageRating"),
            ExpressionUtils.as(
                JPAExpressions
                    .select(productReview.count().intValue())  // 여기서 변환
                    .from(productReview)
                    .where(productReview.product.id.eq(product.id)
                        .and(productReview.status.eq(ReviewStatus.ACTIVE))),
                "reviewCount"
            ),
            product.category.name.as("categoryName"),
            product.releaseDate,
            product.createdAt,
            // 썸네일 DTO 매핑
            Projections.fields(AttachmentFileDto.class,
                productAttachmentFile.id,
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
            .and(productAttachmentFile.isPrimary.isTrue()))
        .where(whereClause)
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  public Long countBySearchCondition(ProductSearchCondition searchCondition) {
    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(searchCondition);

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
        .ifPresent(id -> builder.and(product.category.id.eq(id)));


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
