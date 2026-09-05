package com.example.toycontent.app.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.product.controller.dto.ProductReviewRequest;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.domain.ProductReview;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.product.repository.ProductReviewAttachmentFileRepository;
import com.example.toycontent.app.product.repository.ProductReviewRepository;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewService")
class ProductReviewServiceTest {

  private static final long PRODUCT_ID = 10L;
  private static final long REVIEW_ID = 100L;
  private static final long USER_ID = 7L;
  private static final String NICKNAME = "민채";

  @Mock private ProductRepository productRepository;
  @Mock private ProductPopularityService popularityService;
  @Mock private ProductReviewRepository productReviewRepository;
  @Mock private ProductReviewAttachmentFileRepository productReviewAttachmentFileRepository;
  @Mock private ExternalUserInfoService externalUserInfoService;

  @InjectMocks private ProductReviewService productReviewService;

  private Product product() {
    return Product.builder()
        .id(PRODUCT_ID)
        .name("테스트 상품")
        .isDeleted(false)
        .avgRating(0.0)
        .build();
  }

  private ProductReview activeReview(Product product, int rating) {
    return ProductReview.builder()
        .id(REVIEW_ID)
        .product(product)
        .creatorId(USER_ID)
        .creatorName(NICKNAME)
        .rating(rating)
        .status(ReviewStatus.ACTIVE)
        .build();
  }

  @Nested
  @DisplayName("createReview")
  class CreateReview {

    @Test
    @DisplayName("외부 유저 서비스의 닉네임을 creatorName으로 저장하고 avgRating을 재계산한다")
    void 닉네임_저장_및_avgRating_재계산() {
      // given
      Product product = product();
      ProductReviewRequest.CreateReview request = ProductReviewRequest.CreateReview.builder()
          .productId(PRODUCT_ID)
          .rating(5)
          .comment("좋아요")
          .attachmentFileInfos(new ArrayList<>())
          .build();

      given(productRepository.findByIdAndIsDeletedFalse(PRODUCT_ID))
          .willReturn(Optional.of(product));
      given(externalUserInfoService.getUserNickname(USER_ID)).willReturn(NICKNAME);
      given(productReviewRepository.findAverageRating(PRODUCT_ID, ReviewStatus.ACTIVE))
          .willReturn(5.0);

      // when
      productReviewService.createReview(PRODUCT_ID, request, USER_ID);

      // then
      ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
      then(productReviewRepository).should().save(captor.capture());
      ProductReview saved = captor.getValue();

      assertSoftly(softly -> {
        softly.assertThat(saved.getCreatorName()).as("닉네임이 creatorName 으로 저장").isEqualTo(NICKNAME);
        softly.assertThat(saved.getCreatorId()).isEqualTo(USER_ID);
        softly.assertThat(saved.getRating()).isEqualTo(5);
        softly.assertThat(product.getAvgRating()).as("AVG 결과가 product 에 반영").isEqualTo(5.0);
      });
    }
  }

  @Nested
  @DisplayName("updateReview")
  class UpdateReview {

    @Test
    @DisplayName("rating 이 변경되면 avgRating 을 재계산한다")
    void rating_변경_시_재계산() {
      // given
      Product product = product();
      product.applyAvgRating(5.0);
      ProductReview review = activeReview(product, 5);
      ProductReviewRequest.UpdateReview request = ProductReviewRequest.UpdateReview.builder()
          .rating(3)
          .build();

      given(productReviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));
      given(productReviewRepository.findAverageRating(PRODUCT_ID, ReviewStatus.ACTIVE))
          .willReturn(3.0);

      // when
      productReviewService.updateReview(PRODUCT_ID, REVIEW_ID, request, USER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(review.getRating()).isEqualTo(3);
        softly.assertThat(product.getAvgRating()).isEqualTo(3.0);
      });
    }

    @Test
    @DisplayName("rating 이 null 이면 avgRating 재계산을 호출하지 않는다")
    void rating_null_이면_재계산_스킵() {
      // given
      Product product = product();
      product.applyAvgRating(5.0);
      ProductReview review = activeReview(product, 5);
      ProductReviewRequest.UpdateReview request = ProductReviewRequest.UpdateReview.builder()
          .comment("코멘트만 수정")
          .build();

      given(productReviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));

      // when
      productReviewService.updateReview(PRODUCT_ID, REVIEW_ID, request, USER_ID);

      // then
      then(productReviewRepository).should(never())
          .findAverageRating(org.mockito.ArgumentMatchers.anyLong(),
              org.mockito.ArgumentMatchers.any(ReviewStatus.class));
      assertThat(product.getAvgRating()).as("avgRating 유지").isEqualTo(5.0);
    }
  }

  @Nested
  @DisplayName("deleteReview")
  class DeleteReview {

    @Test
    @DisplayName("soft delete 후 avgRating 을 재계산한다")
    void soft_delete_후_재계산() {
      // given
      Product product = product();
      product.applyAvgRating(4.0);
      ProductReview review = activeReview(product, 4);

      given(productReviewRepository.findById(REVIEW_ID)).willReturn(Optional.of(review));
      given(productReviewRepository.findAverageRating(PRODUCT_ID, ReviewStatus.ACTIVE))
          .willReturn(null); // 남은 ACTIVE 리뷰 없음

      // when
      productReviewService.deleteReview(PRODUCT_ID, REVIEW_ID, USER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(review.getStatus()).isEqualTo(ReviewStatus.DELETED);
        softly.assertThat(product.getAvgRating())
            .as("리뷰가 모두 사라지면 AVG null → 0.0 으로 초기화").isEqualTo(0.0);
      });
    }
  }
}
