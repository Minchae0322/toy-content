package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {

  FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "FEED_001", "피드를 찾을 수 없습니다."),
  CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "FEED_002", "카테고리를 찾을 수 없습니다."),
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "FEED_003", "상품을 찾을 수 없습니다."),
  UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "FEED_004", "피드를 수정할 권한이 없습니다."),
  INVALID_IMAGE_COUNT(HttpStatus.BAD_REQUEST, "FEED_005", "이미지는 최대 10개까지 업로드 가능합니다."),
  INVALID_HASHTAG_COUNT(HttpStatus.BAD_REQUEST, "FEED_006", "해시태그는 최대 20개까지 등록 가능합니다."),
  INVALID_REVIEW_LENGTH(HttpStatus.BAD_REQUEST, "FEED_007", "리뷰는 1000자 이하로 작성해주세요.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
