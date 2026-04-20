package com.example.toycontent.app.reward.badge.repository;

import com.example.toycontent.app.reward.badge.domain.CategoryMastery;
import com.example.toycontent.app.reward.badge.repository.querydsl.CategoryMasteryRepositoryCustom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryMasteryRepository extends JpaRepository<CategoryMastery, Long>,
    CategoryMasteryRepositoryCustom {

  Optional<CategoryMastery> findByUserIdAndCategoryId(Long userId, Long categoryId);

  List<CategoryMastery> findByUserId(Long userId);
}
