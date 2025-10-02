package com.example.toycontent.app.Product.domain;

import com.example.toycontent.app.oneMouth.domain.OneMouth;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_product_attachment_file")
public class ProductAttachmentFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

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
