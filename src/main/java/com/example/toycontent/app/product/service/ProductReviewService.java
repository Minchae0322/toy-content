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

    @Transactional
    public ReviewCreateResponse createReview(Long productId,
        ProductReviewRequest.CreateReview createReviewDto, Long userId, String userName) {
        Product product = getActiveProduct(productId);

        ProductReview productReview = createReviewDto.toEntity(product, userId, userName);
        productReviewRepository.save(productReview);

        createProductReviewAttachmentFiles(createReviewDto.getAttachmentFileInfos(), productReview);

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
    }

    @Transactional
    public void deleteReview(Long productId, Long reviewId, Long userId) {
        ProductReview review = getActiveReview(productId, reviewId);
        verifyOwner(review, userId);

        review.delete();
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
