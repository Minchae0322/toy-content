package com.example.toycontent.app.carrier.repository;

import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.carrier.repository.querydsl.CarrierStickerRepositoryCustom;
import com.example.toycontent.app.common.enumuration.StickerType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierStickerRepository extends JpaRepository<CarrierSticker, Long>, CarrierStickerRepositoryCustom {

  int countByCarrierId(Long carrierId);

  Optional<CarrierSticker> findByCarrierIdAndStickerType(Long id, StickerType stickerType);
}