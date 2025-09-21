package com.example.toycontent.app.oneMouth.repository.queryDsl.impl;

import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse.ListView;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import com.example.toycontent.app.oneMouth.domain.QOneMouthFavorite;
import com.example.toycontent.app.oneMouth.repository.queryDsl.OneMouthRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.toycontent.app.oneMouth.domain.QOneMouth.oneMouth;
import static com.example.toycontent.app.oneMouth.domain.QOneMouthFavorite.oneMouthFavorite;

@Repository
@RequiredArgsConstructor
public class OneMouthRepositoryCustomImpl implements OneMouthRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ListView> searchByCondition(OneMouthSearchCondition condition, Pageable pageable) {
        BooleanBuilder where = where(condition);

        List<ListView> content = queryFactory
            .select(Projections.fields(ListView.class,
                oneMouth.id.as("oneMouthId"),
                oneMouth.title,
                oneMouth.quantity,
                oneMouth.unit,
                oneMouth.price,
                oneMouth.description,
                oneMouth.sellerId,
                oneMouth.location,
                oneMouth.createdAt,
                oneMouth.updatedAt,
                oneMouth.hits,
                oneMouth.thumbnailUrl,

                //관심 수
                Expressions.as(
                    JPAExpressions
                        .select(oneMouthFavorite.count().intValue())
                        .from(oneMouthFavorite)
                        .where(oneMouthFavorite.oneMouth.id.eq(oneMouth.id)),
                    "favoritesCount"
                )
            ))
            .from(oneMouth)
            .where(where)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(oneMouth.createdAt.desc())
            .fetch();

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
