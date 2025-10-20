package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.oneMouth.domain.SalePost;

public interface SaleOptionAdder<T> {
  void addOption(SalePost salePost, T optionDto);
  SaleType getSaleType();
}
