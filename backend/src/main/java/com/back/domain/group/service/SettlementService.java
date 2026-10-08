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

    // 결산은 조회 요청 시 현재 기록을 집계한다. 스케줄러가 결과를 미리 저장하는 구조가 아니다.
    // 소속을 확인한 뒤 DB 상태가 FINISH인 방만 허용하므로 종료 상태 반영 여부도 확인해야 한다.
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

        // 승인된 인증 건수이며, 주간 목표를 달성한 횟수와는 다르다.
        long approvedCount = habitVerifies.stream()
                .filter(verify -> isSameMember(verify, memberId))
                .filter(verify ->
                        verify.getStatus() == HabitVerifyStatus.APPROVED)
                .count();

        // 반려 이력 누적이 아니라 현재 REJECTED 상태인 건수이다. 재제출하면 집계에서 빠진다.
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

            // 습관 설명(Habit.description)이 아닌 인증 설명을 합산한다.
            // codePointCount로 보조 문자도 한 글자로 계산하며 공백도 글자 수에 포함된다.
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

            // 제출된 벌칙만 지연 일수를 계산한다. 미제출 건은 순위에 포함하지 않는다.
            // 회원별 합계가 아닌 가장 오래 지연된 한 건의 일수를 사용한다.
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

    // 방 생성일을 0일째로 두고 6, 13, 20일째를 주간 마감일로 본다.
    // 승인된 인증의 verifyDate를 기준으로 하며 승인 처리 시각을 비교하지 않는다.
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

    // 양수 기록만 순위에 포함한다. 동점은 회원 ID가 작은 사람을 선택하며 대상이 없으면 null이다.
    // 프론트는 해당 항목에 기록이 없는 경우도 표시할 수 있어야 한다.
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
