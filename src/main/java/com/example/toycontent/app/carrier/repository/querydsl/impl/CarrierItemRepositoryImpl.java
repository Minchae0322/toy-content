package com.example.toycontent.app.carrier.repository.querydsl.impl;

import com.example.toycontent.app.carrier.domain.CarrierItem;
import com.example.toycontent.app.carrier.domain.QCarrierItem;
import com.example.toycontent.app.carrier.repository.querydsl.CarrierItemRepositoryCustom;
import com.example.toycontent.app.product.domain.QProduct;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CarrierItemRepositoryImpl implements CarrierItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QCarrierItem carrierItem = QCarrierItem.carrierItem;
    private final QProduct product = QProduct.product;

    @Override
    public List<CarrierItem> findAllByCarrierIdWithProduct(Long carrierId) {
        return queryFactory
                .selectFrom(carrierItem)
                .join(carrierItem.product, product).fetchJoin()
                .where(carrierItem.carrier.id.eq(carrierId))
                .orderBy(carrierItem.zIndex.desc())
                .fetch();
    }

    @Override
    public List<CarrierItem> findAllByIdInAndCarrierId(List<Long> ids, Long carrierId) {
        return queryFactory
                .selectFrom(carrierItem)
                .join(carrierItem.product, product).fetchJoin()
                .where(
                        carrierItem.id.in(ids),
                        carrierItem.carrier.id.eq(carrierId)
                )
                .fetch();
    }
}