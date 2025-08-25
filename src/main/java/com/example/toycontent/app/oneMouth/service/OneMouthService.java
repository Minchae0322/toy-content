package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.repository.OneMouthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OneMouthService {

    private final OneMouthRepository oneMouthRepository;
    private final CategoryRepository categoryRepository;

    public Long createOneMouth(OneMouthCreateDto createDto) {
        Category category = categoryRepository.findById(createDto.getCategoryId()).orElseThrow();

        return oneMouthRepository.save(createDto.toEntity(category)).getId();
    }

    public Long createOneMouthDraft(OneMouthDraftCreateDto draftCreateDto) {
        Category category = categoryRepository.findById(draftCreateDto.getCategoryId()).orElseThrow();

        return oneMouthRepository.save(draftCreateDto.toEntity(category)).getId();
    }
}
