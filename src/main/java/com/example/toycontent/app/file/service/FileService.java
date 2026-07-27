package com.example.toycontent.app.file.service;

import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.file.domain.dto.UploadFileDto;
import com.example.toycontent.app.file.repository.AttachmentFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class FileService {

    private final AttachmentFileRepository attachmentFileRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    public UploadFileDto uploadFile(MultipartFile multipartFile) {
        if(multipartFile.isEmpty()) {
            throw new RuntimeException();
        }
        String originalFilename = multipartFile.getOriginalFilename();

        // 작성자가 업로드한 파일명 -> 서버 내부에서 관리하는 파일명
        // 파일명을 중복되지 않게끔 UUID로 정하고 ".확장자"는 그대로
        String storeFilename = UUID.randomUUID() + "." + extractExt(originalFilename);

        // 파일을 저장하는 부분 -> user.dir + uploadPath + storeFilename 에 저장
        // 다운로드(downloadFileAsBytes)와 동일하게 Path.of 가변인자로 OS 무관하게 경로 구성
        // (구분자를 "\\"로 하드코딩하면 Linux에서 저장 실패 → 전 업로드 500. NF-12 참조)
        Path target = Path.of(System.getProperty("user.dir"), uploadPath, storeFilename);

        try {
            Files.createDirectories(target.getParent());
            multipartFile.transferTo(target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AttachmentFile attachmentFile = attachmentFileRepository.save(AttachmentFile.builder()
                .orgFileNm(originalFilename)
                .storeFileNm(storeFilename)
                .build());

        return UploadFileDto.of(attachmentFile);
    }

    public ResponseEntity<byte[]> downloadFileAsBytes(Long fileId) throws IOException {
        // 데이터베이스에서 파일 정보 조회
        AttachmentFile attachmentFile = attachmentFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        // 저장된 파일 경로 생성
        Path filePath = Path.of(System.getProperty("user.dir"), uploadPath, attachmentFile.getStoreFileNm());

        // 파일을 바이트 배열로 읽기
        byte[] fileData;
        try {
            fileData = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽는 중 오류가 발생했습니다.", e);
        }

        // HTTP 응답 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachmentFile.getOrgFileNm() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(filePath)); // 파일의 MIME 타입 설정
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length));
        headers.setContentDispositionFormData("attachment", attachmentFile.getStoreFileNm());


        return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
    }

    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos + 1);
    }
}
