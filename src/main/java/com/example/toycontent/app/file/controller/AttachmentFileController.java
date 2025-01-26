package com.example.toycontent.app.file.controller;

import com.example.toycontent.app.file.domain.dto.UploadFileDto;
import com.example.toycontent.app.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AttachmentFileController", description = "첨부파일 API")
@RequestMapping("/attachment-file")
@RequiredArgsConstructor
@Slf4j
@RestController
public class AttachmentFileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드", description = "사용자가 파일을 업로드합니다.")
    public ResponseEntity<UploadFileDto> uploadFile(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadFile(file));
    }
}
