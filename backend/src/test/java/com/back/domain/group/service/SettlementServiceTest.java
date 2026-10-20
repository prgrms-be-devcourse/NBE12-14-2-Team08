package com.back.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.back.domain.group.dto.SettlementResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private HabitVerifyRepository habitVerifyRepository;
    @Mock
    private PenaltyVerifyRepository penaltyVerifyRepository;

    @Test
    @DisplayName("방장이 초기화한 벌칙도 결산의 벌칙 수에 포함되고, 제출일이 없어 지연 집계에서는 빠진다")
    void 초기화된_벌칙도_벌칙수에_포함() {
        Member member = Member.create("일반회원", "member", "pass1234");
        ReflectionTestUtils.setField(member, "id", 2L);

        Group group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .build();
        ReflectionTestUtils.setField(group, "id", 100L);
        ReflectionTestUtils.setField(group, "createDate", LocalDateTime.of(2026, 10, 1, 9, 0));
        group.finish();

        GroupMember groupMember = GroupMember.create(group, member, GroupMemberRole.MEMBER);
        Habit habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);

        PenaltyVerify penalty = PenaltyVerify.create(groupMember, habit);
        ReflectionTestUtils.setField(penalty, "createDate", LocalDateTime.of(2026, 10, 8, 0, 5));
        penalty.submit(LocalDate.of(2026, 10, 10), "벌칙 수행했습니다.", "https://example.com/penalty.jpg");
        penalty.approve();
        penalty.resetByOwner();

        given(groupRepository.findById(100L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L)).willReturn(Optional.of(groupMember));
        given(habitVerifyRepository.findAllByGroupId(100L)).willReturn(List.of());
        given(penaltyVerifyRepository.findAllByGroupId(100L)).willReturn(List.of(penalty));

        SettlementResponse response = settlementService.getSettlement(100L, 2L);

        assertThat(response.personalStats().penaltyCount()).isEqualTo(1);
        assertThat(response.groupComparison().longestPenaltyDelay()).isNull();
    }
}
