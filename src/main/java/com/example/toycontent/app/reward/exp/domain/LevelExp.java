package com.example.toycontent.app.reward.exp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_level_exp")
@Comment("레벨별 필요 EXP 정의 테이블")
public class LevelExp {

  @Id
  @Column(name = "level")
  @Comment("레벨 (1~40)")
  private Integer level;

  @Column(name = "required_exp", nullable = false)
  @Comment("이전 레벨에서 이 레벨까지 필요 EXP")
  private Long requiredExp;

  @Column(name = "cumulative_exp", nullable = false)
  @Comment("이 레벨 도달에 필요한 누적 EXP")
  private Long cumulativeExp;
}
