package com.back.domain.habit.service;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.dto.HabitResponse;
import com.back.domain.habit.dto.UpdateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import com.back.domain.habit.repository.HabitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HabitService {
    private final GroupMemberRepository groupMemberRepository;
    private final HabitRepository habitRepository;

//    @Transactional
//    public HabitResponse createHabit(
//            Long groupId,
//            Long memberId,
//            CreateHabitRequest request
//    ) {
//        GroupMember groupMember =
//                groupMemberRepository
//                        .findByGroupIdAndMemberId(
//                                groupId,
//                                memberId
//                        )
//                        .orElseThrow(() ->
//                                new IllegalArgumentException(
//                                        "해당 그룹의 멤버가 아닙니다."
//                                ));
//
//        if (habitRepository.existsByGroupMember_Id(
//                groupMember.getId()
//        )) {
//            throw new IllegalStateException(
//                    "이미 습관이 등록되어 있습니다."
//            );
//        }
//
//        Habit habit = Habit.create(
//                groupMember,
//                request.title(),
//                request.days()
//        );
//
//        return new HabitResponse(
//                habitRepository.save(habit)
//        );
//    }

    @Transactional
    public HabitResponse createHabit(
            Long groupId,
            Long memberId,
            CreateHabitRequest request
    ) {
        // 1. GroupMember 조회
        GroupMember groupMember = groupMemberRepository
                .findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 그룹의 멤버를 찾을 수 없습니다."
                        )
                );

        // 2. 활성 습관 존재 여부 확인
        if (habitRepository.existsByGroupMember_IdAndStatus(
                groupMember.getId(),
                HabitStatus.ACTIVE
        )) {
            throw new IllegalStateException(
                    "이미 활성화된 습관이 존재합니다."
            );
        }

        // 3. 습관 생성
        Habit habit = Habit.create(
                groupMember,
                request.title(),
                request.description(),
                request.days()
        );

        // 4. 저장
        Habit savedHabit = habitRepository.save(habit);

        return new HabitResponse(savedHabit);
    }

    @Transactional
    public HabitResponse updateHabit(
            Long habitId,
            Long memberId,
            UpdateHabitRequest request
    ) {
        if (request.days() == null) {
            throw new IllegalArgumentException(
                    "수정할 값이 없습니다."
            );
        }

        Habit habit = habitRepository
                .findByIdAndGroupMember_Member_Id(
                        habitId.intValue(),
                        memberId.intValue()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "습관이 존재하지 않거나 수정 권한이 없습니다."
                        ));

        habit.update(
                request.days()
        );

        return new HabitResponse(habit);
    }

    @Transactional
    public void failHabit(
            Long groupMemberId,
            Long habitId
    ) {
        Habit habit = habitRepository
                .findByIdAndGroupMember_Id(
                        habitId,
                        groupMemberId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 습관을 찾을 수 없습니다."
                        )
                );

        if (habit.getStatus() != HabitStatus.ACTIVE) {
            throw new IllegalStateException(
                    "활성화된 습관만 실패 처리할 수 있습니다."
            );
        }

        habit.fail();
    }

    @Transactional
    public HabitResponse getActiveHabit(Long memberId, Long groupId) {

        Habit habit = habitRepository
                .findByGroupMember_IdAndGroupMember_Group_IdAndStatus(
                        memberId,
                        groupId,
                        HabitStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("현재 진행 중인 습관이 없습니다.")
                );

        return new HabitResponse(habit);
    }

    @Transactional
    public List<HabitResponse> getFailedHabits(Long memberId, Long groupId) {

        return habitRepository
                .findAllByGroupMember_IdAndGroupMember_Group_IdAndStatusOrderByCreateDateDesc(
                        memberId,
                        groupId,
                        HabitStatus.FAILED
                )
                .stream()
                .map(HabitResponse::new)
                .toList();
    }


    public Optional<Habit> findById(Long habitId) {
        return habitRepository.findById(habitId);
    }
}
