package com.example.toycontent.app.carrier.service;


import com.example.toycontent.app.carrier.controller.dto.CarrierRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierResponse;
import com.example.toycontent.app.carrier.domain.Carrier;
import com.example.toycontent.app.carrier.repository.CarrierRepository;
import com.example.toycontent.app.common.enumuration.CarrierSkinType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CarrierErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarrierService {

    private static final int MAX_CARRIER_COUNT = 5;

    private final CarrierRepository carrierRepository;

    public List<CarrierResponse.Summary> getMyCarriers(Long userId) {
        return carrierRepository.findAllByUserIdOrderByIsDefaultDescCreatedAtAsc(userId)
                .stream()
                .map(CarrierResponse.Summary::from)
                .toList();
    }

    public CarrierResponse.Detail getCarrier(Long carrierId, Long userId) {
        Carrier carrier = getCarrierByOwner(carrierId, userId);
        return CarrierResponse.Detail.from(carrier);
    }

    @Transactional
    public CarrierResponse.Summary createCarrier(CarrierRequest.CreateCarrier request, Long userId) {
        if (carrierRepository.countByUserId(userId) >= MAX_CARRIER_COUNT) {
            throw new RestApiException(CarrierErrorCode.MAX_CARRIER_EXCEEDED);
        }

        boolean isFirst = carrierRepository.findByUserIdAndIsDefaultTrue(userId).isEmpty();

        Carrier carrier = Carrier.builder()
                .userId(userId)
                .skinType(request.getSkinType() != null ? request.getSkinType() : CarrierSkinType.DEFAULT)
                .skinColor(request.getSkinColor())
                .isDefault(isFirst)
                .build();

        carrierRepository.save(carrier);

        return CarrierResponse.Summary.from(carrier);
    }

    @Transactional
    public CarrierResponse.Summary updateCarrierSkin(Long carrierId, CarrierRequest.UpdateSkin request, Long userId) {
        Carrier carrier = getCarrierByOwner(carrierId, userId);

        carrier.updateSkin(request.getSkinType(), request.getSkinColor());
        return CarrierResponse.Summary.from(carrier);
    }

    @Transactional
    public void deleteCarrier(Long carrierId, Long userId) {
        Carrier carrier = getCarrierByOwner(carrierId, userId);

        if (carrier.getIsDefault()) {
            throw new RestApiException(CarrierErrorCode.DEFAULT_CARRIER_CANNOT_DELETE);
        }

        carrierRepository.delete(carrier);
    }

    public Carrier getCarrierByOwner(Long carrierId, Long userId) {
        return carrierRepository.findByIdAndUserId(carrierId, userId)
                .orElseThrow(() -> new RestApiException(CarrierErrorCode.CARRIER_ACCESS_DENIED));
    }
}