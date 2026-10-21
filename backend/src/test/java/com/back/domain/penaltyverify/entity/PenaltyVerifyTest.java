package com.back.domain.penaltyverify.entity;

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

class PenaltyVerifyTest {

    private GroupMember groupMember;
    private Habit habit;

    @BeforeEach
    void setUp() {
        Member member = Member.create("일반회원", "member", "pass1234");
        Group group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .build();
        groupMember = GroupMember.create(group, member, GroupMemberRole.MEMBER);
        habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);
    }

    private PenaltyVerify penaltyIn(PenaltyVerifyStatus status) {
        PenaltyVerify pv = PenaltyVerify.create(groupMember, habit);
        ReflectionTestUtils.setField(pv, "status", status);
        if (status != PenaltyVerifyStatus.REQUIRED) {
            ReflectionTestUtils.setField(pv, "verifyDate", LocalDate.of(2026, 10, 14));
            ReflectionTestUtils.setField(pv, "description", "벌칙 수행했습니다.");
            ReflectionTestUtils.setField(pv, "imageUrl", "https://example.com/penalty.jpg");
        }
        return pv;
    }

    private void assertResetToRequired(PenaltyVerify pv) {
        assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
        assertThat(pv.getDescription()).isNull();
        assertThat(pv.getImageUrl()).isNull();
        assertThat(pv.getVerifyDate()).isNull();
        // 벌칙 내용 스냅샷은 그대로 남는다
        assertThat(pv.getHabitTitle()).isEqualTo("기상 후 스트레칭");
        assertThat(pv.getPenaltyText()).isEqualTo("커피 1잔 쏘기");
    }

    @Nested
    @DisplayName("제출 취소(cancelSubmission)")
    class CancelSubmission {

        @Test
        @DisplayName("PENDING 벌칙을 취소하면 REQUIRED로 돌아가고 설명, 사진, 제출일이 비워진다")
        void pending_취소_성공() {
            PenaltyVerify pv = penaltyIn(PenaltyVerifyStatus.PENDING);

            pv.cancelSubmission();

            assertResetToRequired(pv);
        }

        @ParameterizedTest(name = "{0} 상태는 취소할 수 없다")
        @EnumSource(value = PenaltyVerifyStatus.class, names = {"APPROVED", "REJECTED", "REQUIRED"})
        @DisplayName("PENDING이 아니면 IllegalStateException")
        void pending_아니면_예외(PenaltyVerifyStatus status) {
            PenaltyVerify pv = penaltyIn(status);

            assertThatThrownBy(pv::cancelSubmission)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 대기(PENDING) 중인 벌칙만 제출을 취소할 수 있습니다.");
            assertThat(pv.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("취소한 뒤 다시 제출하면 PENDING이 된다")
        void 취소_후_재제출() {
            PenaltyVerify pv = penaltyIn(PenaltyVerifyStatus.PENDING);
            pv.cancelSubmission();

            pv.submit(LocalDate.of(2026, 10, 16), "다시 수행했습니다.", "https://example.com/new.jpg");

            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
            assertThat(pv.getImageUrl()).isEqualTo("https://example.com/new.jpg");
        }
    }

    @Nested
    @DisplayName("벌칙 초기화(resetByOwner)")
    class ResetByOwner {

        @ParameterizedTest(name = "{0} 벌칙을 초기화할 수 있다")
        @EnumSource(value = PenaltyVerifyStatus.class, names = {"PENDING", "APPROVED", "REJECTED"})
        @DisplayName("제출된 적 있는 벌칙은 REQUIRED로 돌아가고 설명, 사진, 제출일이 비워진다")
        void 제출된_벌칙_초기화_성공(PenaltyVerifyStatus status) {
            PenaltyVerify pv = penaltyIn(status);

            pv.resetByOwner();

            assertResetToRequired(pv);
        }

        @Test
        @DisplayName("이미 REQUIRED인 벌칙은 되돌릴 내용이 없어 IllegalStateException")
        void required_예외() {
            PenaltyVerify pv = penaltyIn(PenaltyVerifyStatus.REQUIRED);

            assertThatThrownBy(pv::resetByOwner)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 미제출(REQUIRED) 상태인 벌칙입니다.");
        }

        @Test
        @DisplayName("초기화한 뒤 다시 제출하면 PENDING이 된다")
        void 초기화_후_재제출() {
            PenaltyVerify pv = penaltyIn(PenaltyVerifyStatus.APPROVED);
            pv.resetByOwner();

            pv.submit(LocalDate.of(2026, 10, 18), "다시 수행했습니다.", "https://example.com/new.jpg");

            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
            assertThat(pv.getVerifyDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        }
    }

    @Nested
    @DisplayName("벌칙 생성(create)")
    class Create {

        @Test
        @DisplayName("미제출(REQUIRED)로 시작하고, 습관 제목과 방의 벌칙 문구를 그때 값으로 남긴다")
        void 생성하면_REQUIRED와_스냅샷() {
            PenaltyVerify pv = PenaltyVerify.create(groupMember, habit);

            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
            assertThat(pv.getVerifyDate()).isNull();
            assertThat(pv.getHabitTitle()).isEqualTo("기상 후 스트레칭");
            assertThat(pv.getPenaltyText()).isEqualTo("커피 1잔 쏘기");
        }
    }

    @Nested
    @DisplayName("승인(approve)과 반려(reject)")
    class ApproveReject {

        @Test
        @DisplayName("검토 대기 벌칙을 승인하면 APPROVED가 된다")
        void 대기_벌칙_승인() {
            PenaltyVerify pv = penaltyIn(PenaltyVerifyStatus.PENDING);

            pv.approve();

            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.APPROVED);
        }

        @Test
        @DisplayName("검토 대기 벌칙을 반려하면 REJECTED가 된다")
        void 대기_벌칙_반려() {
            PenaltyVerify pv = penaltyIn(PenaltyVerifyStatus.PENDING);

            pv.reject();

            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.REJECTED);
        }

        @ParameterizedTest(name = "{0} 벌칙은 승인할 수 없다")
        @EnumSource(value = PenaltyVerifyStatus.class, names = {"REQUIRED", "APPROVED", "REJECTED"})
        @DisplayName("검토 대기가 아니면 승인할 수 없고(409) 상태가 그대로다")
        void 대기_아니면_승인_불가(PenaltyVerifyStatus status) {
            PenaltyVerify pv = penaltyIn(status);

            assertThatThrownBy(pv::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 대기 상태가 아닙니다.");

            assertThat(pv.getStatus()).isEqualTo(status);
        }

        @ParameterizedTest(name = "{0} 벌칙은 반려할 수 없다")
        @EnumSource(value = PenaltyVerifyStatus.class, names = {"REQUIRED", "APPROVED", "REJECTED"})
        @DisplayName("검토 대기가 아니면 반려할 수 없고(409) 상태가 그대로다")
        void 대기_아니면_반려_불가(PenaltyVerifyStatus status) {
            PenaltyVerify pv = penaltyIn(status);

            assertThatThrownBy(pv::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 대기 상태가 아닙니다.");

            assertThat(pv.getStatus()).isEqualTo(status);
        }
    }
}
