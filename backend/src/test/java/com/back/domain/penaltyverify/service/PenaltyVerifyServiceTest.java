package com.back.domain.penaltyverify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.back.domain.group.entity.Group;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.penaltyverify.dto.BulkActionRequest;
import com.back.domain.penaltyverify.dto.PenaltyVerifyDetailResponse;
import com.back.domain.penaltyverify.dto.PenaltyVerifySummaryResponse;
import com.back.domain.penaltyverify.dto.SubmitPenaltyVerifyRequest;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.entity.PenaltyVerifyStatus;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PenaltyVerifyServiceTest {

    @Mock
    private PenaltyVerifyRepository penaltyVerifyRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private HabitRepository habitRepository;

    @InjectMocks
    private PenaltyVerifyService penaltyVerifyService;

    private Member ownerUser;
    private Member normalUser;
    private Member otherUser;
    private Group group;
    private GroupMember ownerGroupMember;
    private GroupMember normalGroupMember;
    private Habit habit;

    @BeforeEach
    void setUp() {
        ownerUser = Member.create("방장", "owner", "pass1234");
        ReflectionTestUtils.setField(ownerUser, "id", 1L);

        normalUser = Member.create("일반회원", "member", "pass1234");
        ReflectionTestUtils.setField(normalUser, "id", 2L);

        otherUser = Member.create("외부인", "other", "pass1234");
        ReflectionTestUtils.setField(otherUser, "id", 999L);

        group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .build();
        ReflectionTestUtils.setField(group, "id", 100L);

        ownerGroupMember = GroupMember.create(group, ownerUser, GroupMemberRole.OWNER);
        ReflectionTestUtils.setField(ownerGroupMember, "id", 10L);

        normalGroupMember = GroupMember.create(group, normalUser, GroupMemberRole.MEMBER);
        ReflectionTestUtils.setField(normalGroupMember, "id", 20L);

        habit = Habit.create(normalGroupMember, "기상 후 스트레칭", "10분 스트레칭", 30);
        ReflectionTestUtils.setField(habit, "id", 50L);
    }

    private PenaltyVerify createFixturePenalty(Long id, PenaltyVerifyStatus status) {
        PenaltyVerify pv = PenaltyVerify.create(normalGroupMember, habit);
        ReflectionTestUtils.setField(pv, "id", id);
        ReflectionTestUtils.setField(pv, "status", status);
        return pv;
    }

    @Nested
    @DisplayName("벌칙 생성 및 제출(submit) 테스트")
    class SubmitTest {

        @Test
        @DisplayName("submit: 본인의 습관에 대한 벌칙 제출 성공 시 PENDING 상태로 전환")
        void submit_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "스트레칭 인증합니다.", "https://example.com/stretch.jpg"
            );

            PenaltyVerifyDetailResponse response = penaltyVerifyService.submit(2L, 50L, request);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.PENDING);
            assertThat(response.description()).isEqualTo("스트레칭 인증합니다.");
            assertThat(response.imageUrl()).isEqualTo("https://example.com/stretch.jpg");
            assertThat(pv.getVerifyDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("submit: 타인의 습관에 벌칙을 제출하려 하면 IllegalStateException 발생")
        void submit_타인습관_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "타인의 벌칙 제출", "https://example.com/test.jpg"
            );

            assertThatThrownBy(() -> penaltyVerifyService.submit(999L, 50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인의 습관에 대해서만 벌칙을 제출할 수 있습니다.");
        }

        @Test
        @DisplayName("submit: REQUIRED 상태가 아닌 벌칙에 제출 시도 시 IllegalStateException 발생")
        void submit_REQUIRED아님_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.APPROVED);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "이미 승인된 벌칙", "https://example.com/test.jpg"
            );

            assertThatThrownBy(() -> penaltyVerifyService.submit(2L, 50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("벌칙 제출 대상(REQUIRED)가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("벌칙 재제출(resubmit) 테스트")
    class ResubmitTest {

        @Test
        @DisplayName("resubmit: 반려된(REJECTED) 벌칙을 본인이 재제출하면 PENDING 상태로 전환")
        void resubmit_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REJECTED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "재촬영 인증", "https://example.com/resubmit.jpg"
            );

            PenaltyVerifyDetailResponse response = penaltyVerifyService.resubmit(2L, 1L, request);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.PENDING);
            assertThat(response.description()).isEqualTo("재촬영 인증");
        }

        @Test
        @DisplayName("resubmit: REJECTED 상태가 아닌 벌칙을 재제출하려 하면 예외 발생")
        void resubmit_반려상태아님_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "재제출 시도", "https://example.com/test.jpg"
            );

            assertThatThrownBy(() -> penaltyVerifyService.resubmit(2L, 1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("반려된 벌칙만 재제출할 수 있습니다.");
        }

        @Test
        @DisplayName("resubmit: 타인의 벌칙을 재제출하려 하면 IllegalStateException 발생")
        void resubmit_타인벌칙_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REJECTED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "타인 재제출 시도", "https://example.com/test.jpg"
            );

            assertThatThrownBy(() -> penaltyVerifyService.resubmit(1L, 1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("본인의 벌칙만 재제출할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("벌칙 승인(approve) 및 거절(reject) 테스트")
    class ApproveRejectTest {

        @Test
        @DisplayName("approve: 방장이 PENDING 상태의 벌칙을 정상 승인(APPROVED)")
        void approve_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));

            PenaltyVerifyDetailResponse response = penaltyVerifyService.approve(1L, 1L);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.APPROVED);
        }

        @Test
        @DisplayName("reject: 방장이 PENDING 상태의 벌칙을 정상 거절(REJECTED)")
        void reject_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));

            PenaltyVerifyDetailResponse response = penaltyVerifyService.reject(1L, 1L);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.REJECTED);
        }

        @Test
        @DisplayName("방장이 아닌 일반 회원이 승인 시도 시 IllegalStateException 발생")
        void approve_방장아님_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.approve(2L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인/거절 권한이 없습니다.");
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 벌칙을 승인/반려하려 할 경우 예외 발생")
        void approve_PENDING아님_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.approve(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 대기 상태가 아닙니다.");
        }
    }

    @Nested
    @DisplayName("일괄 처리(bulkApprove, bulkReject) 테스트")
    class BulkActionTest {

        @Test
        @DisplayName("bulkApprove: 방장이 선택한 여러 건의 벌칙을 일괄 승인")
        void bulkApprove_성공() {
            PenaltyVerify pv1 = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            PenaltyVerify pv2 = createFixturePenalty(2L, PenaltyVerifyStatus.PENDING);

            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));
            given(penaltyVerifyRepository.findAllByIdInAndGroupId(List.of(1L, 2L), 100L))
                .willReturn(List.of(pv1, pv2));

            List<PenaltyVerifyDetailResponse> responses = penaltyVerifyService.bulkApprove(
                1L, 100L, new BulkActionRequest(List.of(1L, 2L))
            );

            assertThat(responses).hasSize(2);
            assertThat(pv1.getStatus()).isEqualTo(PenaltyVerifyStatus.APPROVED);
            assertThat(pv2.getStatus()).isEqualTo(PenaltyVerifyStatus.APPROVED);
        }

        @Test
        @DisplayName("bulkReject: 방장이 아닌 사용자가 일괄 처리 시 예외 발생")
        void bulkReject_권한없음_예외() {
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.bulkReject(2L, 100L, new BulkActionRequest(List.of(1L))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인/거절 권한이 없습니다.");
        }
    }

    @Nested
    @DisplayName("조회 관련(getPendingByGroup, getDetail, getPenaltiesByGroupMember) 테스트")
    class QueryTest {

        @Test
        @DisplayName("getPendingByGroup: 방장은 대기 중인 벌칙 목록을 정상 조회")
        void getPendingByGroup_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));
            given(penaltyVerifyRepository.findPendingByGroupId(100L))
                .willReturn(List.of(pv));

            List<PenaltyVerifySummaryResponse> result = penaltyVerifyService.getPendingByGroup(1L, 100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(PenaltyVerifyStatus.PENDING);
        }

        @Test
        @DisplayName("getPendingByGroup: 일반 멤버가 대기 목록 조회를 시도하면 AccessDeniedException 발생")
        void getPendingByGroup_일반멤버_예외() {
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.getPendingByGroup(2L, 100L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("방장만 대기 목록을 조회할 수 있습니다.");
        }

        @Test
        @DisplayName("getDetail: 동일 그룹 멤버는 벌칙 상세를 정상 조회")
        void getDetail_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 2L)).willReturn(true);

            PenaltyVerifyDetailResponse response = penaltyVerifyService.getDetail(2L, 1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.habitTitle()).isEqualTo("기상 후 스트레칭");
        }

        @Test
        @DisplayName("getDetail: 그룹에 속하지 않은 외부 사용자가 조회 시 AccessDeniedException 발생")
        void getDetail_외부인_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 999L)).willReturn(false);

            assertThatThrownBy(() -> penaltyVerifyService.getDetail(999L, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("같은 그룹의 멤버만 조회할 수 있습니다.");
        }

        @Test
        @DisplayName("getPenaltiesByGroupMember: 해당 그룹에 속하지 않은 groupMemberId 조회 시 IllegalArgumentException 발생")
        void getPenaltiesByGroupMember_없는멤버_예외() {
            given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 1L)).willReturn(true);
            given(groupMemberRepository.existsByIdAndGroupId(9999L, 100L)).willReturn(false);

            assertThatThrownBy(() -> penaltyVerifyService.getPenaltiesByGroupMember(1L, 100L, 9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 그룹에 속하지 않은 멤버입니다.");
        }
    }

    @Nested
    @DisplayName("삭제(delete) 테스트")
    class DeleteTest {

        @Test
        @DisplayName("delete: 벌칙 작성자 본인이 삭제 시 성공")
        void delete_작성자_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            penaltyVerifyService.delete(2L, 1L);

            verify(penaltyVerifyRepository).delete(pv);
        }

        @Test
        @DisplayName("delete: 방장이 타인의 벌칙 삭제 시 성공")
        void delete_방장_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));

            penaltyVerifyService.delete(1L, 1L);

            verify(penaltyVerifyRepository).delete(pv);
        }

        @Test
        @DisplayName("delete: 작성자도 아니고 방장도 아닌 사용자가 삭제 시 IllegalStateException 발생")
        void delete_권한없음_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 999L))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> penaltyVerifyService.delete(999L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("삭제 권한이 없습니다.");
        }
    }
}