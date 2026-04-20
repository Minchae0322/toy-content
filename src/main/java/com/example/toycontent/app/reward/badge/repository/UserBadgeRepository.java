package com.example.toycontent.app.reward.badge.repository;

import com.example.toycontent.app.reward.badge.domain.UserBadge;
import com.example.toycontent.app.reward.badge.repository.querydsl.UserBadgeRepositoryCustom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long>,
    UserBadgeRepositoryCustom {

  List<UserBadge> findByUserIdAndRevokedFalse(Long userId);

  Optional<UserBadge> findByUserIdAndBadgeId(Long userId, Long badgeId);

  boolean existsByUserIdAndBadgeIdAndRevokedFalse(Long userId, Long badgeId);

  long countByUserIdAndRevokedFalse(Long userId);

  List<UserBadge> findByUserIdAndPinnedTrueAndRevokedFalse(Long userId);
}
