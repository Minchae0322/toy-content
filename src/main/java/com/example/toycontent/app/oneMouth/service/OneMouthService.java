package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.file.repository.AttachmentFileRepository;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthResponse;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthUpdateDto;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
import com.example.toycontent.app.oneMouth.domain.OneMouthAttachmentFile;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraft;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraftAttachmentFile;
import com.example.toycontent.app.oneMouth.repository.OneMouthAttachmentFileRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftAttachmentFileRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftRepository;
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

    @Transactional
    public Long createOneMouth(OneMouthCreateDto createDto) {
        draftRepository.findBySellerId(createDto.getSellerId()).ifPresent(draftRepository::delete);
        Category category = categoryRepository.findById(createDto.getCategoryId()).orElseThrow();

        OneMouth createdOneMouth = oneMouthRepository.save(createDto.toEntity(category));
        createOneMouthAttachmentFiles(createdOneMouth, createDto.getAttachmentFileIds());

        return createdOneMouth.getId();
    }

    @Transactional
    public Long createOneMouthDraft(OneMouthDraftCreateDto draftCreateDto) {
        Category category = categoryRepository.findById(draftCreateDto.getCategoryId()).orElseThrow();

        Optional<OneMouthDraft> optionalDraft = draftRepository.findBySellerId(draftCreateDto.getSellerId());

        OneMouthDraft draft = optionalDraft
                .map(existing -> updateExistingDraft(existing, draftCreateDto))
                .orElseGet(() -> {
                    OneMouthDraft createdOneMouthDraft = draftRepository.save(draftCreateDto.toEntity(category));
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


    private void createOneMouthAttachmentFiles(OneMouth oneMouth, List<Long> attachmentFileIds) {
        IntStream.range(0, attachmentFileIds.size())
                .forEach(i -> {
                    Long attachmentFileId = attachmentFileIds.get(i);
                    AttachmentFile attachmentFile = attachmentFileRepository.findById(attachmentFileId)
                            .orElseThrow(() -> new IllegalArgumentException("첨부파일 ID가 존재하지 않습니다: " + attachmentFileId));

                    OneMouthAttachmentFile fileMapping = OneMouthAttachmentFile.builder()
                            .oneMouth(oneMouth)
                            .attachmentFile(attachmentFile)
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


    public OneMouthResponse.Get getOneMouthDetail(Long id) {
        return null;
    }

    public Page<OneMouthResponse> getPagedOneMouthPosts(Pageable pageable, OneMouthSearchCondition condition) {
        List<OneMouthResponse> oneMouthResponses = oneMouthRepository.searchByCondition(condition, pageable);
        long totalCount = oneMouthRepository.countByCondition(condition);

        return new PageImpl<>(oneMouthResponses, pageable, totalCount);
    }

    public OneMouthResponse.Get updateOneMouth(Long id, OneMouthUpdateDto updateDto) {
        return null;
    }

    public void deleteOneMouth(Long id) {
    }
}
