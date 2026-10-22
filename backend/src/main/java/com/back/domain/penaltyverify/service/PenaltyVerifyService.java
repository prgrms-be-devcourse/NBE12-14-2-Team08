package com.back.domain.penaltyverify.service;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.penaltyverify.dto.BulkActionRequest;
import com.back.domain.penaltyverify.dto.PenaltyVerifyDetailResponse;
import com.back.domain.penaltyverify.dto.PenaltyVerifySummaryResponse;
import com.back.domain.penaltyverify.dto.SubmitPenaltyVerifyRequest;
import com.back.domain.penaltyverify.entity.PenaltyVerify;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.ForbiddenException;
import com.back.global.storage.StorageService;
import com.back.global.storage.UploadUrlResponse;
import com.back.global.storage.VerificationImageType;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PenaltyVerifyService {

    private final PenaltyVerifyRepository penaltyVerifyRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final HabitRepository habitRepository;
    private final StorageService storageService;

    @Transactional
    public void createPenaltyVerify(Habit habit) {
        PenaltyVerify penaltyVerify = PenaltyVerify.create(
            habit.getGroupMember(),
            habit
        );
        penaltyVerifyRepository.save(penaltyVerify);
    }

    // 벌칙 인증 사진 업로드 URL 발급: 내 벌칙 → 진행 중인 방 → 제출 가능한 상태 순으로 확인
    public UploadUrlResponse createUploadUrl(Long memberId, Long id, String filename) {
        PenaltyVerify penaltyVerify = findById(id);

        if (!penaltyVerify.getGroupMember().getMember().getId().equals(memberId)) {
            throw new ForbiddenException("본인의 벌칙에만 사진을 올릴 수 있습니다.");
        }

        if (penaltyVerify.getGroupMember().getGroup().isFinished()) {
            throw new BusinessRuleException("이미 종료된 그룹에서는 벌칙 사진을 올릴 수 없습니다.");
        }

        if (!penaltyVerify.isSubmittable()) {
            throw new IllegalStateException("제출 또는 재제출할 수 있는 벌칙이 아닙니다. 현재 상태 : " + penaltyVerify.getStatus());
        }

        return storageService.createUploadUrl(VerificationImageType.PENALTY, memberId, id, filename);
    }

    @Transactional
    public PenaltyVerifyDetailResponse submit(Long memberId, Long habitId,
        SubmitPenaltyVerifyRequest request) {
        PenaltyVerify penaltyVerify = penaltyVerifyRepository.findByHabitId(habitId)
            .orElseThrow(() -> new NoSuchElementException("해당 습관에 대한 벌칙 정보가 존재하지 않습니다."));

        if (!penaltyVerify.getGroupMember().getMember().getId().equals(memberId)) {
            throw new ForbiddenException("본인의 습관에 대해서만 벌칙을 제출할 수 있습니다.");
        }

        if (penaltyVerify.getGroupMember().getGroup().isFinished()) {
            throw new BusinessRuleException("이미 종료된 그룹에서는 벌칙을 제출할 수 없습니다.");
        }

        penaltyVerify.validateSubmittable();
        validatePenaltyImage(memberId, penaltyVerify, request.imageUrl());

        penaltyVerify.submit(
            LocalDate.now(),
            request.description(),
            request.imageUrl()
        );

        return PenaltyVerifyDetailResponse.from(penaltyVerify);
    }

    @Transactional
    public PenaltyVerifyDetailResponse resubmit(Long memberId, Long id, SubmitPenaltyVerifyRequest request) {
        PenaltyVerify penaltyVerify = findById(id);

        if (!penaltyVerify.getGroupMember().getMember().getId().equals(memberId)) {
            throw new ForbiddenException("본인의 벌칙만 재제출할 수 있습니다.");
        }

        if (penaltyVerify.getGroupMember().getGroup().isFinished()) {
            throw new BusinessRuleException("이미 종료된 그룹에서는 벌칙을 재제출할 수 없습니다.");
        }

        penaltyVerify.validateResubmittable();
        validatePenaltyImage(memberId, penaltyVerify, request.imageUrl());

        penaltyVerify.resubmit(
            LocalDate.now(),
            request.description(),
            request.imageUrl()
        );

        return PenaltyVerifyDetailResponse.from(penaltyVerify);
    }

    @Transactional
    public PenaltyVerifyDetailResponse approve(Long memberId, Long id) {
        PenaltyVerify penaltyVerify = findById(id);
        validateOwner(memberId, penaltyVerify.getGroupMember().getGroup().getId());
        penaltyVerify.approve();
        return PenaltyVerifyDetailResponse.from(penaltyVerify);
    }

    @Transactional
    public PenaltyVerifyDetailResponse reject(Long memberId, Long id) {
        PenaltyVerify penaltyVerify = findById(id);
        validateOwner(memberId, penaltyVerify.getGroupMember().getGroup().getId());
        penaltyVerify.reject();
        return PenaltyVerifyDetailResponse.from(penaltyVerify);
    }

    @Transactional
    public List<PenaltyVerifyDetailResponse> bulkApprove(Long memberId, Long groupId,
        BulkActionRequest request) {
        validateOwner(memberId, groupId);
        List<PenaltyVerify> targets = penaltyVerifyRepository.findAllByIdInAndGroupId(request.ids(), groupId);
        targets.forEach(PenaltyVerify::approve);
        return targets.stream().map(PenaltyVerifyDetailResponse::from).toList();
    }

    @Transactional
    public List<PenaltyVerifyDetailResponse> bulkReject(Long memberId, Long groupId,
        BulkActionRequest request) {
        validateOwner(memberId, groupId);
        List<PenaltyVerify> targets = penaltyVerifyRepository.findAllByIdInAndGroupId(request.ids(), groupId);
        targets.forEach(PenaltyVerify::reject);
        return targets.stream().map(PenaltyVerifyDetailResponse::from).toList();
    }

    // 방장만 대기 목록 조회 가능
    public List<PenaltyVerifySummaryResponse> getPendingByGroup(Long memberId, Long groupId) {
        GroupMember requester = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
            .orElseThrow(() -> new AccessDeniedException("해당 방의 멤버가 아닙니다."));

        if (requester.getRole() != GroupMemberRole.OWNER) {
            throw new AccessDeniedException("방장만 대기 목록을 조회할 수 있습니다.");
        }

        return penaltyVerifyRepository.findPendingByGroupId(groupId).stream()
            .map(PenaltyVerifySummaryResponse::from)
            .toList();
    }

    public PenaltyVerifyDetailResponse getDetail(Long memberId, Long id) {
        PenaltyVerify penaltyVerify = findById(id);

        boolean isMember = groupMemberRepository.existsByGroupIdAndMemberId(
            penaltyVerify.getHabit().getGroupMember().getGroup().getId(), memberId
        );

        if (!isMember) {
            throw new AccessDeniedException("같은 그룹의 멤버만 조회할 수 있습니다.");
        }

        return PenaltyVerifyDetailResponse.from(penaltyVerify);
    }

    public long getCount(Long groupId, Long memberId) {
        return penaltyVerifyRepository.countByGroupMember_Group_IdAndGroupMember_Member_Id(groupId, memberId);
    }

    public List<PenaltyVerifySummaryResponse> getPenaltiesByGroupMember(Long memberId, Long groupId, Long groupMemberId) {
        boolean isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId);
        if (!isMember) {
            throw new AccessDeniedException("같은 그룹의 멤버만 조회할 수 있습니다.");
        }

        if (!groupMemberRepository.existsByIdAndGroupId(groupMemberId, groupId)) {
            throw new IllegalArgumentException("해당 그룹에 속하지 않은 멤버입니다.");
        }

        return penaltyVerifyRepository.findByGroupMemberIdOrderByIdDesc(groupMemberId).stream()
            .map(PenaltyVerifySummaryResponse::from)
            .toList();
    }

    // 결산 집계를 위해 벌칙 row는 지우지 않는다 (docs/adr/0001-penalty-verify-no-hard-delete.md)
    // 방장: 벌칙 초기화 (자기 벌칙 포함) / 작성자: PENDING 제출 취소 / 그 외: 403
    @Transactional
    public PenaltyVerifyDetailResponse cancelOrReset(Long memberId, Long id) {
        PenaltyVerify penaltyVerify = findById(id);

        boolean isAuthor = penaltyVerify.getGroupMember().getMember().getId().equals(memberId);
        boolean isOwner = isGroupOwner(memberId, penaltyVerify.getGroupMember().getGroup().getId());

        if (!isAuthor && !isOwner) {
            throw new ForbiddenException("벌칙 취소/초기화 권한이 없습니다.");
        }

        if (penaltyVerify.getGroupMember().getGroup().isFinished()) {
            throw new BusinessRuleException("이미 종료된 그룹의 벌칙 기록은 변경할 수 없습니다.");
        }

        if (isOwner) {
            penaltyVerify.resetByOwner();
        } else {
            penaltyVerify.cancelSubmission();
        }

        return PenaltyVerifyDetailResponse.from(penaltyVerify);
    }

    // 사진 필수 여부는 SubmitPenaltyVerifyRequest의 @NotBlank가 막는다.
    // Supabase 호출이 있으므로 다른 검사가 모두 끝난 뒤 마지막에 확인한다.
    private void validatePenaltyImage(Long memberId, PenaltyVerify penaltyVerify, String imageUrl) {
        storageService.validateImageUrl(
            VerificationImageType.PENALTY,
            memberId,
            penaltyVerify.getId(),
            imageUrl,
            penaltyVerify.getImageUrl()
        );
    }

    private PenaltyVerify findById(Long id) {
        return penaltyVerifyRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("존재하지 않는 벌칙 인증입니다."));
    }

    private void validateOwner(Long memberId, Long groupId) {
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
            .orElseThrow(() -> new ForbiddenException("승인/거절 권한이 없습니다."));

        if (groupMember.getRole() != GroupMemberRole.OWNER) {
            throw new ForbiddenException("승인/거절 권한이 없습니다.");
        }

        if (groupMember.getGroup().isFinished()) {
            throw new BusinessRuleException("이미 종료된 그룹은 벌칙을 검토할 수 없습니다.");
        }
    }

    private boolean isGroupOwner(Long memberId, Long groupId) {
        return groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
            .map(gm -> gm.getRole() == GroupMemberRole.OWNER)
            .orElse(false);
    }
}