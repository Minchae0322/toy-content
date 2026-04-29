package com.example.toycontent.app.feedback.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tb_feedback", indexes = {
    @Index(name = "idx_feedback_created_at", columnList = "created_at DESC")
})
public class Feedback extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Comment("의견 ID")
  private Long id;

  @Column(nullable = false, length = 100)
  @Comment("제목")
  private String title;

  @Column(nullable = false, length = 2000)
  @Comment("내용")
  private String content;

  public static Feedback create(String title, String content) {
    return Feedback.builder()
        .title(title)
        .content(content)
        .build();
  }
}
