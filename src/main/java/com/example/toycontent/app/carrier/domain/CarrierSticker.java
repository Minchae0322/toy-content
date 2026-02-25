package com.example.toycontent.app.carrier.domain;

import com.example.toycontent.app.carrier.controller.dto.CarrierStickerRequest;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.StickerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tb_carrier_sticker",
        indexes = {
                @Index(name = "idx_carrier_sticker_carrier", columnList = "carrier_id")
        })
public class CarrierSticker extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("캐리어 스티커 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    @Comment("소속 캐리어")
    private Carrier carrier;

    @Enumerated(EnumType.STRING)
    @Column(name = "sticker_type", nullable = false, length = 20)
    @Comment("스티커 타입")
    private StickerType stickerType;

    @Column(name = "content", length = 100)
    @Comment("이모지 또는 프리셋 코드")
    private String content;

    @Column(name = "image_url", length = 500)
    @Comment("이미지 URL (PHOTO_TAG 등에서 사용)")
    private String imageUrl;

    @Column(name = "position_x", nullable = false)
    @Comment("X 위치 비율 (0.0 ~ 1.0)")
    private Double positionX;

    @Column(name = "position_y", nullable = false)
    @Comment("Y 위치 비율 (0.0 ~ 1.0)")
    private Double positionY;

    @Column(name = "z_index", nullable = false)
    @Builder.Default
    @Comment("레이어 순서")
    private Integer zIndex = 0;

    @Column(name = "rotation")
    @Builder.Default
    @Comment("회전 각도 (0 ~ 360)")
    private Double rotation = 0.0;

    @Column(name = "scale_ratio")
    @Builder.Default
    @Comment("크기 비율 (1.0 = 기본)")
    private Double scaleRatio = 1.0;

    /**
     * 위치 및 변환 정보 업데이트 (공통)
     * - positionX, positionY는 필수값으로 덮어씀
     * - zIndex, rotation, scaleRatio는 null이면 기존값 유지
     */
    private void updatePositionAndTransform(Double positionX, Double positionY,
        Integer zIndex, Double rotation, Double scaleRatio) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.zIndex = zIndex != null ? zIndex : this.zIndex;
        this.rotation = rotation != null ? rotation : this.rotation;
        this.scaleRatio = scaleRatio != null ? scaleRatio : this.scaleRatio;
    }

    /**
     * PHOTO_TAG 스티커 upsert 시 업데이트
     * - 위치/변환 + 이미지 정보 갱신
     */
    public void updatePhotoTag(CarrierStickerRequest.AddSticker request) {
        updatePositionAndTransform(request.getPositionX(), request.getPositionY(),
            request.getZIndex(), request.getRotation(), request.getScaleRatio());
        this.imageUrl = request.getImageUrl() != null ? request.getImageUrl() : this.imageUrl;
    }

    /**
     * 단건 스티커 수정
     * - 위치/변환 + 콘텐츠/이미지 정보 갱신
     */
    public void update(CarrierStickerRequest.UpdateSticker request) {
        updatePositionAndTransform(request.getPositionX(), request.getPositionY(),
            request.getZIndex(), request.getRotation(), request.getScaleRatio());
        this.content = request.getContent() != null ? request.getContent() : this.content;
        this.imageUrl = request.getImageUrl() != null ? request.getImageUrl() : this.imageUrl;
    }

    /**
     * 벌크 스티커 수정
     * - 위치/변환 정보만 갱신 (콘텐츠/이미지 변경 불가)
     */
    public void updateBulk(CarrierStickerRequest.UpdateStickerBulk request) {
        updatePositionAndTransform(request.getPositionX(), request.getPositionY(),
            request.getZIndex(), request.getRotation(), request.getScaleRatio());
    }
}
