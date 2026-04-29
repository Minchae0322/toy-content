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
     * 스티커 일괄 저장 (reconcile)
     * - 요청의 stickers 목록이 캐리어의 최종 상태가 됨
     * - stickerId 있음 → 수정, 없음 → 신규 생성
     * - 기존에 있는데 요청에 없는 스티커는 삭제
     */
    @Transactional
    public List<CarrierStickerResponse.Detail> bulkSaveStickers(
            Long carrierId, CarrierStickerRequest.BulkSave request, Long userId) {

        Carrier carrier = carrierService.getCarrierByOwner(carrierId, userId);
        List<CarrierStickerRequest.BulkSave.StickerUpsert> items = request.getStickers();

        validatePhotoTagCount(items);

        Map<Long, CarrierSticker> existingMap = loadStickersByCarrier(carrierId);
        List<Long> updateIds = validateAndExtractUpdateIds(items, existingMap);

        deleteOrphans(existingMap, updateIds);

        return items.stream()
                .map(item -> upsertSticker(item, existingMap, carrier))
                .map(CarrierStickerResponse.Detail::from)
                .toList();
    }

    private void validatePhotoTagCount(List<CarrierStickerRequest.BulkSave.StickerUpsert> items) {
        long photoTagCount = items.stream()
                .filter(i -> i.getStickerType() == StickerType.PHOTO_TAG)
                .count();

        if (photoTagCount > 1) {
            throw new RestApiException(CarrierErrorCode.MAX_PHOTO_TAG_EXCEEDED);
        }
    }

    private Map<Long, CarrierSticker> loadStickersByCarrier(Long carrierId) {
        return carrierStickerRepository.findAllByCarrierId(carrierId).stream()
                .collect(Collectors.toMap(CarrierSticker::getId, s -> s));
    }

    private List<Long> validateAndExtractUpdateIds(
            List<CarrierStickerRequest.BulkSave.StickerUpsert> items,
            Map<Long, CarrierSticker> existingMap) {

        List<Long> updateIds = items.stream()
                .map(CarrierStickerRequest.BulkSave.StickerUpsert::getStickerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!existingMap.keySet().containsAll(updateIds)) {
            throw new RestApiException(CarrierErrorCode.STICKER_OUT_OF_SYNC);
        }

        return updateIds;
    }

    private void deleteOrphans(Map<Long, CarrierSticker> existingMap, List<Long> keepIds) {
        List<CarrierSticker> toDelete = existingMap.values().stream()
                .filter(s -> !keepIds.contains(s.getId()))
                .toList();

        if (!toDelete.isEmpty()) {
            carrierStickerRepository.deleteAllInBatch(toDelete);
        }
    }

    private CarrierSticker upsertSticker(CarrierStickerRequest.BulkSave.StickerUpsert item,
        Map<Long, CarrierSticker> existingMap, Carrier carrier) {

        if (item.getStickerId() != null) {
            CarrierSticker sticker = existingMap.get(item.getStickerId());
            sticker.updateFromBulkSave(item);
            return sticker;
        }

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
}