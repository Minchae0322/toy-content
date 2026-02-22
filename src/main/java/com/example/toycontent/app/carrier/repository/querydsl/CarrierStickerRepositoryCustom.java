package com.example.toycontent.app.carrier.repository.querydsl;

import com.example.toycontent.app.carrier.domain.CarrierSticker;

import java.util.List;

public interface CarrierStickerRepositoryCustom {

    List<CarrierSticker> findAllByIdInAndCarrierId(List<Long> ids, Long carrierId);
}
