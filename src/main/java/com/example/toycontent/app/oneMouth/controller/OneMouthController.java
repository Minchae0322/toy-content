package com.example.toycontent.app.oneMouth.controller;

import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthDraftCreateDto;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthListView;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthListView.Detail;
import com.example.toycontent.app.oneMouth.controller.dto.OneMouthUpdateDto;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import com.example.toycontent.app.oneMouth.service.OneMouthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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


    @Operation(summary = "한입만 게시물 상세 조회", description = "한입만 게시물 ID로 게시물 상세정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<Detail> getOneMouthDetail(
            @PathVariable @Parameter(description = "게시물 ID") Long id) {
        Detail product = oneMouthService.getOneMouthDetail(id);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "한입만 게시물 목록 페이징 조회", description = "게시물 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<OneMouthListView>> getPagedOneMouthPosts(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @ParameterObject OneMouthSearchCondition condition
    ) {
        Page<OneMouthListView> products = oneMouthService.getPagedOneMouthPosts(pageable, condition);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "한입만 게시물 수정", description = "한입만 게시물 ID에 해당하는 상품 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<Detail> updateOneMouth(
            @PathVariable @Parameter(description = "게시물 ID") Long id,
            @RequestBody @Parameter(description = "수정할 게시물 정보") OneMouthUpdateDto updateDto) {
        Detail updated = oneMouthService.updateOneMouth(id, updateDto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "한입만 게시물 삭제", description = "한입만 게시물 ID에 해당하는 상품을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOneMouth(
            @PathVariable @Parameter(description = "게시물 ID") Long id) {
        oneMouthService.deleteOneMouth(id);
        return ResponseEntity.noContent().build();
    }
}
