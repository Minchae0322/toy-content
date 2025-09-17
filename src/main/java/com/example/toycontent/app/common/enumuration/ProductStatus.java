package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ProductStatus {


    APPROVED("APPROVED", "승인"),
    PENDING("PENDING", "대기중"),
    REJECTED("REJECTED", "거절됨"),
    ;

    private String title;
    private String description;

    public static ProductStatus getProductStatus(String title) {
        return values()[Integer.parseInt(title.toUpperCase())];
    }

}
