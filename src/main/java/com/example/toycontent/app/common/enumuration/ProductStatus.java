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

    FOR_SALE("FOR_SALE", "판매중"),
    SOLD_OUT("SOLD_OUT", "품절"),
    RESERVED("RESERVED", "예약중"),
    DISCONTINUED("DISCONTINUED", "판매중단");

    private String title;
    private String description;
}
