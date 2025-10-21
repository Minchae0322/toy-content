package com.example.toycontent.app.oneMouth.service.impl;

import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.oneMouth.controller.dto.SalePostRequest.OneMouthOptionDto;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.domain.option.OneMouthOption;
import com.example.toycontent.app.oneMouth.service.SaleOptionAdder;
import org.springframework.stereotype.Component;

@Component
public class OneMouthOptionAdder implements SaleOptionAdder<OneMouthOptionDto> {


  @Override
  public void addOption(SalePost salePost, OneMouthOptionDto optionDto) {
    OneMouthOption option = OneMouthOption.builder()
        .unitQuantity(optionDto.getUnitQuantity())
        .unitPrice(optionDto.getUnitPrice())
        .originalPrice(optionDto.getOriginalPrice())
        .optionName(optionDto.getOptionName())
        .build();

    salePost.addBiteSizeOption(option);
  }

  @Override
  public SaleType getSaleType() {
    return SaleType.ONEMOUTH;
  }
}
