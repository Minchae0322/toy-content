package com.example.toycontent.app.file.service;

import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.file.domain.dto.UploadFileDto;
import com.example.toycontent.app.file.repository.AttachmentFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        Path rootPath = Paths.get(System.getProperty("user.dir"));
        // 작성자가 업로드한 파일명 -> 서버 내부에서 관리하는 파일명
        // 파일명을 중복되지 않게끔 UUID로 정하고 ".확장자"는 그대로
        String storeFilename = UUID.randomUUID() + "." + extractExt(originalFilename);

        // 파일을 저장하는 부분 -> 파일경로 + storeFilename 에 저장

        try {
            multipartFile.transferTo(Path.of(rootPath + "\\" + uploadPath + storeFilename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AttachmentFile attachmentFile = attachmentFileRepository.save(AttachmentFile.builder()
                .orgFileNm(originalFilename)
                .storeFileNm(storeFilename)
                .build());

        return UploadFileDto.of(attachmentFile);
    }

    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos + 1);
    }
}
