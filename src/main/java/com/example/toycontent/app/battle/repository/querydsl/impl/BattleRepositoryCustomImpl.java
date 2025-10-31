package com.example.toycontent.app.battle.repository.querydsl.impl;

import static com.example.toycontent.app.battle.domain.QBattle.battle;
import static com.example.toycontent.app.battle.domain.QBattleAttachmentFile.battleAttachmentFile;
import static com.example.toycontent.app.battle.domain.QBattleItem.battleItem;
import static com.example.toycontent.app.category.domain.QCategory.category;
import static com.example.toycontent.app.product.domain.QProduct.product;

import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.repository.querydsl.BattleRepositoryCustom;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.SubCategoryDetail;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class BattleRepositoryCustomImpl implements BattleRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<BattleResponse.BattleList> findBattlesWithSearchCondition(
      BattleSearchCondition condition, Pageable pageable) {

    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(condition);

    // 배틀 기본 정보 조회
    List<BattleResponse.BattleList> content = queryFactory
        .select(Projections.fields(BattleList.class,
            battle.id,
            battle.title,
            Projections.fields(SubCategoryDetail.class,
                category.id.as("subCategoryId"),
                category.name.as("subCategoryName"),
                category.parent.id.as("categoryId"),
                category.parent.name.as("categoryName")
            ).as("subCategoryDetail"),
            battle.status,
            battle.itemAddPermissionType,
            battle.totalParticipants,
            battle.totalVotes,
            battle.totalViews,
            battle.startDate,
            battle.endDate,
            battle.createdAt,
            battle.creatorId.as("creatorId"),
            // 썸네일 DTO 매핑
            Projections.fields(AttachmentFileResponse.class,
                battleAttachmentFile.id,
                battleAttachmentFile.orgFileNm,
                battleAttachmentFile.fileUrl,
                battleAttachmentFile.fileSize,
                battleAttachmentFile.fileExplain,
                battleAttachmentFile.contentType
            ).as("thumbnailDto")
        ))
        .from(battle)
        .leftJoin(battle.battleAttachmentFiles, battleAttachmentFile)
        .on(battleAttachmentFile.isPrimary.isTrue())
        .join(battle.category, category)
        .join(category.parent)
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    // 썸네일이 없는 배틀 ID 수집
    List<Long> battleIdsWithoutThumbnail = content.stream()
        .filter(battleList -> battleList.getThumbnailDto() == null)
        .map(BattleResponse.BattleList::getId)
        .toList();

    // 한 번의 쿼리로 모든 상위 아이템 이미지 조회
    if (!battleIdsWithoutThumbnail.isEmpty()) {
      Map<Long, List<String>> topImagesMap = fetchTopItemImagesForBattles(battleIdsWithoutThumbnail);

      // 매핑
      content.forEach(battleList -> {
        if (battleList.getThumbnailDto() == null) {
          battleList.setTopItemImages(topImagesMap.getOrDefault(battleList.getId(), List.of()));
        }
      });
    }

    return content;
  }



  public Long countBattlesWithSearchCondition(BattleSearchCondition condition) {
    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(condition);

    return queryFactory
        .select(battle.count())
        .from(battle)
        .where(whereClause)
        .fetchOne();
  }

  /**
   * 검색 조건 생성
   */
  private BooleanBuilder getWhereClauseWithSearchCondition(BattleSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder();

    // 기본 조건: 삭제되지 않은 배틀
    builder.and(battle.isDeleted.isFalse());

    // 카테고리 필터
    if (condition.getCategory() != null) {
      builder.and(battle.category.id.eq(condition.getCategory())
          .or(battle.category.parent.id.eq(condition.getCategory())));
    }


    // 배틀 상태 필터
    if (condition.getStatus() != null) {
      builder.and(battle.status.eq(condition.getStatus()));
    }

    // 생성자 ID 필터
    if (condition.getCreatorId() != null) {
      builder.and(battle.creatorId.eq(condition.getCreatorId()));
    }

    // 키워드 검색 (제목)
    if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
      builder.and(battle.title.containsIgnoreCase(condition.getKeyword()));
    }

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
        default -> orders.add(new OrderSpecifier<>(Order.DESC, product.createdAt)); // 기본 정렬
      }
    }

    // 정렬 조건이 없으면 기본 정렬 적용
    if (orders.isEmpty()) {
      orders.add(new OrderSpecifier<>(Order.DESC, product.createdAt));
    }

    return orders.toArray(new OrderSpecifier[0]);
  }
  /**
   * 배틀의 상위 아이템 이미지 조회 (최대 4개)
   */
  private List<String> getTopItemImages(Long battleId) {
    return queryFactory
        .select(battleItem.customImageUrl)
        .from(battleItem)
        .where(battleItem.battle.id.eq(battleId)
            .and(battleItem.isDeleted.isFalse())
            .and(battleItem.status.eq(BattleItemStatus.ACTIVE))
            .and(battleItem.customImageUrl.isNotNull()))
        .orderBy(battleItem.displayOrder.asc())
        .limit(4)
        .fetch();
  }

  /**
   * 여러 배틀의 상위 아이템 이미지를 한 번에 조회
   */
  private Map<Long, List<String>> fetchTopItemImagesForBattles(List<Long> battleIds) {
    // 서브쿼리로 각 배틀의 상위 4개 아이템만 가져오기
    List<Tuple> results = queryFactory
        .select(
            battleItem.battle.id,
            battleItem.customImageUrl
        )
        .from(battleItem)
        .where(
            battleItem.battle.id.in(battleIds)
                .and(battleItem.isDeleted.isFalse())
                .and(battleItem.status.eq(BattleItemStatus.ACTIVE))
                .and(battleItem.customImageUrl.isNotNull())
                .and(battleItem.displayOrder.lt(
                    JPAExpressions
                        .select(battleItem.displayOrder.add(4))
                        .from(battleItem)
                        .where(battleItem.battle.id.eq(battleItem.battle.id))
                        .orderBy(battleItem.displayOrder.asc())
                        .limit(1)
                ))
        )
        .orderBy(battleItem.battle.id.asc(), battleItem.displayOrder.asc())
        .fetch();

    // battleId별로 그룹핑
    return results.stream()
        .collect(Collectors.groupingBy(
            tuple -> tuple.get(battleItem.battle.id),
            Collectors.mapping(
                tuple -> tuple.get(battleItem.customImageUrl),
                Collectors.toList()
            )
        ));
  }

}
