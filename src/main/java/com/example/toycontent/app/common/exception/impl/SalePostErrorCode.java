package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SalePostErrorCode implements ErrorCode {

  // 게시글 관련
  SALE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "판매 게시글을 찾을 수 없습니다."),
  SALE_POST_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 게시글입니다."),
  SALE_POST_ALREADY_SOLD_OUT(HttpStatus.BAD_REQUEST, "이미 품절된 게시글입니다."),

  // 판매 타입 관련
  INVALID_SALE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 판매 타입입니다."),
  SALE_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "판매 타입이 일치하지 않습니다."),
  INVALID_OPTION_FOR_SALE_TYPE(HttpStatus.BAD_REQUEST, "판매 타입에 맞는 옵션을 입력해주세요."),

  // 옵션 관련
  OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "판매 옵션을 찾을 수 없습니다."),
  OPTION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성화되지 않은 옵션입니다."),
  OPTION_ALREADY_INACTIVE(HttpStatus.BAD_REQUEST, "이미 비활성화된 옵션입니다."),

  // 재고 관련
  INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
  STOCK_SOLD_OUT(HttpStatus.BAD_REQUEST, "품절된 상품입니다."),
  INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "재고 수량이 올바르지 않습니다."),

  // 한입만 관련
  BITE_SIZE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "한입만 옵션을 찾을 수 없습니다."),
  BITE_SIZE_SOLD_OUT(HttpStatus.BAD_REQUEST, "한입만 수량이 모두 판매되었습니다."),

  // 일반 판매 관련
  NORMAL_SALE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "일반 판매 옵션을 찾을 수 없습니다."),
  NORMAL_SALE_OPTION_REQUIRED(HttpStatus.BAD_REQUEST, "일반 판매 옵션은 최소 1개 이상 필요합니다."),

  // 공동구매 관련
  GROUP_BUY_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "공동구매 옵션을 찾을 수 없습니다."),
  GROUP_BUY_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "모집 중인 공동구매가 아닙니다."),
  GROUP_BUY_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "공동구매 마감 시간이 지났습니다."),
  GROUP_BUY_TARGET_NOT_REACHED(HttpStatus.BAD_REQUEST, "공동구매 목표 인원을 달성하지 못했습니다."),
  GROUP_BUY_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 마감된 공동구매입니다."),
  GROUP_BUY_ALREADY_FAILED(HttpStatus.BAD_REQUEST, "실패한 공동구매입니다."),
  GROUP_BUY_FULL(HttpStatus.BAD_REQUEST, "공동구매 정원이 마감되었습니다."),
  INVALID_INVITE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 토큰입니다."),
  PRIVATE_GROUP_BUY_REQUIRE_TOKEN(HttpStatus.BAD_REQUEST, "비공개 공동구매는 초대 토큰이 필요합니다."),

  // 대리구매 관련
  PROXY_BUY_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "대리구매 옵션을 찾을 수 없습니다."),
  PROXY_BUY_FULL(HttpStatus.BAD_REQUEST, "대리구매 신청이 마감되었습니다."),
  PROXY_BUY_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "대리구매 신청을 찾을 수 없습니다."),
  PROXY_BUY_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 취소된 대리구매 신청입니다."),

  // 권한 관련
  NOT_SELLER(HttpStatus.FORBIDDEN, "판매자만 수정할 수 있습니다."),
  NOT_BUYER(HttpStatus.FORBIDDEN, "구매자만 접근할 수 있습니다."),
  UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

  // 이미지 관련
  IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 5개까지만 등록 가능합니다."),
  IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "최소 1개 이상의 이미지가 필요합니다."),

  // 비즈니스 로직 관련
  INVALID_PRICE(HttpStatus.BAD_REQUEST, "가격이 올바르지 않습니다."),
  INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량이 올바르지 않습니다."),
  INVALID_DEADLINE(HttpStatus.BAD_REQUEST, "마감 기한이 올바르지 않습니다."),
  DEADLINE_MUST_BE_FUTURE(HttpStatus.BAD_REQUEST, "마감 기한은 현재 시간 이후여야 합니다."),
  ;

  private final HttpStatus httpStatus;
  private final String message;
}
