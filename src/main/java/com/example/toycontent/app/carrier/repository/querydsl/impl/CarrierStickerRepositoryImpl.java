package com.example.toycontent.app.carrier.repository.querydsl.impl;


import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.carrier.domain.QCarrierSticker;
import com.example.toycontent.app.carrier.repository.querydsl.CarrierStickerRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarrierStickerRepositoryImpl implements CarrierStickerRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QCarrierSticker carrierSticker = QCarrierSticker.carrierSticker;

    @Override
    public List<CarrierSticker> findAllByIdInAndCarrierId(List<Long> ids, Long carrierId) {
        return queryFactory
                .selectFrom(carrierSticker)
                .where(
                        carrierSticker.id.in(ids),
                        carrierSticker.carrier.id.eq(carrierId)
                )
                .fetch();
    }
}
