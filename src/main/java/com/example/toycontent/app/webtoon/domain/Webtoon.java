package com.example.toycontent.app.webtoon.domain;

import com.example.toycontent.app.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import jakarta.persistence.EntityListeners;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@SuperBuilder
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
public class Webtoon extends BaseEntity {
}
