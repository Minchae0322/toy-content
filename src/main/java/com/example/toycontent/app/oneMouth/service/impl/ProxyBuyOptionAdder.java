package com.example.toycontent.app.oneMouth.service.impl;

import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.oneMouth.controller.dto.SalePostRequest.ProxyBuyOptionDto;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.domain.option.ProxyBuyOption;
import com.example.toycontent.app.oneMouth.service.SaleOptionAdder;
import org.springframework.stereotype.Component;

@Component
public class ProxyBuyOptionAdder implements SaleOptionAdder<ProxyBuyOptionDto> {

  @Override
  public void addOption(SalePost salePost, ProxyBuyOptionDto dto) {
    ProxyBuyOption option = ProxyBuyOption.builder()
        .estimatedProductPrice(dto.getEstimatedProductPrice())
        .serviceFee(dto.getServiceFee())
        .purchaseLocation(dto.getPurchaseLocation())
        .expectedPurchaseDate(dto.getExpectedPurchaseDate())
        .maxQuantity(dto.getMaxQuantity())
        .optionName(dto.getOptionName())
        .build();

    salePost.addProxyBuyOption(option);
  }

  @Override
  public SaleType getSaleType() {
    return SaleType.PROXY;
  }
}
