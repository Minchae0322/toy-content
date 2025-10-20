package com.example.toycontent.app.oneMouth.service.impl;

import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.domain.option.BiteSizeOption;
import com.example.toycontent.app.oneMouth.service.SaleOptionAdder;
import org.springframework.stereotype.Component;

@Component
public class OneMouthOptionAdder implements SaleOptionAdder<BiteSizeOption> {


  @Override
  public void addOption(SalePost salePost, BiteSizeOption optionDto) {
    BiteSizeOption option = BiteSizeOption.builder()
        .unitQuantity(optionDto.getUnitQuantity())
        .unitPrice(optionDto.getUnitPrice())
        .totalBiteCount(optionDto.getTotalBiteCount())
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
