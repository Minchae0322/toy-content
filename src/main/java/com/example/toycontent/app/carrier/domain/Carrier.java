package com.example.toycontent.app.carrier.domain;

import com.example.toycontent.app.carrier.repository.CarrierStickerRepository;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.CarrierSkinType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tb_carrier",
        indexes = {
                @Index(name = "idx_carrier_user", columnList = "user_id")
        })
public class Carrier extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("캐리어 ID")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("사용자 ID (User 서비스)")
    private Long userId;

    @Column(name = "skin_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Comment("캐리어 스킨 타입")
    private CarrierSkinType skinType = CarrierSkinType.DEFAULT;

    @Column(name = "skin_color", length = 7)
    @Comment("캐리어 스킨 색상 (#HEX)")
    private String skinColor;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    @Comment("기본 캐리어 여부")
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @Comment("캐리어 아이템 목록")
    private List<CarrierItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @Comment("스티커 목록")
    private List<CarrierSticker> stickers = new ArrayList<>();


    public void updateSkin(CarrierSkinType skinType, String skinColor) {
        this.skinType = skinType;
        this.skinColor = skinColor;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
