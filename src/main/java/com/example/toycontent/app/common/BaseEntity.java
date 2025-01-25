package com.example.toycontent.app.common;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@MappedSuperclass
@SuperBuilder
@EntityListeners(value = {AuditingEntityListener.class})
public abstract class BaseEntity {

    @CreatedBy
    @Column(nullable = false, updatable = false, name = "inputr_id")
    private Long id;
}
