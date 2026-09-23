package com.back.domain.habitVerify.service;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.dto.HabitVerifyRequest;
import com.back.domain.habitVerify.dto.HabitVerifyResponse;
import com.back.domain.habitVerify.dto.HabitVerifySummaryResponse;
import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.global.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.back.domain.habitVerify.dto.BulkActionRequest;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitVerifyService {

    private final HabitRepository habitRepository;
    private final HabitVerifyRepository habitVerifyRepository;
    private final StorageService storageService;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public HabitVerifyResponse create(
            Long habitId,
            HabitVerifyRequest request,
            Long memberId

    ) {

        Habit habit = habitRepository.findByIdAndGroupMember_Member_Id(habitId, memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 습관입니다."
                        )
                );
        LocalDate verifyDate = LocalDate.now();

        boolean alreadyExists =
                habitVerifyRepository.existsByHabitIdAndVerifyDate(
                        habitId,
                        verifyDate
                );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "해당 날짜에 이미 인증 기록이 있습니다."
            );
        }
        if (request.imageUrl() != null) {

            boolean exists =
                    storageService.existsHabitImage(
                            request.imageUrl()
                    );

            if (!exists) {
                throw new IllegalArgumentException(
                        "업로드된 이미지를 확인할 수 없습니다."
                );
            }
        }

        HabitVerify habitVerify = HabitVerify.create(
                habit, verifyDate,
                request.description(),
                request.imageUrl()
        );

        HabitVerify savedHabitVerify =
                habitVerifyRepository.save(habitVerify);

        return HabitVerifyResponse.from(savedHabitVerify);
    }

    @Transactional(readOnly = true)
    public List<HabitVerifyResponse> findAll(
            Long habitId, Long memberId
    ){ habitRepository.findByIdAndGroupMember_Member_Id(
            habitId,
            memberId
    ).orElseThrow(() ->
            new IllegalArgumentException("권한이 없는 습관입니다.")
    );
        return habitVerifyRepository
                .findAllByHabitIdOrderByVerifyDateDesc(habitId)
                .stream()
                .map(HabitVerifyResponse::from)
                .toList();
    }

    @Transactional
    public HabitVerifyResponse update(
            Long habitId,
            Long verificationId,
            HabitVerifyRequest request,
            Long memberId
    ) {
        habitRepository.findByIdAndGroupMember_Member_Id(
                habitId,
                memberId
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "권한이 없는 습관입니다."
                )
        );
        HabitVerify habitVerify =
                habitVerifyRepository
                        .findByIdAndHabitId(
                                verificationId,
                                habitId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인증 기록입니다."
                                )
                        );

        habitVerify.update(
                request.description(),
                request.imageUrl()
        );

        return HabitVerifyResponse.from(habitVerify);
    }

    @Transactional
    public void delete(
            Long habitId,
            Long verificationId,
            Long memberId
    ) {
        habitRepository.findByIdAndGroupMember_Member_Id(
                habitId,
                memberId
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "권한이 없는 습관입니다."
                )
        );
        HabitVerify habitVerify =
                habitVerifyRepository
                        .findByIdAndHabitId(
                                verificationId,
                                habitId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인증 기록입니다."
                                )
                        );

        habitVerifyRepository.delete(habitVerify);
    }
    @Transactional(readOnly = true)
    public List<HabitVerifySummaryResponse> getPendingByGroup(
            Long memberId,
            Long groupId
    ) {
        GroupMember groupMember =
                groupMemberRepository
                        .findByGroupIdAndMemberId(groupId, memberId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "해당 그룹의 멤버가 아닙니다."
                                )
                        );

        if (groupMember.getRole() != GroupMemberRole.OWNER) {
            throw new AccessDeniedException(
                    "방장만 대기 목록을 조회할 수 있습니다."
            );
        }

        return habitVerifyRepository
                .findPendingByGroupId(groupId)
                .stream()
                .map(HabitVerifySummaryResponse::from)
                .toList();
    }
    @Transactional
    public HabitVerifyResponse approve(
            Long memberId,
            Long verificationId
    ) {
        HabitVerify habitVerify =
                habitVerifyRepository
                        .findById(verificationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인증 기록입니다."
                                )
                        );

        Long groupId = habitVerify
                .getHabit()
                .getGroupMember()
                .getGroup()
                .getId();

        validateOwner(memberId, groupId);

        habitVerify.approve();

        return HabitVerifyResponse.from(habitVerify);
    }
    @Transactional
    public HabitVerifyResponse reject(
            Long memberId,
            Long verificationId
    ) {
        HabitVerify habitVerify =
                habitVerifyRepository
                        .findById(verificationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인증 기록입니다."
                                )
                        );

        Long groupId = habitVerify
                .getHabit()
                .getGroupMember()
                .getGroup()
                .getId();

        validateOwner(memberId, groupId);

        habitVerify.reject();

        return HabitVerifyResponse.from(habitVerify);
    }

    @Transactional
    public List<HabitVerifyResponse> bulkApprove(
            Long memberId,
            Long groupId,
            BulkActionRequest request
    ) {
        validateOwner(memberId, groupId);

        List<HabitVerify> targets =
                habitVerifyRepository.findAllByIdInAndGroupId(
                        request.ids(),
                        groupId
                );

        targets.forEach(HabitVerify::approve);

        return targets.stream()
                .map(HabitVerifyResponse::from)
                .toList();
    }

    @Transactional
    public List<HabitVerifyResponse> bulkReject(
            Long memberId,
            Long groupId,
            BulkActionRequest request
    ) {
        validateOwner(memberId, groupId);

        List<HabitVerify> targets =
                habitVerifyRepository.findAllByIdInAndGroupId(
                        request.ids(),
                        groupId
                );

        targets.forEach(HabitVerify::reject);

        return targets.stream()
                .map(HabitVerifyResponse::from)
                .toList();
    }
    private void validateOwner(
            Long memberId,
            Long groupId
    ) {
        boolean isOwner =
                groupMemberRepository
                        .findByGroupIdAndMemberId(
                                groupId,
                                memberId
                        )
                        .map(groupMember ->
                                groupMember.getRole()
                                        == GroupMemberRole.OWNER
                        )
                        .orElse(false);

        if (!isOwner) {
            throw new AccessDeniedException(
                    "승인/거절 권한이 없습니다."
            );
        }
    }
}