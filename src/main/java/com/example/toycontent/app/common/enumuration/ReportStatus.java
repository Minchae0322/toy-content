package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReportStatus {

    PENDING("PENDING", "접수"),
    REVIEWED("REVIEWED", "검토중"),
    RESOLVED("RESOLVED", "처리됨"),
    REJECTED("REJECTED", "반려"),
    ;

    private String title;
    private String description;
}
