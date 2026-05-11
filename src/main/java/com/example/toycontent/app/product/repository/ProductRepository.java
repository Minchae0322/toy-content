package com.example.toycontent.app.product.repository;

import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.querydsl.ProductRepositoryCustom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

  /** 삭제되지 않은 제품 단건 조회 */
  Optional<Product> findByIdAndIsDeletedFalse(Long id);


  // dirty 상품 조회
  @Query("""
        SELECT p FROM Product p
        WHERE p.popularityDirty = true
        AND p.status = 'APPROVED'
        AND p.isDeleted = false
        ORDER BY p.updatedAt DESC
        """)
  List<Product> findByPopularityDirtyTrue(PageRequest of);

  /** 승인 완료된 제품 수 조회 (삭제 제외) */
  long countByStatusAndIsDeletedFalse(ProductStatus status);

  // 상태별 조회 (삭제 제외)
  Slice<Product> findByStatusAndIsDeletedFalse(ProductStatus status, Pageable pageable);

  // 벌크 점수 업데이트
  @Modifying(clearAutomatically = true)
  @Query("""
        UPDATE Product p
        SET p.popularityScore = :score,
            p.popularityCalculatedAt = CURRENT_TIMESTAMP,
            p.popularityDirty = false
        WHERE p.id = :productId
        """)
  void updatePopularityScore(@Param("productId") Long productId, @Param("score") Double score);

  // dirty 마킹
  @Modifying
  @Query("""
        UPDATE Product p
        SET p.popularityDirty = true
        WHERE p.id = :productId
        """)
  void markPopularityDirty(@Param("productId") Long productId);
}
