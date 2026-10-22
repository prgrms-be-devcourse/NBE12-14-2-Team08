package com.back.domain.habitVerify.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.domain.group.entity.Group;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.habit.entity.Habit;
import com.back.domain.member.entity.Member;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class HabitVerifyTest {

    private Habit habit;

    @BeforeEach
    void setUp() {
        Member member = Member.create("일반회원", "member", "pass1234");
        Group group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .build();
        GroupMember groupMember = GroupMember.create(group, member, GroupMemberRole.MEMBER);
        habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);
    }

    private HabitVerify verifyIn(HabitVerifyStatus status) {
        HabitVerify habitVerify = HabitVerify.create(habit, LocalDate.of(2026, 10, 21), "스트레칭 완료", "https://example.com/a.png");
        ReflectionTestUtils.setField(habitVerify, "status", status);
        return habitVerify;
    }

    @Test
    @DisplayName("인증을 만들면 검토 대기(PENDING)로 시작한다")
    void 생성하면_PENDING() {
        HabitVerify habitVerify = HabitVerify.create(habit, LocalDate.of(2026, 10, 21), "스트레칭 완료", "https://example.com/a.png");

        assertThat(habitVerify.getStatus()).isEqualTo(HabitVerifyStatus.PENDING);
        assertThat(habitVerify.getHabit()).isSameAs(habit);
        assertThat(habitVerify.getVerifyDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Nested
    @DisplayName("승인(approve)")
    class Approve {

        @Test
        @DisplayName("검토 대기 인증을 승인하면 APPROVED가 된다")
        void 대기_인증_승인() {
            HabitVerify habitVerify = verifyIn(HabitVerifyStatus.PENDING);

            habitVerify.approve();

            assertThat(habitVerify.getStatus()).isEqualTo(HabitVerifyStatus.APPROVED);
        }

        @ParameterizedTest(name = "{0} 상태")
        @EnumSource(value = HabitVerifyStatus.class, names = {"APPROVED", "REJECTED"})
        @DisplayName("이미 처리된 인증은 승인할 수 없고(409) 상태가 그대로다")
        void 처리된_인증_승인_불가(HabitVerifyStatus status) {
            HabitVerify habitVerify = verifyIn(status);

            assertThatThrownBy(habitVerify::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("검토 대기 상태가 아닙니다");

            assertThat(habitVerify.getStatus()).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("반려(reject)")
    class Reject {

        @Test
        @DisplayName("검토 대기 인증을 반려하면 REJECTED가 된다")
        void 대기_인증_반려() {
            HabitVerify habitVerify = verifyIn(HabitVerifyStatus.PENDING);

            habitVerify.reject();

            assertThat(habitVerify.getStatus()).isEqualTo(HabitVerifyStatus.REJECTED);
        }

        @ParameterizedTest(name = "{0} 상태")
        @EnumSource(value = HabitVerifyStatus.class, names = {"APPROVED", "REJECTED"})
        @DisplayName("이미 처리된 인증은 반려할 수 없고(409) 상태가 그대로다")
        void 처리된_인증_반려_불가(HabitVerifyStatus status) {
            HabitVerify habitVerify = verifyIn(status);

            assertThatThrownBy(habitVerify::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("검토 대기 상태가 아닙니다");

            assertThat(habitVerify.getStatus()).isEqualTo(status);
        }
    }
}
