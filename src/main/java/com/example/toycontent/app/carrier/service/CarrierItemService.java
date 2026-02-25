package com.example.toycontent.app.carrier.service;

import com.example.toycontent.app.carrier.controller.dto.CarrierItemRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierItemResponse;
import com.example.toycontent.app.carrier.controller.dto.CarrierStickerRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierStickerResponse;
import com.example.toycontent.app.carrier.domain.Carrier;
import com.example.toycontent.app.carrier.domain.CarrierItem;
import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.carrier.repository.CarrierItemRepository;
import com.example.toycontent.app.carrier.repository.CarrierStickerRepository;
import com.example.toycontent.app.common.enumuration.StickerType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CarrierErrorCode;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarrierItemService {

    private static final int MAX_ITEM_COUNT = 30;
    private static final int MAX_STICKER_COUNT = 20;

    private final CarrierService carrierService;
    private final CarrierItemRepository carrierItemRepository;
    private final CarrierStickerRepository carrierStickerRepository;
    private final ProductRepository productRepository;


    @Transactional
    public CarrierItemResponse.Detail addItem(Long carrierId, CarrierItemRequest.AddItem request, Long userId) {
        Carrier carrier = carrierService.getCarrierByOwner(carrierId, userId);

        if (carrierItemRepository.existsByCarrierIdAndProductId(carrierId, request.getProductId())) {
            throw new RestApiException(CarrierErrorCode.ITEM_DUPLICATE_PRODUCT);
        }

        if (carrierItemRepository.countByCarrierId(carrierId) >= MAX_ITEM_COUNT) {
            throw new RestApiException(CarrierErrorCode.MAX_ITEM_EXCEEDED);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RestApiException(CarrierErrorCode.PRODUCT_NOT_FOUND));

        CarrierItem item = CarrierItem.builder()
                .carrier(carrier)
                .product(product)
                .positionX(request.getPositionX())
                .positionY(request.getPositionY())
                .zIndex(request.getZIndex() != null ? request.getZIndex() : 0)
                .build();

        carrierItemRepository.save(item);

        return CarrierItemResponse.Detail.from(item);
    }

    @Transactional
    public CarrierItemResponse.Detail updateItemPosition(Long carrierId, Long itemId, CarrierItemRequest.UpdatePosition request, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        CarrierItem item = carrierItemRepository.findById(itemId)
                .orElseThrow(() -> new RestApiException(CarrierErrorCode.ITEM_NOT_FOUND));

        item.updatePosition(
                request.getPositionX(),
                request.getPositionY(),
                request.getZIndex() != null ? request.getZIndex() : item.getZIndex()
        );

        return CarrierItemResponse.Detail.from(item);
    }

    @Transactional
    public List<CarrierItemResponse.Detail> updateItemPositions(Long carrierId, List<CarrierItemRequest.UpdatePositionBulk> request, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        List<Long> itemIds = request.stream()
                .map(CarrierItemRequest.UpdatePositionBulk::getItemId)
                .toList();

        List<CarrierItem> items = carrierItemRepository.findAllByIdInAndCarrierId(itemIds, carrierId);

        if (items.size() != itemIds.size()) {
            throw new RestApiException(CarrierErrorCode.ITEM_NOT_IN_CARRIER);
        }

        Map<Long, CarrierItemRequest.UpdatePositionBulk> requestMap = request.stream()
                .collect(Collectors.toMap(CarrierItemRequest.UpdatePositionBulk::getItemId, r -> r));

        items.forEach(item -> {
            CarrierItemRequest.UpdatePositionBulk req = requestMap.get(item.getId());
            item.updatePosition(
                    req.getPositionX(),
                    req.getPositionY(),
                    req.getZIndex() != null ? req.getZIndex() : item.getZIndex()
            );
        });

        return items.stream()
                .map(CarrierItemResponse.Detail::from)
                .toList();
    }

    @Transactional
    public void removeItem(Long carrierId, Long itemId, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        CarrierItem item = carrierItemRepository.findById(itemId)
                .orElseThrow(() -> new RestApiException(CarrierErrorCode.ITEM_NOT_FOUND));

        carrierItemRepository.delete(item);
    }

    @Transactional
    public CarrierStickerResponse.Detail addSticker(Long carrierId, CarrierStickerRequest.AddSticker request, Long userId) {
        Carrier carrier = carrierService.getCarrierByOwner(carrierId, userId);

        // PHOTO_TAG 스티커는 캐리어당 1개만 존재 (upsert 처리)
        if (request.getStickerType() == StickerType.PHOTO_TAG) {
            return upsertPhotoTagSticker(carrier, request);
        }

        return createNormalSticker(carrier, request, carrierId);
    }

    /**
     * PHOTO_TAG 스티커 upsert
     * - 이미 존재하면 내용 업데이트, 없으면 새로 생성
     */
    private CarrierStickerResponse.Detail upsertPhotoTagSticker(Carrier carrier, CarrierStickerRequest.AddSticker request) {
        CarrierSticker sticker = carrierStickerRepository
            .findByCarrierIdAndStickerType(carrier.getId(), StickerType.PHOTO_TAG)
            .map(existing -> {
                existing.updatePhotoTag(request);
                return existing;
            })
            .orElseGet(() -> createSticker(carrier, request));

        carrierStickerRepository.save(sticker);
        return CarrierStickerResponse.Detail.from(sticker);
    }

    /**
     * 일반 스티커 생성
     * - 최대 개수 초과 시 예외 발생
     */
    private CarrierStickerResponse.Detail createNormalSticker(Carrier carrier, CarrierStickerRequest.AddSticker request, Long carrierId) {
        if (carrierStickerRepository.countByCarrierId(carrierId) >= MAX_STICKER_COUNT) {
            throw new RestApiException(CarrierErrorCode.MAX_STICKER_EXCEEDED);
        }

        CarrierSticker sticker = createSticker(carrier, request);
        carrierStickerRepository.save(sticker);
        return CarrierStickerResponse.Detail.from(sticker);
    }

    private CarrierSticker createSticker(Carrier carrier, CarrierStickerRequest.AddSticker request) {
        return CarrierSticker.builder()
            .carrier(carrier)
            .stickerType(request.getStickerType())
            .content(request.getContent())
            .imageUrl(request.getImageUrl())
            .positionX(request.getPositionX())
            .positionY(request.getPositionY())
            .zIndex(request.getZIndex() != null ? request.getZIndex() : 0)
            .rotation(request.getRotation() != null ? request.getRotation() : 0.0)
            .scaleRatio(request.getScaleRatio() != null ? request.getScaleRatio() : 1.0)
            .build();
    }

    @Transactional
    public CarrierStickerResponse.Detail updateSticker(Long carrierId, Long stickerId, CarrierStickerRequest.UpdateSticker request, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        CarrierSticker sticker = carrierStickerRepository.findById(stickerId)
            .orElseThrow(() -> new RestApiException(CarrierErrorCode.STICKER_NOT_FOUND));

        sticker.update(request);

        return CarrierStickerResponse.Detail.from(sticker);
    }

    @Transactional
    public List<CarrierStickerResponse.Detail> updateStickerPositions(Long carrierId, List<CarrierStickerRequest.UpdateStickerBulk> request, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        List<Long> stickerIds = request.stream()
            .map(CarrierStickerRequest.UpdateStickerBulk::getStickerId)
            .toList();

        List<CarrierSticker> stickers = carrierStickerRepository.findAllByIdInAndCarrierId(stickerIds, carrierId);

        if (stickers.size() != stickerIds.size()) {
            throw new RestApiException(CarrierErrorCode.STICKER_NOT_IN_CARRIER);
        }

        Map<Long, CarrierStickerRequest.UpdateStickerBulk> requestMap = request.stream()
            .collect(Collectors.toMap(CarrierStickerRequest.UpdateStickerBulk::getStickerId, r -> r));

        stickers.forEach(sticker -> {
            CarrierStickerRequest.UpdateStickerBulk req = requestMap.get(sticker.getId());
            sticker.updateBulk(req);
        });

        return stickers.stream()
            .map(CarrierStickerResponse.Detail::from)
            .toList();
    }

    @Transactional
    public void removeSticker(Long carrierId, Long stickerId, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        CarrierSticker sticker = carrierStickerRepository.findById(stickerId)
            .orElseThrow(() -> new RestApiException(CarrierErrorCode.STICKER_NOT_FOUND));

        carrierStickerRepository.delete(sticker);
    }

    @Transactional
    public void removeStickers(Long carrierId, List<Long> stickerIds, Long userId) {
        carrierService.getCarrierByOwner(carrierId, userId);

        List<CarrierSticker> stickers = carrierStickerRepository.findAllById(stickerIds);

        if (stickers.size() != stickerIds.size()) {
            throw new RestApiException(CarrierErrorCode.STICKER_NOT_FOUND);
        }

        carrierStickerRepository.deleteAllInBatch(stickers);
    }
}