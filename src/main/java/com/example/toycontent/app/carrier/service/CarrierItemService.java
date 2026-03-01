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
import java.util.Objects;
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

    /**
     * 스티커 일괄 저장 (bulk upsert)
     * - stickerId가 null이면 신규 생성, 있으면 기존 수정
     * - PHOTO_TAG 타입은 캐리어당 1개만 유지 (자동 upsert)
     */
    @Transactional
    public List<CarrierStickerResponse.Detail> bulkSaveStickers(
            Long carrierId, CarrierStickerRequest.BulkSave request, Long userId) {

        Carrier carrier = carrierService.getCarrierByOwner(carrierId, userId);
        List<CarrierStickerRequest.BulkSave.StickerUpsert> items = request.getStickers();

        // 수정 대상 조회 및 검증
        Map<Long, CarrierSticker> existingMap = findExistingStickers(items, carrierId);

        // 검증
        validateUpdateIds(items, existingMap);
        validateNewStickerCount(items, carrierId);

        // upsert 처리
        List<CarrierSticker> result = items.stream()
                .map(item -> upsertSticker(item, existingMap, carrier, carrierId))
                .toList();

        return result.stream()
                .map(CarrierStickerResponse.Detail::from)
                .toList();
    }

    /**
     * 수정 대상 스티커 일괄 조회
     */
    private Map<Long, CarrierSticker> findExistingStickers(
            List<CarrierStickerRequest.BulkSave.StickerUpsert> items, Long carrierId) {

        List<Long> updateIds = items.stream()
                .map(CarrierStickerRequest.BulkSave.StickerUpsert::getStickerId)
                .filter(Objects::nonNull)
                .toList();

        if (updateIds.isEmpty()) {
            return Map.of();
        }

        return carrierStickerRepository.findAllByIdInAndCarrierId(updateIds, carrierId)
                .stream()
                .collect(Collectors.toMap(CarrierSticker::getId, s -> s));
    }

    /**
     * 수정 요청한 stickerId가 실제 캐리어에 존재하는지 검증
     */
    private void validateUpdateIds(
            List<CarrierStickerRequest.BulkSave.StickerUpsert> items,
            Map<Long, CarrierSticker> existingMap) {

        List<Long> updateIds = items.stream()
                .map(CarrierStickerRequest.BulkSave.StickerUpsert::getStickerId)
                .filter(Objects::nonNull)
                .toList();

        if (updateIds.size() != existingMap.size()) {
            throw new RestApiException(CarrierErrorCode.STICKER_NOT_IN_CARRIER);
        }
    }

    /**
     * 신규 스티커 개수 제한 검증
     * - 기존 스티커 + 신규 요청이 MAX_STICKER_COUNT 초과하면 예외
     */
    private void validateNewStickerCount(
            List<CarrierStickerRequest.BulkSave.StickerUpsert> items, Long carrierId) {

        long newCount = items.stream()
                .filter(i -> i.getStickerId() == null)
                .count();

        if (newCount == 0) return;

        long currentCount = carrierStickerRepository.countByCarrierId(carrierId);
        if (currentCount + newCount > MAX_STICKER_COUNT) {
            throw new RestApiException(CarrierErrorCode.MAX_STICKER_EXCEEDED);
        }
    }

    private CarrierSticker upsertSticker(
            CarrierStickerRequest.BulkSave.StickerUpsert item,
            Map<Long, CarrierSticker> existingMap,
            Carrier carrier, Long carrierId) {

        // 기존 수정 (PHOTO_TAG 포함 - 사진 추가/삭제/위치 변경)
        if (item.getStickerId() != null) {
            CarrierSticker sticker = existingMap.get(item.getStickerId());
            sticker.updateFromBulkSave(item);
            return sticker;
        }

        // PHOTO_TAG 신규 생성 (캐리어당 1개 보장 - 이미 있으면 무시)
        if (item.getStickerType() == StickerType.PHOTO_TAG) {
            return carrierStickerRepository
                    .findByCarrierIdAndStickerType(carrierId, StickerType.PHOTO_TAG)
                    .orElseGet(() -> carrierStickerRepository.save(createStickerFromBulk(carrier, item)));
        }

        // 일반 신규 생성
        return carrierStickerRepository.save(createStickerFromBulk(carrier, item));
    }

    private CarrierSticker createStickerFromBulk(Carrier carrier, CarrierStickerRequest.BulkSave.StickerUpsert item) {
        return CarrierSticker.builder()
                .carrier(carrier)
                .stickerType(item.getStickerType())
                .content(item.getContent())
                .imageUrl(item.getImageUrl())
                .positionX(item.getPositionX())
                .positionY(item.getPositionY())
                .zIndex(item.getZIndex() != null ? item.getZIndex() : 0)
                .rotation(item.getRotation() != null ? item.getRotation() : 0.0)
                .scaleRatio(item.getScaleRatio() != null ? item.getScaleRatio() : 1.0)
                .build();
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