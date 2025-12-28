package com.example.toycontent.app.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커서 기반 페이징 응답")
public class CursorResponse<T> {
  @Schema(description = "조회 데이터 목록")
  private List<T> content;

  @Schema(description = "다음 페이지 커서 (다음 조회 시 cursor 파라미터로 사용)", example = "95")
  private Long nextCursor;

  @Schema(description = "다음 페이지 존재 여부", example = "true")
  private boolean hasNext;

  @Schema(description = "조회된 데이터 수", example = "20")
  private Integer size;

  public static <T> CursorResponse<T> of(List<T> content, Integer requestSize, Function<T, Long> cursorExtractor) {
    boolean hasNext = content.size() > requestSize;

    List<T> actualContent = hasNext
        ? content.subList(0, requestSize)
        : content;

    Long nextCursor = hasNext
        ? cursorExtractor.apply(actualContent.get(actualContent.size() - 1))
        : null;

    return CursorResponse.<T>builder()
        .content(actualContent)
        .nextCursor(nextCursor)
        .hasNext(hasNext)
        .size(actualContent.size())
        .build();
  }
}
