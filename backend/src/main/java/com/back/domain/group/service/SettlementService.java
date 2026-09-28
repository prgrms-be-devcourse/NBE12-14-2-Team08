package com.back.domain.group.service;

import com.back.domain.group.dto.SettlementResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.entity.HabitVerifyStatus;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final HabitVerifyRepository habitVerifyRepository;
    private final PenaltyVerifyRepository penaltyVerifyRepository;

    public SettlementResponse getSettlement(Long groupId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new EntityNotFoundException("존재하지 않는 그룹입니다."));

        GroupMember currentGroupMember =
                groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                        .orElseThrow(() ->
                                new ForbiddenException("해당 그룹의 결산을 조회할 권한이 없습니다."));

        if (group.getStatus() != GroupStatus.FINISH) {
            throw new BusinessRuleException("완료된 그룹만 결산을 조회할 수 있습니다.");
        }

        List<HabitVerify> habitVerifies =
                habitVerifyRepository.findAllByGroupId(groupId);

        List<PenaltyVerify> penaltyVerifies =
                penaltyVerifyRepository.findAllByGroupId(groupId);

        SettlementResponse.PersonalStats personalStats =
                createPersonalStats(
                        currentGroupMember,
                        habitVerifies,
                        penaltyVerifies
                );

        SettlementResponse.GroupComparison groupComparison =
                createGroupComparison(
                        group,
                        habitVerifies,
                        penaltyVerifies
                );

        return new SettlementResponse(
                group.getId(),
                group.getTitle(),
                personalStats,
                groupComparison
        );
    }

    private SettlementResponse.PersonalStats createPersonalStats(
            GroupMember currentGroupMember,
            List<HabitVerify> habitVerifies,
            List<PenaltyVerify> penaltyVerifies
    ) {
        Long memberId = currentGroupMember.getMember().getId();

        long totalVerifyCount = habitVerifies.stream()
                .filter(verify -> isSameMember(verify, memberId))
                .count();

        long approvedCount = habitVerifies.stream()
                .filter(verify -> isSameMember(verify, memberId))
                .filter(verify ->
                        verify.getStatus() == HabitVerifyStatus.APPROVED)
                .count();

        long rejectedCount = habitVerifies.stream()
                .filter(verify -> isSameMember(verify, memberId))
                .filter(verify ->
                        verify.getStatus() == HabitVerifyStatus.REJECTED)
                .count();

        long penaltyCount = penaltyVerifies.stream()
                .filter(penalty ->
                        penalty.getGroupMember()
                                .getMember()
                                .getId()
                                .equals(memberId))
                .count();

        return new SettlementResponse.PersonalStats(
                memberId,
                currentGroupMember.getMember().getNickname(),
                approvedCount,
                totalVerifyCount,
                rejectedCount,
                penaltyCount
        );
    }

    private SettlementResponse.GroupComparison createGroupComparison(
            Group group,
            List<HabitVerify> habitVerifies,
            List<PenaltyVerify> penaltyVerifies
    ) {
        Map<Long, String> nicknames = new HashMap<>();
        Map<Long, Long> descriptionChars = new HashMap<>();
        Map<Long, Long> deadlineVerifications = new HashMap<>();
        Map<Long, Long> rejectedVerifications = new HashMap<>();
        Map<Long, Long> penaltyDelays = new HashMap<>();

        LocalDate groupStartDate =
                group.getCreateDate().toLocalDate();

        for (HabitVerify verify : habitVerifies) {
            GroupMember groupMember =
                    verify.getHabit().getGroupMember();

            Long memberId = groupMember.getMember().getId();
            String nickname = groupMember.getMember().getNickname();

            nicknames.putIfAbsent(memberId, nickname);

            if (verify.getDescription() != null
                    && !verify.getDescription().isBlank()) {
                long characterCount =
                        verify.getDescription().codePointCount(
                                0,
                                verify.getDescription().length()
                        );

                descriptionChars.merge(
                        memberId,
                        characterCount,
                        Long::sum
                );
            }

            if (verify.getStatus() == HabitVerifyStatus.REJECTED) {
                rejectedVerifications.merge(
                        memberId,
                        1L,
                        Long::sum
                );
            }

            if (verify.getStatus() == HabitVerifyStatus.APPROVED
                    && isWeeklyDeadline(
                    groupStartDate,
                    verify.getVerifyDate()
            )) {
                deadlineVerifications.merge(
                        memberId,
                        1L,
                        Long::sum
                );
            }
        }

        for (PenaltyVerify penalty : penaltyVerifies) {
            GroupMember groupMember = penalty.getGroupMember();

            Long memberId = groupMember.getMember().getId();
            String nickname = groupMember.getMember().getNickname();

            nicknames.putIfAbsent(memberId, nickname);

            if (penalty.getCreateDate() != null
                    && penalty.getVerifyDate() != null) {
                long delayDays = ChronoUnit.DAYS.between(
                        penalty.getCreateDate().toLocalDate(),
                        penalty.getVerifyDate()
                );

                penaltyDelays.merge(
                        memberId,
                        Math.max(delayDays, 0),
                        Math::max
                );
            }
        }

        return new SettlementResponse.GroupComparison(
                findWinner(descriptionChars, nicknames),
                findWinner(deadlineVerifications, nicknames),
                findWinner(rejectedVerifications, nicknames),
                findWinner(penaltyDelays, nicknames)
        );
    }

    private boolean isSameMember(
            HabitVerify verify,
            Long memberId
    ) {
        return verify.getHabit()
                .getGroupMember()
                .getMember()
                .getId()
                .equals(memberId);
    }

    private boolean isWeeklyDeadline(
            LocalDate groupStartDate,
            LocalDate verifyDate
    ) {
        if (verifyDate == null || verifyDate.isBefore(groupStartDate)) {
            return false;
        }

        long elapsedDays =
                ChronoUnit.DAYS.between(groupStartDate, verifyDate);

        return elapsedDays % 7 == 6;
    }

    private SettlementResponse.Winner findWinner(
            Map<Long, Long> values,
            Map<Long, String> nicknames
    ) {
        return values.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(
                        Comparator
                                .<Map.Entry<Long, Long>>comparingLong(
                                        Map.Entry::getValue
                                )
                                .reversed()
                                .thenComparingLong(Map.Entry::getKey)
                )
                .findFirst()
                .map(entry ->
                        new SettlementResponse.Winner(
                                entry.getKey(),
                                nicknames.get(entry.getKey()),
                                entry.getValue()
                        )
                )
                .orElse(null);
    }
}
