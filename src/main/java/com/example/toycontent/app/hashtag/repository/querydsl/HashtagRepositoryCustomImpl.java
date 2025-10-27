package com.example.toycontent.app.hashtag.repository.querydsl;

import static com.example.toycontent.app.hashtag.domain.QHashtag.hashtag;

import com.example.toycontent.app.hashtag.controller.dto.HashtagSearchCondition;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HashtagRepositoryCustomImpl implements HashtagRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<Hashtag> findHotHashtags(HashtagSearchCondition condition, Pageable pageable) {
    List<Hashtag> content = queryFactory
        .selectFrom(hashtag)
        .where(
            nameContains(condition.getName()),
            usageCountGoe(condition.getMinUsageCount())
        )
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    JPAQuery<Long> countQuery = queryFactory
        .select(hashtag.count())
        .from(hashtag)
        .where(
            nameContains(condition.getName()),
            usageCountGoe(condition.getMinUsageCount())
        );

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  private BooleanExpression nameContains(String name) {
    return name != null ? hashtag.name.contains(name) : null;
  }

  private BooleanExpression usageCountGoe(Long minUsageCount) {
    return minUsageCount != null ? hashtag.usageCount.goe(minUsageCount) : null;
  }

  private OrderSpecifier<?>[] getOrderSpecifier(Sort sort) {
    if (!sort.isSorted()) {
      // 기본 정렬: usageCount 내림차순
      return new OrderSpecifier[]{new OrderSpecifier<>(Order.DESC, hashtag.usageCount)};
    }

    return sort.stream()
        .map(order -> {
          Order direction = order.isAscending() ? Order.ASC : Order.DESC;
          String property = order.getProperty();

          PathBuilder<Hashtag> pathBuilder = new PathBuilder<>(Hashtag.class,
              hashtag.getMetadata());
          return new OrderSpecifier(direction, pathBuilder.get(property));

        })
        .toArray(OrderSpecifier[]::new);
  }
}