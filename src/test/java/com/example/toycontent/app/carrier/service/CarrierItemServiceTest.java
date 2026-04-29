package com.example.toycontent.app.carrier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.carrier.controller.dto.CarrierStickerRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierStickerResponse;
import com.example.toycontent.app.carrier.domain.Carrier;
import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.carrier.repository.CarrierItemRepository;
import com.example.toycontent.app.carrier.repository.CarrierStickerRepository;
import com.example.toycontent.app.common.enumuration.StickerType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CarrierErrorCode;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.support.fixture.CarrierFixture;
import com.example.toycontent.support.fixture.CarrierStickerFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarrierItemService")
class CarrierItemServiceTest {

    private static final Long CARRIER_ID = CarrierFixture.DEFAULT_CARRIER_ID;
    private static final Long USER_ID = CarrierFixture.DEFAULT_USER_ID;

    @Mock private CarrierService carrierService;
    @Mock private CarrierItemRepository carrierItemRepository;
    @Mock private CarrierStickerRepository carrierStickerRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private CarrierItemService carrierItemService;

    @Nested
    @DisplayName("bulkSaveStickers - 스티커 reconcile 저장")
    class BulkSaveStickers {

        @Test
        @DisplayName("빈 요청은 캐리어의 모든 스티커를 삭제한다")
        void 빈_요청은_전체_삭제() {
            Carrier carrier = CarrierFixture.defaultCarrier();
            CarrierSticker s1 = CarrierStickerFixture.emoji(1L, carrier, "안녕");
            CarrierSticker s2 = CarrierStickerFixture.emoji(2L, carrier, "잘가");
            given(carrierService.getCarrierByOwner(CARRIER_ID, USER_ID)).willReturn(carrier);
            given(carrierStickerRepository.findAllByCarrierId(CARRIER_ID))
                    .willReturn(List.of(s1, s2));

            List<CarrierStickerResponse.Detail> result = carrierItemService.bulkSaveStickers(
                    CARRIER_ID, bulkSave(List.of()), USER_ID);

            assertThat(result).isEmpty();
            then(carrierStickerRepository).should().deleteAllInBatch(anyList());
            then(carrierStickerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("신규 스티커만 있는 요청은 모두 신규 저장하고 삭제는 일어나지 않는다")
        void 신규만_저장() {
            Carrier carrier = CarrierFixture.defaultCarrier();
            given(carrierService.getCarrierByOwner(CARRIER_ID, USER_ID)).willReturn(carrier);
            given(carrierStickerRepository.findAllByCarrierId(CARRIER_ID)).willReturn(List.of());
            given(carrierStickerRepository.save(any(CarrierSticker.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            CarrierStickerRequest.BulkSave request = bulkSave(List.of(
                    newSticker(StickerType.EMOJI, "신규1", 0.1, 0.1),
                    newSticker(StickerType.EMOJI, "신규2", 0.2, 0.2)
            ));

            List<CarrierStickerResponse.Detail> result =
                    carrierItemService.bulkSaveStickers(CARRIER_ID, request, USER_ID);

            assertThat(result).hasSize(2);
            then(carrierStickerRepository).should(never()).deleteAllInBatch(anyList());
        }

        @Test
        @DisplayName("기존 수정 + 누락된 기존 삭제 + 신규 저장이 한 번에 처리된다 (reconcile)")
        void reconcile() {
            Carrier carrier = CarrierFixture.defaultCarrier();
            CarrierSticker existing1 = CarrierStickerFixture.emoji(1L, carrier, "old1");
            CarrierSticker existing2 = CarrierStickerFixture.emoji(2L, carrier, "old2");
            given(carrierService.getCarrierByOwner(CARRIER_ID, USER_ID)).willReturn(carrier);
            given(carrierStickerRepository.findAllByCarrierId(CARRIER_ID))
                    .willReturn(List.of(existing1, existing2));
            given(carrierStickerRepository.save(any(CarrierSticker.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            CarrierStickerRequest.BulkSave request = bulkSave(List.of(
                    updateSticker(1L, StickerType.EMOJI, "new1", 0.9, 0.9),
                    newSticker(StickerType.EMOJI, "추가", 0.5, 0.5)
            ));

            List<CarrierStickerResponse.Detail> result =
                    carrierItemService.bulkSaveStickers(CARRIER_ID, request, USER_ID);

            assertThat(result).hasSize(2);
            assertThat(existing1.getContent()).isEqualTo("new1");
            then(carrierStickerRepository).should().deleteAllInBatch(List.of(existing2));
        }

        @Test
        @DisplayName("PHOTO_TAG가 2개 이상이면 MAX_PHOTO_TAG_EXCEEDED 예외를 던진다")
        void photoTag_여러개_거부() {
            given(carrierService.getCarrierByOwner(CARRIER_ID, USER_ID))
                    .willReturn(CarrierFixture.defaultCarrier());

            CarrierStickerRequest.BulkSave request = bulkSave(List.of(
                    newSticker(StickerType.PHOTO_TAG, null, 0.1, 0.1),
                    newSticker(StickerType.PHOTO_TAG, null, 0.2, 0.2)
            ));

            assertThatThrownBy(() -> carrierItemService.bulkSaveStickers(CARRIER_ID, request, USER_ID))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(CarrierErrorCode.MAX_PHOTO_TAG_EXCEEDED.getMessage());

            then(carrierStickerRepository).should(never()).save(any());
            then(carrierStickerRepository).should(never()).deleteAllInBatch(anyList());
        }

        @Test
        @DisplayName("캐리어에 없는 stickerId가 포함되면 STICKER_OUT_OF_SYNC 예외를 던진다")
        void 모르는_id_거부() {
            Carrier carrier = CarrierFixture.defaultCarrier();
            given(carrierService.getCarrierByOwner(CARRIER_ID, USER_ID)).willReturn(carrier);
            given(carrierStickerRepository.findAllByCarrierId(CARRIER_ID)).willReturn(List.of(
                    CarrierStickerFixture.emoji(1L, carrier, "있음")
            ));

            CarrierStickerRequest.BulkSave request = bulkSave(List.of(
                    updateSticker(999L, StickerType.EMOJI, "없는ID", 0.5, 0.5)
            ));

            assertThatThrownBy(() -> carrierItemService.bulkSaveStickers(CARRIER_ID, request, USER_ID))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(CarrierErrorCode.STICKER_OUT_OF_SYNC.getMessage());

            then(carrierStickerRepository).should(never()).save(any());
            then(carrierStickerRepository).should(never()).deleteAllInBatch(anyList());
        }

        @Test
        @DisplayName("같은 stickerId가 중복 요청돼도 예외 없이 마지막 값이 적용된다")
        void 중복_id_허용() {
            Carrier carrier = CarrierFixture.defaultCarrier();
            CarrierSticker existing = CarrierStickerFixture.emoji(1L, carrier, "old");
            given(carrierService.getCarrierByOwner(CARRIER_ID, USER_ID)).willReturn(carrier);
            given(carrierStickerRepository.findAllByCarrierId(CARRIER_ID))
                    .willReturn(List.of(existing));

            CarrierStickerRequest.BulkSave request = bulkSave(List.of(
                    updateSticker(1L, StickerType.EMOJI, "first", 0.1, 0.1),
                    updateSticker(1L, StickerType.EMOJI, "last", 0.9, 0.9)
            ));

            carrierItemService.bulkSaveStickers(CARRIER_ID, request, USER_ID);

            assertThat(existing.getContent()).isEqualTo("last");
            then(carrierStickerRepository).should(never()).save(any());
            then(carrierStickerRepository).should(never()).deleteAllInBatch(anyList());
        }
    }

    private CarrierStickerRequest.BulkSave bulkSave(
            List<CarrierStickerRequest.BulkSave.StickerUpsert> items) {
        return new CarrierStickerRequest.BulkSave(items);
    }

    private CarrierStickerRequest.BulkSave.StickerUpsert newSticker(
            StickerType type, String content, double x, double y) {
        return new CarrierStickerRequest.BulkSave.StickerUpsert(
                null, type, content, null, x, y, 0, 0.0, 1.0);
    }

    private CarrierStickerRequest.BulkSave.StickerUpsert updateSticker(
            Long id, StickerType type, String content, double x, double y) {
        return new CarrierStickerRequest.BulkSave.StickerUpsert(
                id, type, content, null, x, y, 0, 0.0, 1.0);
    }
}
