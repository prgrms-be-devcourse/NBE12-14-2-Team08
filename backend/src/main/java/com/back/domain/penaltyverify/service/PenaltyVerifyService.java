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
        // groupId로 스코프를 좁혀서 조회 -> 요청 ids에 타 그룹 벌칙이 섞여도 그 항목은 애초에 조회되지 않음
        List<PenaltyVerify> targets = penaltyVerifyRepository.findAllByIdInAndGroupId(request.ids(),
            groupId);
        targets.forEach(PenaltyVerify::approve);
        return targets.stream().map(PenaltyVerifyDetailResponse::from).toList();
    }

    @Transactional
    public List<PenaltyVerifyDetailResponse> bulkReject(Long memberId, Long groupId,
        BulkActionRequest request) {
        validateOwner(memberId, groupId);
        List<PenaltyVerify> targets = penaltyVerifyRepository.findAllByIdInAndGroupId(request.ids(),
            groupId);
        targets.forEach(PenaltyVerify::reject);
        return targets.stream().map(PenaltyVerifyDetailResponse::from).toList();
    }

    public List<PenaltyVerifySummaryResponse> getPendingByGroup(Long groupId) {
        return penaltyVerifyRepository.findPendingByGroupId(groupId).stream()
            .map(PenaltyVerifySummaryResponse::from)
            .toList();
    }

    public PenaltyVerifyDetailResponse getDetail(Long id) {
        return PenaltyVerifyDetailResponse.from(findById(id));
    }

    public long getCount(Long groupId, Long memberId) {
        return penaltyVerifyRepository.countByGroupMember_Group_IdAndGroupMember_Member_Id(groupId,
            memberId);
    }

    public List<PenaltyVerifySummaryResponse> getPenaltiesByGroupMember(Long groupMemberId) {
        return penaltyVerifyRepository.findByGroupMemberIdOrderByIdDesc(groupMemberId).stream()
            .map(PenaltyVerifySummaryResponse::from)
            .toList();

    }

    // 본인이 작성했거나, 해당 그룹의 방장인 경우에만 삭제 가능
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

    // memberId가 groupId의 방장인지 여부만 반환 (예외 안 던짐 - delete에서 author 체크와 함께 써야 해서 분리)
    private boolean isGroupOwner(Long memberId, Long groupId) {
        return groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
            .map(gm -> gm.getRole() == GroupMemberRole.OWNER)
            .orElse(false);
    }
}
