package com.example.toycontent.app.oneMouth.controller;

import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.service.OneMouthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OneMouthController", description = "한입만 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/one-mouths")
public class OneMouthController {

    private final OneMouthService oneMouthService;

    @PostMapping("")
    @Operation(summary = "한입만 게시물 생성", tags = "OneMouthController")
    public ResponseEntity<Long> createOneMouth(
            @RequestBody @Valid OneMouthCreateDto createDto
    ) {
        return ResponseEntity.ok(oneMouthService.createOneMouth(createDto));
    }

    @PostMapping("/draft")
    @Operation(summary = "한입만 게시물 임시저장", tags = "OneMouthController")
    public ResponseEntity<Long> createOneMouthDraft(
            @RequestBody @Valid OneMouthDraftCreateDto createDto
    ) {
        return ResponseEntity.ok(oneMouthService.createOneMouthDraft(createDto));
    }
}
