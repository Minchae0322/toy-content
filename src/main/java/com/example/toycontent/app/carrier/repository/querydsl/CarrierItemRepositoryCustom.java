package com.example.toycontent.app.carrier.repository.querydsl;

import com.example.toycontent.app.carrier.domain.CarrierItem;

import java.util.List;

public interface CarrierItemRepositoryCustom {

    List<CarrierItem> findAllByCarrierIdWithProduct(Long carrierId);

    List<CarrierItem> findAllByIdInAndCarrierId(List<Long> ids, Long carrierId);

}
