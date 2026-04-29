package com.example.toycontent.support.fixture;

import com.example.toycontent.app.carrier.domain.Carrier;
import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.common.enumuration.StickerType;

public class CarrierStickerFixture {

    private CarrierStickerFixture() {}

    public static CarrierSticker emoji(Long id, Carrier carrier, String content) {
        return CarrierSticker.builder()
                .id(id)
                .carrier(carrier)
                .stickerType(StickerType.EMOJI)
                .content(content)
                .positionX(0.5)
                .positionY(0.5)
                .zIndex(0)
                .rotation(0.0)
                .scaleRatio(1.0)
                .build();
    }

    public static CarrierSticker photoTag(Long id, Carrier carrier, String imageUrl) {
        return CarrierSticker.builder()
                .id(id)
                .carrier(carrier)
                .stickerType(StickerType.PHOTO_TAG)
                .imageUrl(imageUrl)
                .positionX(0.5)
                .positionY(0.5)
                .zIndex(0)
                .rotation(0.0)
                .scaleRatio(1.0)
                .build();
    }
}
