package com.back.domain.penaltyverify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
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
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.ForbiddenException;
import com.back.global.storage.StorageService;
import com.back.global.storage.UploadUrlResponse;
import com.back.global.storage.VerificationImageType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Mock
    private StorageService storageService;

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
        ReflectionTestUtils.setField(pv, "imageUrl", "https://example.com/test.jpg");
        ReflectionTestUtils.setField(pv, "description", "기존 제출 내용");
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
        @DisplayName("submit: 타인의 습관에 벌칙을 제출하려 하면 ForbiddenException(403), 사진 검사는 하지 않는다")
        void submit_타인습관_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "타인의 벌칙 제출", "https://example.com/test.jpg"
            );

            assertThatThrownBy(() -> penaltyVerifyService.submit(999L, 50L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("본인의 습관에 대해서만 벌칙을 제출할 수 있습니다.");
            verify(storageService, never()).validateImageUrl(any(), anyLong(), anyLong(), anyString(), any());
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
        @DisplayName("resubmit: 타인의 벌칙을 재제출하려 하면 ForbiddenException(403), 사진 검사는 하지 않는다")
        void resubmit_타인벌칙_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REJECTED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            SubmitPenaltyVerifyRequest request = new SubmitPenaltyVerifyRequest(
                "타인 재제출 시도", "https://example.com/test.jpg"
            );

            assertThatThrownBy(() -> penaltyVerifyService.resubmit(1L, 1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("본인의 벌칙만 재제출할 수 있습니다.");
            verify(storageService, never()).validateImageUrl(any(), anyLong(), anyLong(), anyString(), any());
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
    @DisplayName("벌칙 제출 취소 및 초기화(cancelOrReset) 테스트")
    class CancelOrResetTest {

        private PenaltyVerify createOwnerPenalty(Long id, PenaltyVerifyStatus status) {
            Habit ownerHabit = Habit.create(ownerGroupMember, "아침 독서", "10쪽 읽기", 3);
            ReflectionTestUtils.setField(ownerHabit, "id", 51L);

            PenaltyVerify pv = PenaltyVerify.create(ownerGroupMember, ownerHabit);
            ReflectionTestUtils.setField(pv, "id", id);
            ReflectionTestUtils.setField(pv, "status", status);
            ReflectionTestUtils.setField(pv, "imageUrl", "https://example.com/owner.jpg");
            ReflectionTestUtils.setField(pv, "description", "방장 제출 내용");
            return pv;
        }

        @Test
        @DisplayName("작성자가 PENDING 벌칙을 제출 취소하면 REQUIRED가 되고 행은 지우지 않는다")
        void 작성자_PENDING_제출취소_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            PenaltyVerifyDetailResponse response = penaltyVerifyService.cancelOrReset(2L, 1L);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
            assertThat(pv.getImageUrl()).isNull();
            assertThat(pv.getDescription()).isNull();
            assertThat(pv.getVerifyDate()).isNull();
            verify(penaltyVerifyRepository, never()).delete(pv);
        }

        @Test
        @DisplayName("작성자는 APPROVED 벌칙을 제출 취소할 수 없다 (409)")
        void 작성자_APPROVED_제출취소_예외() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.APPROVED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.cancelOrReset(2L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 대기(PENDING) 중인 벌칙만 제출을 취소할 수 있습니다.");
            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.APPROVED);
        }

        @Test
        @DisplayName("방장은 멤버의 APPROVED 벌칙을 초기화할 수 있다")
        void 방장_APPROVED_초기화_성공() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.APPROVED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));

            PenaltyVerifyDetailResponse response = penaltyVerifyService.cancelOrReset(1L, 1L);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
            assertThat(pv.getImageUrl()).isNull();
            assertThat(pv.getDescription()).isNull();
            verify(penaltyVerifyRepository, never()).delete(pv);
        }

        @Test
        @DisplayName("방장은 자기 자신의 APPROVED 벌칙도 초기화할 수 있다")
        void 방장_본인벌칙_APPROVED_초기화_성공() {
            PenaltyVerify pv = createOwnerPenalty(2L, PenaltyVerifyStatus.APPROVED);
            given(penaltyVerifyRepository.findById(2L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 1L))
                .willReturn(Optional.of(ownerGroupMember));

            PenaltyVerifyDetailResponse response = penaltyVerifyService.cancelOrReset(1L, 2L);

            assertThat(response.status()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
            assertThat(pv.getImageUrl()).isNull();
        }

        @Test
        @DisplayName("방 밖의 사람이 요청하면 ForbiddenException (403)")
        void 방밖의_사람_ForbiddenException() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 999L))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> penaltyVerifyService.cancelOrReset(999L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("벌칙 취소/초기화 권한이 없습니다.");
            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
        }

        @Test
        @DisplayName("같은 방의 일반 멤버가 남의 벌칙을 요청하면 ForbiddenException (403)")
        void 같은방_일반멤버_ForbiddenException() {
            GroupMember otherGroupMember = GroupMember.create(group, otherUser, GroupMemberRole.MEMBER);
            ReflectionTestUtils.setField(otherGroupMember, "id", 30L);

            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 999L))
                .willReturn(Optional.of(otherGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.cancelOrReset(999L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("벌칙 취소/초기화 권한이 없습니다.");
            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
        }

        @Test
        @DisplayName("종료된 방의 벌칙은 권한이 있어도 BusinessRuleException (400)")
        void 종료된방_BusinessRuleException() {
            group.finish();

            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(groupMemberRepository.findByGroupIdAndMemberId(100L, 2L))
                .willReturn(Optional.of(normalGroupMember));

            assertThatThrownBy(() -> penaltyVerifyService.cancelOrReset(2L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("이미 종료된 그룹의 벌칙 기록은 변경할 수 없습니다.");
            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("벌칙 사진 업로드 URL 발급(createUploadUrl) 테스트")
    class CreateUploadUrlTest {

        @ParameterizedTest(name = "{0} 벌칙은 발급 가능")
        @EnumSource(value = PenaltyVerifyStatus.class, names = {"REQUIRED", "REJECTED"})
        @DisplayName("내 벌칙이 제출 또는 재제출 가능한 상태면 벌칙 사진 업로드 URL을 발급한다")
        void 업로드_URL_발급_성공(PenaltyVerifyStatus status) {
            PenaltyVerify pv = createFixturePenalty(1L, status);
            UploadUrlResponse expected = new UploadUrlResponse("https://upload", "https://public/2/1/a.png");
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));
            given(storageService.createUploadUrl(VerificationImageType.PENALTY, 2L, 1L, "proof.png")).willReturn(expected);

            UploadUrlResponse response = penaltyVerifyService.createUploadUrl(2L, 1L, "proof.png");

            assertThat(response).isEqualTo(expected);
        }

        @Test
        @DisplayName("내 벌칙이 아니면 ForbiddenException (403)")
        void 내_벌칙_아님_403() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            assertThatThrownBy(() -> penaltyVerifyService.createUploadUrl(999L, 1L, "proof.png"))
                .isInstanceOf(ForbiddenException.class);

            verify(storageService, never()).createUploadUrl(any(), anyLong(), anyLong(), anyString());
        }

        @ParameterizedTest(name = "{0} 벌칙은 발급 불가")
        @EnumSource(value = PenaltyVerifyStatus.class, names = {"PENDING", "APPROVED"})
        @DisplayName("이미 제출했거나 승인된 벌칙이면 IllegalStateException (409)")
        void 제출_불가_상태_409(PenaltyVerifyStatus status) {
            PenaltyVerify pv = createFixturePenalty(1L, status);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            assertThatThrownBy(() -> penaltyVerifyService.createUploadUrl(2L, 1L, "proof.png"))
                .isInstanceOf(IllegalStateException.class);

            verify(storageService, never()).createUploadUrl(any(), anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("종료된 방이면 BusinessRuleException (400)")
        void 종료된_방_400() {
            group.finish();
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            assertThatThrownBy(() -> penaltyVerifyService.createUploadUrl(2L, 1L, "proof.png"))
                .isInstanceOf(BusinessRuleException.class);

            verify(storageService, never()).createUploadUrl(any(), anyLong(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("벌칙 제출 시 인증 사진 검사 테스트")
    class SubmitImageValidationTest {

        @Test
        @DisplayName("submit: 사진 URL 검사에 통과하면 지금 벌칙 id와 기존 사진을 넘겨 검사한 뒤 PENDING이 된다")
        void submit_사진검사_통과() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            ReflectionTestUtils.setField(pv, "imageUrl", null);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            penaltyVerifyService.submit(2L, 50L, new SubmitPenaltyVerifyRequest("수행", "https://storage/new.png"));

            verify(storageService).validateImageUrl(VerificationImageType.PENALTY, 2L, 1L, "https://storage/new.png", null);
            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
        }

        @Test
        @DisplayName("submit: 사진 URL 검사에 실패하면 400이고 벌칙 상태는 그대로다")
        void submit_사진검사_실패() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));
            willThrow(new IllegalArgumentException("업로드한 사진만 사용할 수 있습니다."))
                .given(storageService).validateImageUrl(any(), anyLong(), anyLong(), anyString(), any());

            assertThatThrownBy(() -> penaltyVerifyService.submit(2L, 50L,
                new SubmitPenaltyVerifyRequest("수행", "https://evil.com/a.png")))
                .isInstanceOf(IllegalArgumentException.class);

            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.REQUIRED);
        }

        @Test
        @DisplayName("submit: 사진 없이 제출하면 400 (벌칙 인증 사진은 필수)")
        void submit_사진없음() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REQUIRED);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            assertThatThrownBy(() -> penaltyVerifyService.submit(2L, 50L, new SubmitPenaltyVerifyRequest("수행", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("벌칙 인증 사진은 필수입니다.");

            verify(storageService, never()).validateImageUrl(any(), anyLong(), anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("submit: 제출할 수 없는 상태(PENDING)면 사진 검사 전에 409")
        void submit_상태먼저() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.PENDING);
            given(penaltyVerifyRepository.findByHabitId(50L)).willReturn(Optional.of(pv));

            assertThatThrownBy(() -> penaltyVerifyService.submit(2L, 50L,
                new SubmitPenaltyVerifyRequest("수행", "https://storage/new.png")))
                .isInstanceOf(IllegalStateException.class);

            verify(storageService, never()).validateImageUrl(any(), anyLong(), anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("resubmit: 지금 붙어 있는 사진을 기존 사진으로 넘겨 검사한다 (예전 경로 사진 재사용 허용)")
        void resubmit_기존사진_전달() {
            PenaltyVerify pv = createFixturePenalty(1L, PenaltyVerifyStatus.REJECTED);
            ReflectionTestUtils.setField(pv, "imageUrl", "https://storage/penalties/2/old.png");
            given(penaltyVerifyRepository.findById(1L)).willReturn(Optional.of(pv));

            penaltyVerifyService.resubmit(2L, 1L,
                new SubmitPenaltyVerifyRequest("다시 수행", "https://storage/penalties/2/old.png"));

            verify(storageService).validateImageUrl(VerificationImageType.PENALTY, 2L, 1L,
                "https://storage/penalties/2/old.png", "https://storage/penalties/2/old.png");
            assertThat(pv.getStatus()).isEqualTo(PenaltyVerifyStatus.PENDING);
        }
    }
}
