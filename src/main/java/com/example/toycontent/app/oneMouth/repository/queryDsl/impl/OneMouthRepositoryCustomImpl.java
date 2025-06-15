package com.example.toycontent.app.oneMouth.repository.queryDsl.impl;

import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import com.example.toycontent.app.oneMouth.repository.queryDsl.OneMouthRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.toycontent.app.oneMouth.domain.QOneMouth.oneMouth;

@Repository
@RequiredArgsConstructor
public class OneMouthRepositoryCustomImpl implements OneMouthRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<OneMouthResponse> searchByCondition(OneMouthSearchCondition condition, Pageable pageable) {
        BooleanBuilder where = where(condition);

        List<OneMouthResponse> content = queryFactory
                .select(Projections.constructor(OneMouthResponse.class,
                        oneMouth.id,
                        oneMouth.title,
                        oneMouth.quantity,
                        oneMouth.unit,
                        oneMouth.price,
                        oneMouth.productStatus,
                        oneMouth.description,
                        oneMouth.sellerId,
                        oneMouth.category.id,
                        oneMouth.location,
                        oneMouth.createdAt,
                        oneMouth.updatedAt,
                        oneMouth.productType,
                        oneMouth.hits
                ))
                .from(oneMouth)
                .where(where)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(oneMouth.createdAt.desc())
                .fetch();

        // 2. 게시글 ID 추출
        List<Long> ids = content.stream().map(OneMouthResponse::getOneMouthId).toList();

        return content;
    }

    @Override
    public long countByCondition(OneMouthSearchCondition condition) {
        BooleanBuilder where = where(condition);

        Long count = queryFactory
                .select(oneMouth.count())
                .from(oneMouth)
                .where(where)
                .fetchOne();

        return Optional.ofNullable(count).orElse(0L);
    }

    private BooleanBuilder where(OneMouthSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        Optional.ofNullable(condition.getKeyword())
                .ifPresent(keyword -> builder.and(oneMouth.title.contains(keyword).or(oneMouth.description.contains(keyword))));

        return builder;
    }

}
