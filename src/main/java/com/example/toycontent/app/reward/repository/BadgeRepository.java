package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.Badge;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

  Optional<Badge> findByCode(String code);

  boolean existsByCode(String code);

  List<Badge> findByActivatedTrue();

  List<Badge> findByCategory(String category);

  List<Badge> findByIsSeasonalTrueAndSeasonCode(String seasonCode);
}
