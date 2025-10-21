package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.common.enumuration.GroupBuyType;
import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class SalePostRequest {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "판매 게시글 생성 요청")
  public static class SalePostCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 500, message = "제목은 500자를 초과할 수 없습니다")
    @Schema(description = "게시글 제목", example = "맛있는 홈메이드 쿠키")
    private String title;

    @Size(max = 2000, message = "설명은 2000자를 초과할 수 없습니다")
    @Schema(description = "상세 설명", example = "직접 구운 수제 쿠키입니다")
    private String description;

    @NotNull(message = "판매 방식은 필수입니다")
    @Schema(description = "판매 방식", example = "BITE_SIZE",
        allowableValues = {"BITE_SIZE", "NORMAL", "GROUP_BUY", "PROXY_BUY"})
    private SaleType saleType;

    @Schema(description = "연결할 공식 제품 ID (선택)")
    private Long productId;

    @Size(max = 255, message = "거래 지역은 255자를 초과할 수 없습니다")
    @Schema(description = "거래 희망 지역", example = "서울시 강남구")
    private String tradeLocation;

    @NotNull(message = "대표이미지는 필수 입니다.")
    @Schema(description = "대표이미지")
    private AttachmentInfo thumbnailAttachmentInfo;

    @Schema(description = "이미지 URL 목록")
    @Size(max = 5, message = "이미지는 최대 5개까지 등록 가능합니다")
    private List<AttachmentInfo> attachmentFileInfos = new ArrayList<>();

    @Schema(description = "한입만 판매 옵션 (saleType이 BITE_SIZE인 경우 필수)")
    private OneMouthOptionDto biteSizeOption;

    @Schema(description = "일반 판매 옵션 목록 (saleType이 NORMAL인 경우 필수)")
    private List<NormalSaleOptionDto> normalSaleOptions;

    @Schema(description = "공동구매 옵션 (saleType이 GROUP_BUY인 경우 필수)")
    private GroupBuyOptionDto groupBuyOption;

    @Schema(description = "대리구매 옵션 (saleType이 PROXY_BUY인 경우 필수)")
    private ProxyBuyOptionDto proxyBuyOption;

    @AssertTrue(message = "판매 타입에 맞는 옵션을 입력해주세요")
    public boolean isValidOption() {
      return switch (saleType) {
        case ONEMOUTH -> biteSizeOption != null && normalSaleOptions == null
            && groupBuyOption == null && proxyBuyOption == null;
        case NORMAL -> normalSaleOptions != null && !normalSaleOptions.isEmpty()
            && biteSizeOption == null && groupBuyOption == null && proxyBuyOption == null;
        case GROUP_BUY -> groupBuyOption != null && biteSizeOption == null
            && normalSaleOptions == null && proxyBuyOption == null;
        case PROXY -> proxyBuyOption != null && biteSizeOption == null
            && normalSaleOptions == null && groupBuyOption == null;
      };
    }

    public Object getOptionDto() {
      return switch (saleType) {
        case ONEMOUTH -> biteSizeOption;
        case NORMAL -> normalSaleOptions;
        case GROUP_BUY -> groupBuyOption;
        case PROXY -> proxyBuyOption;
      };
    }
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "한입만 판매 옵션")
  public static class OneMouthOptionDto {

    @NotNull(message = "한입 단위 수량은 필수입니다")
    @Min(value = 1, message = "한입 단위 수량은 1 이상이어야 합니다")
    @Schema(description = "한입 단위 수량", example = "2")
    private Integer unitQuantity;

    @NotNull(message = "한입 가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "한입 가격", example = "3000")
    private Long unitPrice;

    @NotNull(message = "총 한입 판매 수량은 필수입니다")
    @Min(value = 1, message = "총 수량은 1 이상이어야 합니다")
    @Schema(description = "총 한입 판매 수량", example = "10")
    private Integer totalBiteCount;

    @Min(value = 0, message = "원가는 0 이상이어야 합니다")
    @Schema(description = "원가 (할인율 계산용)", example = "5000")
    private Long originalPrice;

    @Size(max = 100, message = "옵션명은 100자를 초과할 수 없습니다")
    @Schema(description = "옵션명", example = "2개 한입 세트")
    private String optionName;
  }


  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "일반 판매 옵션")
  public static class NormalSaleOptionDto {

    @NotNull(message = "가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "판매 가격", example = "15000")
    private Long price;

    @NotNull(message = "재고는 필수입니다")
    @Min(value = 0, message = "재고는 0 이상이어야 합니다")
    @Schema(description = "총 재고", example = "50")
    private Integer totalStock;

    @Min(value = 0, message = "원가는 0 이상이어야 합니다")
    @Schema(description = "원가", example = "20000")
    private Long originalPrice;

    @NotBlank(message = "옵션명은 필수입니다")
    @Size(max = 100, message = "옵션명은 100자를 초과할 수 없습니다")
    @Schema(description = "옵션명", example = "5개입", required = true)
    private String optionName;
  }


  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "공동구매 옵션")
  public static class GroupBuyOptionDto {

    @NotNull(message = "공동구매 타입은 필수입니다")
    @Schema(description = "공동구매 타입", example = "OPEN",
        allowableValues = {"OPEN", "PRIVATE"})
    private GroupBuyType groupBuyType;

    @NotNull(message = "목표 인원은 필수입니다")
    @Min(value = 2, message = "목표 인원은 2명 이상이어야 합니다")
    @Schema(description = "목표 인원", example = "20")
    private Integer targetCount;

    @NotNull(message = "할인된 가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "할인된 가격", example = "10000")
    private Long discountedPrice;

    @Min(value = 0, message = "정상 가격은 0 이상이어야 합니다")
    @Schema(description = "정상 가격 (목표 미달 시)", example = "15000")
    private Long normalPrice;

    @Min(value = 0, message = "할인율은 0 이상이어야 합니다")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다")
    @Schema(description = "할인율 (%)", example = "33")
    private Integer discountRate;

    @NotNull(message = "마감 기한은 필수입니다")
    @Future(message = "마감 기한은 미래 시간이어야 합니다")
    @Schema(description = "마감 기한", example = "2025-12-31T23:59:59")
    private LocalDateTime deadline;

    @Size(max = 100, message = "옵션명은 100자를 초과할 수 없습니다")
    @Schema(description = "옵션명", example = "20명 공동구매")
    private String optionName;
  }


  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "대리구매 옵션")
  public static class ProxyBuyOptionDto {

    @NotNull(message = "예상 제품 가격은 필수입니다")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @Schema(description = "예상 제품 가격", example = "50000")
    private Long estimatedProductPrice;

    @NotNull(message = "수수료는 필수입니다")
    @Min(value = 0, message = "수수료는 0 이상이어야 합니다")
    @Schema(description = "대리구매 수수료", example = "5000")
    private Long serviceFee;

    @NotBlank(message = "구매 장소는 필수입니다")
    @Size(max = 500, message = "구매 장소는 500자를 초과할 수 없습니다")
    @Schema(description = "구매 예정 장소", example = "명동 매장")
    private String purchaseLocation;

    @Future(message = "예상 구매 일시는 미래 시간이어야 합니다")
    @Schema(description = "예상 구매 일시", example = "2025-11-01T14:00:00")
    private LocalDateTime expectedPurchaseDate;

    @NotNull(message = "최대 수량은 필수입니다")
    @Min(value = 1, message = "최대 수량은 1 이상이어야 합니다")
    @Schema(description = "최대 대리구매 수량", example = "5")
    private Integer maxQuantity;

    @Size(max = 100, message = "옵션명은 100자를 초과할 수 없습니다")
    @Schema(description = "옵션명", example = "명동 대리구매")
    private String optionName;
  }
}