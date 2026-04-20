package com.example.toycontent.app.reward.mission.repository.querydsl.impl;

import static com.example.toycontent.app.reward.mission.domain.QDailyMission.dailyMission;
import static com.example.toycontent.app.reward.mission.domain.QUserDailyMissionAssignment.userDailyMissionAssignment;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.DailyMissionInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.MissionAssignmentInfo;
import com.example.toycontent.app.reward.mission.repository.querydsl.UserDailyMissionAssignmentRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserDailyMissionAssignmentRepositoryCustomImpl
    implements UserDailyMissionAssignmentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<MissionAssignmentInfo> findAssignmentsWithMissionByUserIdAndDate(Long userId,
      LocalDate date) {
    return queryFactory
        .select(Projections.fields(MissionAssignmentInfo.class,
            userDailyMissionAssignment.id,
            Projections.fields(DailyMissionInfo.class,
                dailyMission.id,
                dailyMission.code,
                dailyMission.title,
                dailyMission.description,
                dailyMission.difficulty,
                dailyMission.targetCount,
                dailyMission.rewardExp,
                dailyMission.grantsGachaTicket
            ).as("mission"),
            userDailyMissionAssignment.assignedDate,
            userDailyMissionAssignment.currentCount,
            userDailyMissionAssignment.targetCount,
            userDailyMissionAssignment.status,
            userDailyMissionAssignment.completedAt,
            userDailyMissionAssignment.claimedAt
        ))
        .from(userDailyMissionAssignment)
        .join(userDailyMissionAssignment.mission, dailyMission)
        .where(buildWhereClause(userId, date))
        .orderBy(dailyMission.difficulty.asc())
        .fetch();
  }

  private BooleanBuilder buildWhereClause(Long userId, LocalDate date) {
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(userDailyMissionAssignment.userId.eq(userId));
    builder.and(userDailyMissionAssignment.assignedDate.eq(date));
    return builder;
  }
}
