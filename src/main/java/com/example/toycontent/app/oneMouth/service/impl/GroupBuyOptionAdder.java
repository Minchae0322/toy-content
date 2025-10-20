package com.example.toycontent.app.oneMouth.service.impl;

import com.example.toycontent.app.common.enumuration.GroupBuyType;
import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.oneMouth.controller.dto.SalePostRequest.GroupBuyOptionDto;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.domain.option.GroupBuyOption;
import com.example.toycontent.app.oneMouth.service.SaleOptionAdder;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GroupBuyOptionAdder implements SaleOptionAdder<GroupBuyOptionDto> {

  @Override
  public void addOption(SalePost salePost, GroupBuyOptionDto dto) {
    String inviteToken = null;
    if (dto.getGroupBuyType() == GroupBuyType.PRIVATE) {
      inviteToken = UUID.randomUUID().toString();
    }

    GroupBuyOption option = GroupBuyOption.builder()
        .groupBuyType(dto.getGroupBuyType())
        .targetCount(dto.getTargetCount())
        .discountedPrice(dto.getDiscountedPrice())
        .normalPrice(dto.getNormalPrice())
        .discountRate(dto.getDiscountRate())
        .deadline(dto.getDeadline())
        .inviteToken(inviteToken)
        .optionName(dto.getOptionName())
        .build();

    salePost.addGroupBuyOption(option);
  }

  @Override
  public SaleType getSaleType() {
    return SaleType.GROUP_BUY;
  }
}
