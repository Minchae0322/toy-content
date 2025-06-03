package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraft;
import com.example.toycontent.app.oneMouth.repository.OneMouthDraftRepository;
import com.example.toycontent.app.oneMouth.repository.OneMouthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OneMouthService {

    private final OneMouthRepository oneMouthRepository;
    private final CategoryRepository categoryRepository;
    private final OneMouthDraftRepository draftRepository;

    public Long createOneMouth(OneMouthCreateDto createDto) {
        draftRepository.findBySellerId(createDto.getSellerId()).ifPresent(draftRepository::delete);
        Category category = categoryRepository.findById(createDto.getCategoryId()).orElseThrow();

        return oneMouthRepository.save(createDto.toEntity(category)).getId();
    }

    public Long createOneMouthDraft(OneMouthDraftCreateDto draftCreateDto) {
        Category category = categoryRepository.findById(draftCreateDto.getCategoryId()).orElseThrow();

        Optional<OneMouthDraft> optionalDraft = draftRepository.findBySellerId(draftCreateDto.getSellerId());

        OneMouthDraft draft = optionalDraft
                .map(OneMouthDraft::update)
                .orElseGet(() -> draftRepository.save(draftCreateDto.toEntity(category)));

        return draft.getId();
    }
}
