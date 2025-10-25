package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.OneMouthStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.OneMouthErrorCode;
import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import com.example.toycontent.app.file.repository.AttachmentFileRepository;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse.Detail;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse.ListView;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthUpdateDto;
import com.example.toycontent.app.oneMouth.controller.dto.SalePostRequest.SalePostCreateRequest;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import com.example.toycontent.app.oneMouth.domain.SalePostAttachmentFile;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraft;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraftAttachmentFile;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.repository.SalePostAttachmentFileRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftAttachmentFileRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthFavoriteRepository;
import com.example.toycontent.app.oneMouth.repository.SalePostRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalePostService {

  private final SalePostRepository salePostRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OneMouthDraftRepository draftRepository;
    private final AttachmentFileRepository attachmentFileRepository;
    private final SalePostAttachmentFileRepository salePostAttachmentFileRepository;
    private final OneMouthDraftAttachmentFileRepository oneMouthDraftAttachmentFileRepository;
    private final OneMouthFavoriteRepository oneMouthFavoriteRepository;

  private final SaleOptionAdderFactory saleOptionAdderFactory;

  @Transactional
  public OneMouthResponse.SalePostCreateResponse createSalePost(SalePostCreateRequest request, Long sellerId) {

    // 공식 제품 조회 (있는 경우)
    Product product = Optional.ofNullable(request.getProductId())
        .flatMap(productRepository::findById)
        .orElse(null);

    // 게시글 생성
    SalePost salePost = toEntity(request, sellerId, product);

    addSaleOption(salePost, request);

    createSalePostAttachmentFiles(request.getThumbnailAttachmentInfo(),
        request.getAttachmentFileInfos(), salePost);

    SalePost savedPost = salePostRepository.save(salePost);
    return OneMouthResponse.SalePostCreateResponse.of(savedPost);

  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void addSaleOption(SalePost salePost, SalePostCreateRequest request) {
    SaleOptionAdder adder = saleOptionAdderFactory.getAdder(request.getSaleType());
    adder.addOption(salePost, request.getOptionDto());
  }

  private SalePost toEntity(SalePostCreateRequest request, Long sellerId, Product product) {
    return SalePost.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .sellerId(sellerId)
        .saleType(request.getSaleType())
        .status(OneMouthStatus.ON_SALE)
        .tradeLocation(request.getTradeLocation())
        .product(product)
        .build();
  }

    @Transactional
    public Long createOneMouthDraft(OneMouthDraftCreateDto draftCreateDto) {
        Optional<OneMouthDraft> optionalDraft = draftRepository.findBySellerId(draftCreateDto.getSellerId());

        OneMouthDraft draft = optionalDraft
                .map(existing -> updateExistingDraft(existing, draftCreateDto))
                .orElseGet(() -> {
                    OneMouthDraft createdOneMouthDraft = draftRepository.save(draftCreateDto.toEntity());
                    createOneMouthDraftAttachmentFiles(createdOneMouthDraft, draftCreateDto.getAttachmentFileIds());
                    return createdOneMouthDraft;
                });

        return draft.getId();
    }

    private OneMouthDraft updateExistingDraft(OneMouthDraft existing, OneMouthDraftCreateDto dto) {
        deleteOneMouthDraftAttachmentFiles(existing);
        existing.update();
        createOneMouthDraftAttachmentFiles(existing, dto.getAttachmentFileIds());
        return existing;
    }


  private void createSalePostAttachmentFiles(AttachmentInfo thumbnailAttachmentInfo,
      List<AttachmentInfo> attachmentInfos,
      SalePost salePost) {
    // 대표 이미지 파일 생성
    SalePostAttachmentFile primaryImage = createAttachmentFile(thumbnailAttachmentInfo, salePost, 0, true);

    // 상세 이미지 파일 생성 (순서 부여)
    List<SalePostAttachmentFile> detailFiles = IntStream.range(0, attachmentInfos.size())
        .mapToObj(i -> createAttachmentFile(attachmentInfos.get(i), salePost, i + 1, false))
        .toList();

    // 대표 + 상세 이미지 통합 저장
    salePostAttachmentFileRepository.saveAll(
        Stream.concat(Stream.of(primaryImage), detailFiles.stream()).toList()
    );
  }

  private SalePostAttachmentFile createAttachmentFile(AttachmentInfo info, SalePost salePost, int order, boolean isPrimary) {
    return info.toEntity(salePost, order, isPrimary);
  }


  private void createOneMouthDraftAttachmentFiles(OneMouthDraft oneMouthDraft, List<Long> attachmentFileIds) {
        IntStream.range(0, attachmentFileIds.size())
                .forEach(i -> {
                    Long attachmentFileId = attachmentFileIds.get(i);
                    AttachmentFile attachmentFile = attachmentFileRepository.findById(attachmentFileId)
                            .orElseThrow(() -> new IllegalArgumentException("첨부파일 ID가 존재하지 않습니다: " + attachmentFileId));

                    OneMouthDraftAttachmentFile fileMapping = OneMouthDraftAttachmentFile.builder()
                            .oneMouthDraft(oneMouthDraft)
                            .attachmentFile(attachmentFile)
                            .sortOrder(i)
                            .build();

                    oneMouthDraftAttachmentFileRepository.save(fileMapping);
                });
    }

    private void deleteOneMouthDraftAttachmentFiles(OneMouthDraft oneMouthDraft) {
        oneMouthDraft.getOneMouthDraftAttachmentFiles().clear();
    }


    public Detail getOneMouthDetail(Long id, Long viewerId) {
      SalePost salePost = salePostRepository.findById(id).orElseThrow(
          () -> new RestApiException(OneMouthErrorCode.ONE_MOUTH_NOT_FOUND));

      boolean isFavorite = oneMouthFavoriteRepository.existsByUserId(viewerId);

      return OneMouthResponse.Detail.from(salePost, isFavorite);
    }

    public Page<ListView> getPagedOneMouthPosts(Pageable pageable, OneMouthSearchCondition condition) {
        List<ListView> oneMouthRespons = salePostRepository.searchByCondition(condition, pageable);
        long totalCount = salePostRepository.countByCondition(condition);

        return new PageImpl<>(oneMouthRespons, pageable, totalCount);
    }

    public Detail updateOneMouth(Long id, OneMouthUpdateDto updateDto) {
        return null;
    }

    public void deleteOneMouth(Long id) {
    }
}
