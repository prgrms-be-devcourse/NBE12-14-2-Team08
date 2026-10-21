package com.back.domain.habit.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.domain.group.entity.Group;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.member.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HabitTest {

    private GroupMember groupMember;

    @BeforeEach
    void setUp() {
        Member member = Member.create("일반회원", "member", "pass1234");
        Group group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .build();
        groupMember = GroupMember.create(group, member, GroupMemberRole.MEMBER);
    }

    @Nested
    @DisplayName("생성(create)")
    class Create {

        @Test
        @DisplayName("생성하면 입력한 값이 그대로 담기고 상태는 ACTIVE다")
        void 생성_초기_상태는_ACTIVE() {
            Habit habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);

            assertThat(habit.getGroupMember()).isSameAs(groupMember);
            assertThat(habit.getTitle()).isEqualTo("기상 후 스트레칭");
            assertThat(habit.getDescription()).isEqualTo("10분 스트레칭");
            assertThat(habit.getDays()).isEqualTo(3);
            assertThat(habit.getStatus()).isEqualTo(HabitStatus.ACTIVE);
        }

        @Test
        @DisplayName("설명이 없어도 생성할 수 있다")
        void 설명_없이_생성() {
            Habit habit = Habit.create(groupMember, "물 마시기", null, 7);

            assertThat(habit.getDescription()).isNull();
            assertThat(habit.getStatus()).isEqualTo(HabitStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("실패 처리(fail)")
    class Fail {

        @Test
        @DisplayName("ACTIVE 습관은 FAILED로 바뀐다")
        void ACTIVE_실패_성공() {
            Habit habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);

            habit.fail();

            assertThat(habit.getStatus()).isEqualTo(HabitStatus.FAILED);
        }

        @Test
        @DisplayName("이미 FAILED인 습관을 다시 실패 처리하면 예외가 발생하고 상태는 그대로다")
        void FAILED_재실패_예외() {
            Habit habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);
            habit.fail();

            assertThatThrownBy(habit::fail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("활성화된 습관만 실패 처리할 수 있습니다.");
            assertThat(habit.getStatus()).isEqualTo(HabitStatus.FAILED);
        }
    }
}
