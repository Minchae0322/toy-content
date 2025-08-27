package com.example.toycontent.app.oneMouth.repository;

import com.example.toycontent.app.oneMouth.domain.OneMouthFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneMouthFavoriteRepository extends JpaRepository<OneMouthFavorite, Long> {


  boolean existsByUserId(Long viewerId);
}
