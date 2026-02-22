package com.example.toycontent.app.carrier.domain;


import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "tb_carrier_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_carrier_product",
                columnNames = {"carrier_id", "product_id"}),
        indexes = {
                @Index(name = "idx_carrier_item_carrier", columnList = "carrier_id")
        })
public class CarrierItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("캐리어 아이템 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    @Comment("캐리어")
    private Carrier carrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @Comment("연결된 제품")
    private Product product;

    @Column(name = "position_x", nullable = false)
    @Comment("X 위치 비율 (0.0 ~ 1.0)")
    private Double positionX;

    @Column(name = "position_y", nullable = false)
    @Comment("Y 위치 비율 (0.0 ~ 1.0)")
    private Double positionY;

    @Column(name = "z_index", nullable = false)
    @Builder.Default
    @Comment("레이어 순서 (높을수록 위에 표시)")
    private Integer zIndex = 0;

    // ===== 비즈니스 메서드 =====

    public void updatePosition(Double positionX, Double positionY, Integer zIndex) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.zIndex = zIndex;
    }
}