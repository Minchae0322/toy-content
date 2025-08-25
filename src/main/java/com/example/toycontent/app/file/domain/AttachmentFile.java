package com.example.toycontent.app.file.domain;


import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.FileCode;
import com.example.toycontent.app.common.enumuration.FileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_attch_file")
@EntityListeners(AuditingEntityListener.class)
public class AttachmentFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attch_file_id", unique = true, nullable = false)
    @Comment("첨부 파일 아이디")
    private Long id;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    @Comment("파일 코드")
    private FileCode fileCode;

    @Column(name = "org_file_nm")
    @Comment("원본 파일 명")
    private String orgFileNm;

    @Column(name = "store_file_nm")
    @Comment("저장 파일 명")
    private String storeFileNm;

    @Column(length = 2000)
    @Comment("파일 url")
    private String fileUrl;

    @Comment("파일 사이즈")
    private Long fileSize;

    @Column(name = "file_explain", length = 2000)
    @Comment("파일 설명")
    private String fileExplain;

    @Comment("파일 상태")
    private FileStatus fileStatus;

}
