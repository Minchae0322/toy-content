package com.example.toycontent.app.product.service;


import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.common.dto.CursorResponse;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.product.controller.dto.ProductReactionResponse.ProductUserReaction;
import com.example.toycontent.app.product.controller.dto.ProductRequest;
import com.example.toycontent.app.product.controller.dto.ProductRequest.ProductStatusRequest;
import com.example.toycontent.app.product.controller.dto.ProductResponse;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductBattle;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductCreate;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductDetail;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductFeed;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.product.controller.dto.ProductReviewRequest;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse.ReviewCreateResponse;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse.ReviewList;
import com.example.toycontent.app.product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.domain.ProductAttachmentFile;
import com.example.toycontent.app.product.domain.ProductReview;
import com.example.toycontent.app.product.domain.ProductReviewAttachmentFile;
import com.example.toycontent.app.product.repository.ProductAttachmentFileRepository;
import com.example.toycontent.app.product.repository.ProductReactionRepository;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.product.repository.ProductReviewAttachmentFileRepository;
import com.example.toycontent.app.product.repository.ProductReviewRepository;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private final FeedRepository feedRepository;
    private final ExternalUserInfoService externalUserInfoService;
    private final BattleRepository battleRepository;
    private final BattleItemRepository battleItemRepository;

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
        Product newProduct = productRepository.save(productDto.toEntity(category, userId));

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

      /*  // 활성 리뷰 목록 조회
        List<ProductReviewResponse.ReviewList> productReviewResponses = productReviewRepository
            .searchProductReviews(product.getId(), ReviewStatus.ACTIVE);*/

        // 종합 DTO로 변환 후 반환
        return ProductDetail.of(product, productUserReaction);
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
    @Transactional(readOnly = true)
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

    /**
     * 특정 사용자가 등록한 제품 목록을 페이징하여 조회합니다.
     *
     * @param userId    조회할 사용자 ID
     * @param condition 검색 조건 (카테고리, 상태, 키워드 등)
     * @param pageable  페이징 및 정렬 정보
     * @return 사용자가 등록한 제품 목록 (페이징 처리)
     */
    @Transactional(readOnly = true)
    public Page<ProductList> getProductsByUserId(Long userId, ProductSearchCondition condition, Pageable pageable) {
        // 조건 기반 조회
        List<ProductList> productLists = productRepository.findByUserIdAndSearchCondition(userId, condition, pageable);
        Long totalCount = productRepository.countByUserIdAndSearchCondition(userId, condition);

        return new PageImpl<>(productLists, pageable, totalCount);
    }

    @Transactional
    public ReviewCreateResponse createReview(Long productId, ProductReviewRequest.CreateReview createReviewDto, Long userId, String userName) {
        Product product = getProductById(productId);

        ProductReview productReview = createReviewDto.toEntity(product, userId, userName);
        productReviewRepository.save(productReview);

        createProductReviewAttachmentFiles(
            createReviewDto.getAttachmentFileInfos(),
            productReview
        );

        return ReviewCreateResponse.of(productReview);
    }

    public List<ReviewList> getReviews(Long productId, Pageable pageable) {
        Product product = getProductById(productId);

        List<ProductReview> productReviews = productReviewRepository.findProductReviews(productId,
            ReviewStatus.ACTIVE,
            pageable);

        return productReviews.stream()
            .map(ReviewList::of)
            .toList();
    }

    @Transactional(readOnly = true)
    public CursorResponse<ProductFeed> findProductFeeds(Long productId,  Long cursor,
        Integer requestSize) {

        Product product = getProductById(productId);

        List<Feed> productFeeds = feedRepository.findByProductIdAndIsDeletedNot(product.getId(),
            false, cursor, requestSize + 1);

        List<ProductResponse.ProductFeed> productFeedListDto = toListView(productFeeds);

        return CursorResponse.of(productFeedListDto, requestSize, ProductFeed::getFeedId);
    }

    /**
     * Feed 리스트 -> ProductFeed 변환
     */
    private List<ProductResponse.ProductFeed> toListView(List<Feed> feeds) {
        List<Long> creatorIds = feeds.stream()
            .map(Feed::getUserId)
            .toList();

        Map<Long, ExternalUserInfo> externalUserInfoMap = externalUserInfoService.getUserInfos(
            creatorIds);

        return feeds.stream()
            .map(feed -> ProductFeed.from(feed,
                externalUserInfoMap.get(feed.getUserId())))
            .toList();
    }


    @Transactional(readOnly = true)
    public CursorResponse<ProductBattle> findProductBattles(Long productId, Long cursor, int size) {
        Product product = getProductById(productId);

        List<Battle> battlesContainingProduct = battleRepository.findBattlesContainingProduct(
            product.getId(), cursor, size + 1);

        List<Long> battleIds = battlesContainingProduct.stream().map(Battle::getId).toList();
        List<BattleItem> allItems = battleItemRepository.findItemsByBattleIds(battleIds);

        Map<Long, List<BattleItem>> itemsByBattle = allItems.stream()
            .collect(Collectors.groupingBy(bi -> bi.getBattle().getId(), LinkedHashMap::new,
                Collectors.toList()));

        List<ProductBattle> content = battlesContainingProduct.stream()
            .map(battle -> ProductBattle.from(battle, itemsByBattle.get(battle.getId()), productId))
            .toList();

        return CursorResponse.of(content, size, ProductBattle::getBattleId);
    }

    @Transactional
    public ProductResponse.ProductUpdate updateProductStatus(Long productId, ProductStatusRequest request) {
        Product product = getProductById(productId);

        product.updateStatus(request.getStatus(), request.getRejectReason());

        return ProductResponse.ProductUpdate.of(product);
    }


    private Product getProductById(Long productId) {
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
