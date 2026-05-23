package com.example.toycontent.app.notification.hotcontent;

import static com.example.toycontent.app.feed.domain.QFeed.feed;
import static com.example.toycontent.app.product.domain.QProduct.product;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class FeedHotContentSource implements HotContentSource {

  private static final String DEFAULT_DISPLAY_NAME = "피드";

  private final JPAQueryFactory queryFactory;

  @Override
  public HotContentType type() {
    return HotContentType.FEED;
  }

  @Override
  public List<HotContentCandidate> findTopCandidates(int limit) {
    List<Tuple> rows = queryFactory
        .select(feed.id, feed.hotScore, feed.productNameCustom, product.name)
        .from(feed)
        .leftJoin(feed.product, product)
        .where(
            feed.isDeleted.isFalse(),
            feed.hotScore.gt(0.0)
        )
        .orderBy(feed.hotScore.desc(), feed.id.desc())
        .limit(limit)
        .fetch();

    return rows.stream()
        .map(row -> HotContentCandidate.builder()
            .type(HotContentType.FEED)
            .contentId(row.get(feed.id))
            .displayName(resolveDisplayName(
                row.get(feed.productNameCustom),
                row.get(product.name)))
            .hotScore(row.get(feed.hotScore))
            .build())
        .toList();
  }

  private static String resolveDisplayName(String customName, String productName) {
    if (customName != null && !customName.isBlank()) {
      return customName;
    }
    if (productName != null && !productName.isBlank()) {
      return productName;
    }
    return DEFAULT_DISPLAY_NAME;
  }
}
