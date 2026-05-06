package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.common.enumuration.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class FeedReportRequest {

    @Schema(description = "신고 사유", example = "SPAM")
    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Schema(description = "상세 내용 (선택)", example = "광고성 댓글이 반복됩니다.")
    @Size(max = 500, message = "상세 내용은 500자 이하여야 합니다.")
    private String detail;
}
