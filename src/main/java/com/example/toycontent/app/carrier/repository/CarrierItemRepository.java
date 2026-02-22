package com.example.toycontent.app.carrier.repository;

import com.example.toycontent.app.carrier.domain.CarrierItem;
import com.example.toycontent.app.carrier.repository.querydsl.CarrierItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierItemRepository extends JpaRepository<CarrierItem, Long>, CarrierItemRepositoryCustom {

    boolean existsByCarrierIdAndProductId(Long carrierId, Long productId);

    int countByCarrierId(Long carrierId);
}
