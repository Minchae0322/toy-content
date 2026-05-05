package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {

  FEED_NOT_FOUND(HttpStatus.NOT_FOUND,  "피드를 찾을 수 없습니다."),
  CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,  "카테고리를 찾을 수 없습니다."),
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND,  "상품을 찾을 수 없습니다."),
  UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN,  "피드를 수정할 권한이 없습니다."),
  INVALID_IMAGE_COUNT(HttpStatus.BAD_REQUEST,  "이미지는 최대 10개까지 업로드 가능합니다."),
  INVALID_HASHTAG_COUNT(HttpStatus.BAD_REQUEST,  "해시태그는 최대 20개까지 등록 가능합니다."),
  CREATOR_NOT_MATCH(HttpStatus.NOT_FOUND, "작성자만 수정 또는 삭제할 수 있습니다."),
  INVALID_REVIEW_LENGTH(HttpStatus.BAD_REQUEST,  "리뷰는 1000자 이하로 작성해주세요."),
  REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "리액션을 찾을 수 없습니다."),


  FEED_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,  "피드 댓글을 찾을 수 없습니다."),
  REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "답글에는 다시 답글을 달 수 없습니다."),
  PARENT_COMMENT_DELETED(HttpStatus.BAD_REQUEST, "삭제된 댓글에는 답글을 달 수 없습니다."),

  FEED_REPORT_DUPLICATED(HttpStatus.CONFLICT, "이미 신고한 피드입니다."),
  FEED_REPORT_SELF(HttpStatus.BAD_REQUEST, "본인의 피드는 신고할 수 없습니다."),
  ;

  private final HttpStatus httpStatus;
  private final String message;
}
