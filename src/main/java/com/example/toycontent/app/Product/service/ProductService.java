package com.example.toycontent.app.Product.service;


import com.example.toycontent.app.Product.controller.dto.ProductReactionResponse.ProductUserReaction;
import com.example.toycontent.app.Product.controller.dto.ProductRequest;
import com.example.toycontent.app.Product.controller.dto.ProductResponse;
import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductCreate;
import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductDetail;
import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.Product.controller.dto.ProductReviewRequest;
import com.example.toycontent.app.Product.controller.dto.ProductReviewResponse;
import com.example.toycontent.app.Product.controller.dto.ProductReviewResponse.ReviewCreateResponse;
import com.example.toycontent.app.Product.controller.dto.ProductReviewResponse.ReviewList;
import com.example.toycontent.app.Product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.Product.domain.ProductAttachmentFile;
import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.Product.domain.ProductReviewAttachmentFile;
import com.example.toycontent.app.Product.repository.ProductAttachmentFileRepository;
import com.example.toycontent.app.Product.repository.ProductReactionRepository;
import com.example.toycontent.app.Product.repository.ProductRepository;
import com.example.toycontent.app.Product.repository.ProductReviewAttachmentFileRepository;
import com.example.toycontent.app.Product.repository.ProductReviewRepository;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReactionRepository productReactionRepository;
    private final ProductReviewRepository productReviewRepository;
    private final CategoryRepository categoryRepository;
    private final ProductAttachmentFileRepository productAttachmentFileRepository;
    private final ProductReviewAttachmentFileRepository productReviewAttachmentFileRepository;

    /**
     * 제품 등록
     * - 카테고리 유효성 검증 후 신규 Product 생성
     * - 대표 이미지 + 상세 이미지 파일 생성 및 저장
     * - 등록된 Product 정보를 DTO로 반환
     *
     * @param productDto 제품 등록 요청 DTO
     * @param userId     등록자(요청 사용자) ID
     */
    @Transactional
    public ProductResponse.ProductCreate createProduct(ProductRequest.ProductCreate productDto, Long userId) {
        // 카테고리 유효성 검증
        Category category = categoryRepository.findById(productDto.getCategoryId())
            .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        // 신규 제품 엔티티 생성 및 저장
        Product newProduct = productRepository.save(productDto.toEntity(category));

        // 대표 이미지 및 상세 이미지 파일 생성
        createProductAttachmentFiles(
            productDto.getThumbnailAttachmentInfo(),
            productDto.getAttachmentFileInfos(),
            newProduct
        );

        // 생성된 Product DTO 반환
        return ProductCreate.of(newProduct);
    }

    /**
     * 제품 첨부파일(대표 이미지 + 상세 이미지) 생성
     * - 썸네일(대표 이미지)와 상세 이미지 파일을 각각 엔티티로 변환 후 일괄 저장
     */
    private void createProductAttachmentFiles(AttachmentInfo thumbnailAttachmentInfo,
        List<AttachmentInfo> attachmentInfos,
        Product product) {
        // 대표 이미지 파일 생성
        ProductAttachmentFile primaryImage = createAttachmentFile(thumbnailAttachmentInfo, product, 0, true);

        // 상세 이미지 파일 생성 (순서 부여)
        List<ProductAttachmentFile> detailFiles = IntStream.range(0, attachmentInfos.size())
            .mapToObj(i -> createAttachmentFile(attachmentInfos.get(i), product, i + 1, false))
            .toList();

        // 대표 + 상세 이미지 통합 저장
        productAttachmentFileRepository.saveAll(
            Stream.concat(Stream.of(primaryImage), detailFiles.stream()).toList()
        );
    }

    /**
     * 제품 첨부파일(대표 이미지 + 상세 이미지) 생성 - 썸네일(대표 이미지)와 상세 이미지 파일을 각각 엔티티로 변환 후 일괄 저장
     */
    private void createProductReviewAttachmentFiles(List<AttachmentInfo> attachmentInfos,
        ProductReview productReview) {

        // 상세 이미지 파일 생성 (순서 부여)
        List<ProductReviewAttachmentFile> detailFiles = IntStream.range(0, attachmentInfos.size())
            .mapToObj(
                i -> createAttachmentFile(attachmentInfos.get(i), productReview, i + 1))
            .toList();

        // 대표 + 상세 이미지 통합 저장
        productReviewAttachmentFileRepository.saveAll(
           detailFiles
        );
    }

    /**
     * 개별 첨부파일 생성 헬퍼 메서드
     * - AttachmentInfo → ProductAttachmentFile 변환
     * - 순서(order)와 대표 여부(isPrimary) 설정 포함
     */
    private ProductAttachmentFile createAttachmentFile(AttachmentInfo info, Product product, int order, boolean isPrimary) {
        return info.toEntity(product, order, isPrimary);
    }

    private ProductReviewAttachmentFile createAttachmentFile(AttachmentInfo info, ProductReview productReview, int order) {
        return info.toEntity(productReview, order);
    }

    /**
     * 제품 상세 조회
     * - 제품 기본 정보 조회
     * - 로그인 사용자의 반응 정보(좋아요 등) 조회
     * - 활성 상태의 리뷰 목록 조회
     * - 통합 DTO(ProductDetail)로 변환하여 반환
     *
     * @param id            제품 ID
     * @param currentUserId 현재 로그인한 사용자 ID (null 허용)
     */
    public ProductResponse.ProductDetail getProduct(Long id, Long currentUserId) {
        // 제품 기본 정보 조회
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RestApiException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 사용자 반응 정보 (로그인 사용자가 있을 때만 조회)
        ProductUserReaction productUserReaction = Optional.ofNullable(currentUserId)
            .map(userId -> productReactionRepository.findByUserIdAndProductIdAndIsActiveTrue(userId, id))
            .map(ProductUserReaction::of)
            .orElse(ProductUserReaction.createDefault());

        // 활성 리뷰 목록 조회
        List<ProductReviewResponse.ReviewList> productReviewResponses = productReviewRepository
            .searchProductReviews(product.getId(), ReviewStatus.ACTIVE);

        // 종합 DTO로 변환 후 반환
        return ProductDetail.of(product, productUserReaction, productReviewResponses);
    }

    /**
     * 제품 목록 조회 (검색 조건 + 페이징)
     * - 관리자 여부에 따라 승인 상태 필터링
     * - 검색 조건 기반 목록/총 개수 조회
     * - Page 객체로 감싸서 반환
     *
     * @param searchCondition 검색 조건 객체
     * @param pageable        페이징 정보
     * @param isAdmin         관리자 여부
     */
    public Page<ProductResponse.ProductList> getAllProducts(
        ProductSearchCondition searchCondition, Pageable pageable, boolean isAdmin) {

        // 일반 사용자는 승인된 상품만 조회 가능
        if (!isAdmin) {
            searchCondition.setStatus(ProductStatus.APPROVED);
        }

        // 조건 기반 조회
        List<ProductList> productLists = productRepository.findBySearchCondition(searchCondition, pageable);
        Long totalCount = productRepository.countBySearchCondition(searchCondition);

        // 페이징 객체 생성
        return new PageImpl<>(productLists, pageable, totalCount);
    }

    @Transactional
    public ReviewCreateResponse createReview(Long productId, ProductReviewRequest.CreateReview createReviewDto, Long userId, String userName) {
        Product product = findProductByIdOrElseThrow(productId);

        ProductReview productReview = createReviewDto.toEntity(product, userId, userName);
        productReviewRepository.save(productReview);

        createProductReviewAttachmentFiles(
            createReviewDto.getAttachmentFileInfos(),
            productReview
        );

        return ReviewCreateResponse.of(productReview);
    }

    private Product findProductByIdOrElseThrow(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new RestApiException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * TODO: 제품 수정
     * - 제품명, 브랜드, 카테고리, 이미지 변경 로직 구현 예정
     */
    public ProductResponse updateProduct(Long id, ProductResponse productDto) {
        return null;
    }

    /**
     * TODO: 제품 삭제
     * - 상태 변경 (soft delete) 또는 물리 삭제 정책에 따라 구현 예정
     */
    public void deleteProduct(Long id) {
    }



}
