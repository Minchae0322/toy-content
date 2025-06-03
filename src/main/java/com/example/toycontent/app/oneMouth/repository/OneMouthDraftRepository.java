package com.example.toycontent.app.oneMouth.repository;

import com.example.toycontent.app.oneMouth.domain.OneMouthDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OneMouthDraftRepository extends JpaRepository<OneMouthDraft, Long> {
    Optional<OneMouthDraft> findBySellerId(Long sellerId);
}
