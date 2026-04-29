package com.example.toycontent.app.carrier.repository;

import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.carrier.repository.querydsl.CarrierStickerRepositoryCustom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrierStickerRepository extends JpaRepository<CarrierSticker, Long>, CarrierStickerRepositoryCustom {

  List<CarrierSticker> findAllByCarrierId(Long carrierId);
}
