package com.example.toycontent.app.oneMouth.service.impl;

import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.oneMouth.controller.dto.SalePostRequest.NormalSaleOptionDto;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.domain.option.NormalSaleOption;
import com.example.toycontent.app.oneMouth.service.SaleOptionAdder;
import org.springframework.stereotype.Component;

@Component
public class NormalSaleOptionAdder implements SaleOptionAdder<NormalSaleOptionDto> {

  @Override
  public void addOption(SalePost salePost, NormalSaleOptionDto dto) {
      NormalSaleOption option = NormalSaleOption.builder()
          .price(dto.getPrice())
          .totalStock(dto.getTotalStock())
          .originalPrice(dto.getOriginalPrice())
          .optionName(dto.getOptionName())
          .build();

      salePost.addNormalSaleOption(option);
  }

  @Override
  public SaleType getSaleType() {
    return SaleType.NORMAL;
  }
}
