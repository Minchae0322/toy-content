package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.OneMouthErrorCode;
import com.example.toycontent.app.file.controller.dto.AttachmentFileRequest.SimpleDto;
import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.file.repository.AttachmentFileRepository;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse.Detail;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse.ListView;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthUpdateDto;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
import com.example.toycontent.app.oneMouth.domain.OneMouthAttachmentFile;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraft;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraftAttachmentFile;
import com.example.toycontent.app.oneMouth.repository.OneMouthAttachmentFileRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftAttachmentFileRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthFavoriteRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OneMouthService {

    private final OneMouthRepository oneMouthRepository;
    private final CategoryRepository categoryRepository;
    private final OneMouthDraftRepository draftRepository;
    private final AttachmentFileRepository attachmentFileRepository;
    private final OneMouthAttachmentFileRepository oneMouthAttachmentFileRepository;
    private final OneMouthDraftAttachmentFileRepository oneMouthDraftAttachmentFileRepository;
    private final OneMouthFavoriteRepository oneMouthFavoriteRepository;

    @Transactional
    public Long createOneMouth(OneMouthCreateDto createDto) {
        draftRepository.findBySellerId(createDto.getSellerId()).ifPresent(draftRepository::delete);

        OneMouth createdOneMouth = oneMouthRepository.save(createDto.toEntity());
        createOneMouthAttachmentFiles(createdOneMouth, createDto.getAttachmentFileSimpleDtos());

        return createdOneMouth.getId();
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


    private void createOneMouthAttachmentFiles(OneMouth oneMouth, List<SimpleDto> simpleDtos) {
        IntStream.range(0, simpleDtos.size())
                .forEach(i -> {
                    SimpleDto simpleDto = simpleDtos.get(i);

                  OneMouthAttachmentFile fileMapping = OneMouthAttachmentFile.builder()
                      .oneMouth(oneMouth)
                      .attachFileId(simpleDto.getAttachFileId())
                      .fileExplain(simpleDto.getFileExplain())
                      .fileSize(simpleDto.getFileSize())
                      .fileUrl(simpleDto.getFileUrl())
                      .orgFileNm(simpleDto.getOrgFileNm())
                      .sortOrder(i)
                      .build();

                    oneMouthAttachmentFileRepository.save(fileMapping);
                });
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
      OneMouth oneMouth = oneMouthRepository.findById(id).orElseThrow(
          () -> new RestApiException(OneMouthErrorCode.ONE_MOUTH_NOT_FOUND));

      boolean isFavorite = oneMouthFavoriteRepository.existsByUserId(viewerId);

      return OneMouthResponse.Detail.from(oneMouth, isFavorite);
    }

    public Page<ListView> getPagedOneMouthPosts(Pageable pageable, OneMouthSearchCondition condition) {
        List<ListView> oneMouthRespons = oneMouthRepository.searchByCondition(condition, pageable);
        long totalCount = oneMouthRepository.countByCondition(condition);

        return new PageImpl<>(oneMouthRespons, pageable, totalCount);
    }

    public Detail updateOneMouth(Long id, OneMouthUpdateDto updateDto) {
        return null;
    }

    public void deleteOneMouth(Long id) {
    }
}
