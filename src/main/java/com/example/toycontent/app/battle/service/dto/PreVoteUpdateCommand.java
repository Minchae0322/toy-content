package com.example.toycontent.app.battle.service.dto;

import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import java.time.LocalDateTime;

public record PreVoteUpdateCommand(
    ItemAddPermissionType itemAddPermissionType,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime participationStartDate,
    VoteType voteType
) {}
