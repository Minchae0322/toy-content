package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.SalePostErrorCode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SaleOptionAdderFactory {

  private final List<SaleOptionAdder<?>> adders;

  public SaleOptionAdderFactory(List<SaleOptionAdder<?>> adders) {
    this.adders = adders;
  }

  @SuppressWarnings("unchecked")
  public <T> SaleOptionAdder<T> getAdder(SaleType saleType) {
    return (SaleOptionAdder<T>) adders.stream()
        .filter(adder -> adder.getSaleType() == saleType)
        .findFirst()
        .orElseThrow(() -> new RestApiException(SalePostErrorCode.INVALID_SALE_TYPE));
  }
}
