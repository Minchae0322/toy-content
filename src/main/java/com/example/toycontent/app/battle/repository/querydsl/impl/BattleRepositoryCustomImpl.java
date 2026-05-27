package com.example.toycontent.app.battle.repository.querydsl.impl;

import static com.example.toycontent.app.battle.domain.QBattle.battle;
import static com.example.toycontent.app.battle.domain.QBattleAttachmentFile.battleAttachmentFile;
import static com.example.toycontent.app.battle.domain.QBattleItem.battleItem;
import static com.example.toycontent.app.category.domain.QCategory.category;
import static com.example.toycontent.app.product.domain.QProduct.product;

import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleHotList;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.querydsl.BattleRepositoryCustom;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.SubCategoryDetail;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
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
            battle.totalCommentCount,
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
        .leftJoin(category.parent)
        .where(whereClause)
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

  @Override
  public Page<BattleHotList> findHotBattlesWithSearchCondition(Pageable pageable) {

    List<BattleResponse.BattleHotList> content = queryFactory
        .select(Projections.fields(BattleHotList.class,
            battle.id,
            battle.title,
            battle.totalParticipants,
            battle.totalVotes,
            battle.totalViews,
            battle.totalCommentCount,
            battle.status,
            battle.endDate,
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
        .where(battle.isDeleted.isFalse())
        .orderBy(battle.hotScore.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    // 썸네일이 없는 배틀은 상위 아이템 이미지로 대체
    List<Long> battleIdsWithoutThumbnail = content.stream()
        .filter(b -> b.getThumbnailDto() == null)
        .map(BattleHotList::getId)
        .toList();

    if (!battleIdsWithoutThumbnail.isEmpty()) {
      Map<Long, List<String>> topImagesMap = fetchTopItemImagesForBattles(battleIdsWithoutThumbnail);
      content.forEach(b -> {
        if (b.getThumbnailDto() == null) {
          b.setTopItemImages(topImagesMap.getOrDefault(b.getId(), List.of()));
        }
      });
    }

    JPAQuery<Long> countQuery = queryFactory
        .select(battle.count())
        .from(battle)
        .where(battle.isDeleted.isFalse());

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }



  public Long countBattlesWithSearchCondition(BattleSearchCondition condition) {
    BooleanBuilder whereClause = getWhereClauseWithSearchCondition(condition);

    return queryFactory
        .select(battle.count())
        .from(battle)
        .where(whereClause)
        .fetchOne();
  }

  @Override
  public List<Battle> findBattlesNeedingTimeWeightUpdate(LocalDateTime activeThreshold) {

    return queryFactory
        .selectFrom(battle)
        .where(
            battle.isDeleted.eq(false),
            battle.status.eq(BattleStatus.NORMAL),
            battle.status.eq(BattleStatus.NORMAL)
                .and(battle.hotScoreUpdatedAt.lt(activeThreshold))
                .or(battle.hotScoreUpdatedAt.isNull())
        )
        .fetch();
  }

  @Override
  public List<Battle> findActiveAndUpcomingBattles() {
    return queryFactory
        .selectFrom(battle)
        .where(
            battle.isDeleted.eq(false),
            battle.status.eq(BattleStatus.NORMAL)
        )
        .fetch();
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
      if (condition.getCategoryDepth() != null && condition.getCategoryDepth() <= 1) {
        builder.and(battle.category.id.eq(condition.getCategory())
            .or(battle.category.parent.id.eq(condition.getCategory())));
      } else {
        builder.and(battle.category.id.eq(condition.getCategory()));
      }
    }

    // 배틀 상태 필터
    if (condition.getStatus() != null) {
      builder.and(battle.status.eq(condition.getStatus()));
    }

    // 배틀 진행 여부
    if (condition.getIsActive() != null) {
      LocalDateTime now = LocalDateTime.now();
      if (condition.getIsActive()) {
        // 진행중: 시작일 <= 현재 < 종료일
        builder.and(battle.startDate.loe(now))
            .and(battle.endDate.gt(now));
      } else {
        // 종료됨: 종료일 <= 현재
        builder.and(battle.endDate.loe(now));
      }
    }

    // 생성자 ID 필터
    if (condition.getCreatorId() != null) {
      builder.and(battle.creatorId.eq(condition.getCreatorId()));
    }

    // 키워드 검색 (제목 OR 활성 아이템명 — customName/product.name)
    if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
      String keyword = condition.getKeyword();
      builder.and(
          battle.title.containsIgnoreCase(keyword)
              .or(JPAExpressions.selectOne()
                  .from(battleItem)
                  .leftJoin(battleItem.product, product)
                  .where(
                      battleItem.battle.eq(battle),
                      battleItem.isDeleted.isFalse(),
                      battleItem.status.eq(BattleItemStatus.ACTIVE),
                      battleItem.customName.containsIgnoreCase(keyword)
                          .or(product.name.containsIgnoreCase(keyword))
                  )
                  .exists())
      );
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
        case "createdAt" -> orders.add(new OrderSpecifier<>(direction, battle.createdAt));
        case "updatedAt" -> orders.add(new OrderSpecifier<>(direction, battle.updatedAt));
        case "hotScore" -> orders.add(new OrderSpecifier<>(direction, battle.hotScore));
        case "endDate" -> orders.add(new OrderSpecifier<>(direction, battle.endDate));
        default -> orders.add(new OrderSpecifier<>(Order.DESC, battle.createdAt)); // 기본 정렬
      }
    }

    // 정렬 조건이 없으면 기본 정렬 적용
    if (orders.isEmpty()) {
      orders.add(new OrderSpecifier<>(Order.DESC, battle.createdAt));
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
