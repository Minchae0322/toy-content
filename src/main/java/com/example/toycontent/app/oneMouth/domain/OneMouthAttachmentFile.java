package com.example.toycontent.app.oneMouth.domain;


import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.common.enumuration.FileCode;
import com.example.toycontent.app.file.domain.AttachmentFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_one_mouth_attachment_file")
public class OneMouthAttachmentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "one_mouth_id")
    private OneMouth oneMouth;

    @Column(name = "attch_file_id")
    @Comment("첨부 파일 아이디")
    private Long attachFileId;

    @Column(name = "file_url", length = 2000)
    private String fileUrl;

    @Column(name = "org_file_nm")
    private String orgFileNm;

    @Column(name = "file_size")
    private Long fileSize; // 파일크기 표시용

    @Column(name = "content_type")
    private String contentType; // 파일타입 아이콘 표시용

    private Integer sortOrder;

    private String fileExplain;

    private Boolean isPrimary;
}
