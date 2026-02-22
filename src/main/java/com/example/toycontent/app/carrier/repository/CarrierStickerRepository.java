package com.example.toycontent.app.carrier.repository;

import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.carrier.repository.querydsl.CarrierStickerRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierStickerRepository extends JpaRepository<CarrierSticker, Long>, CarrierStickerRepositoryCustom {

    int countByCarrierId(Long carrierId);
}