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
import com.back.global.util.ImageUrlValidator;
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

    @Transactional
    public void createPenaltyVerify(Habit habit) {
        PenaltyVerify penaltyVerify = PenaltyVerify.create(
            habit.getGroupMember(),
            habit
        );
        penaltyVerifyRepository.save(penaltyVerify);
    }

    @Transactional
    public PenaltyVerifyDetailResponse submit(Long memberId, Long habitId,
        SubmitPenaltyVerifyRequest request) {
        ImageUrlValidator.validate(request.imageUrl());

        PenaltyVerify penaltyVerify = penaltyVerifyRepository.findByHabitId(habitId)
            .orElseThrow(() -> new NoSuchElementException("해당 습관에 대한 벌칙 정보가 존재하지 않습니다."));

        if (!penaltyVerify.getGroupMember().getMember().getId().equals(memberId)) {
            throw new IllegalStateException("본인의 습관에 대해서만 벌칙을 제출할 수 있습니다.");
        }

        penaltyVerify.submit(
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

    @Transactional
    public void delete(Long memberId, Long id) {
        PenaltyVerify penaltyVerify = findById(id);

        boolean isAuthor = penaltyVerify.getGroupMember().getMember().getId().equals(memberId);
        boolean isOwner = isGroupOwner(memberId, penaltyVerify.getGroupMember().getGroup().getId());

        if (!isAuthor && !isOwner) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        penaltyVerifyRepository.delete(penaltyVerify);
    }

    private PenaltyVerify findById(Long id) {
        return penaltyVerifyRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("존재하지 않는 벌칙 인증입니다."));
    }

    private void validateOwner(Long memberId, Long groupId) {
        if (!isGroupOwner(memberId, groupId)) {
            throw new IllegalStateException("승인/거절 권한이 없습니다.");
        }
    }

    private boolean isGroupOwner(Long memberId, Long groupId) {
        return groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
            .map(gm -> gm.getRole() == GroupMemberRole.OWNER)
            .orElse(false);
    }
}