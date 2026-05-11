package com.example.toycontent.app.product.service;

import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import com.example.toycontent.app.product.controller.dto.ProductReviewRequest;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse.ReviewCreateResponse;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse.ReviewList;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.domain.ProductReview;
import com.example.toycontent.app.product.domain.ProductReviewAttachmentFile;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.product.repository.ProductReviewAttachmentFileRepository;
import com.example.toycontent.app.product.repository.ProductReviewRepository;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewAttachmentFileRepository productReviewAttachmentFileRepository;
    private final ExternalUserInfoService externalUserInfoService;

    @Transactional
    public ReviewCreateResponse createReview(Long productId,
        ProductReviewRequest.CreateReview createReviewDto, Long userId) {
        Product product = getActiveProduct(productId);

        String nickname = externalUserInfoService.getUserNickname(userId);
        ProductReview productReview = createReviewDto.toEntity(product, userId, nickname);
        productReviewRepository.save(productReview);

        createProductReviewAttachmentFiles(createReviewDto.getAttachmentFileInfos(), productReview);
        recalculateAvgRating(product);

        return ReviewCreateResponse.of(productReview);
    }

    @Transactional(readOnly = true)
    public List<ReviewList> getReviews(Long productId, Pageable pageable) {
        getActiveProduct(productId);

        List<ProductReview> productReviews = productReviewRepository.findProductReviews(productId,
            ReviewStatus.ACTIVE, pageable);

        return productReviews.stream()
            .map(ReviewList::of)
            .toList();
    }

    @Transactional
    public void updateReview(Long productId, Long reviewId,
        ProductReviewRequest.UpdateReview request, Long userId) {
        ProductReview review = getActiveReview(productId, reviewId);
        verifyOwner(review, userId);

        review.update(request.getRating(), request.getComment());

        if (request.getRating() != null) {
            recalculateAvgRating(review.getProduct());
        }
    }

    @Transactional
    public void deleteReview(Long productId, Long reviewId, Long userId) {
        ProductReview review = getActiveReview(productId, reviewId);
        verifyOwner(review, userId);

        review.delete();
        recalculateAvgRating(review.getProduct());
    }

    /**
     * ACTIVE 리뷰들의 AVG(rating) 으로 제품 평균 평점을 갱신한다.
     * 같은 트랜잭션 안의 직전 변경(save/dirty/soft-delete)은 JPA AUTO flush 로
     * AVG 쿼리 실행 전에 DB 에 반영되므로 결과는 최신 상태를 반영한다.
     */
    private void recalculateAvgRating(Product product) {
        Double avg = productReviewRepository.findAverageRating(product.getId(), ReviewStatus.ACTIVE);
        product.applyAvgRating(avg);
    }

    private Product getActiveProduct(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
            .orElseThrow(() -> new RestApiException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductReview getActiveReview(Long productId, Long reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
            .orElseThrow(() -> new RestApiException(ProductErrorCode.REVIEW_NOT_FOUND));
        if (review.getStatus() == ReviewStatus.DELETED
            || !review.getProduct().getId().equals(productId)) {
            throw new RestApiException(ProductErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    private void verifyOwner(ProductReview review, Long userId) {
        if (!review.getCreatorId().equals(userId)) {
            throw new RestApiException(ProductErrorCode.REVIEW_CREATOR_NOT_MATCH);
        }
    }

    private void createProductReviewAttachmentFiles(List<AttachmentInfo> attachmentInfos,
        ProductReview productReview) {
        List<ProductReviewAttachmentFile> detailFiles = IntStream.range(0, attachmentInfos.size())
            .mapToObj(i -> attachmentInfos.get(i).toEntity(productReview, i + 1))
            .toList();

        productReviewAttachmentFileRepository.saveAll(detailFiles);
    }
}
