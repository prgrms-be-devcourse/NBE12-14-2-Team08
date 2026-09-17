package com.back.domain.habit.service;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.dto.HabitResponse;
import com.back.domain.habit.dto.UpdateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HabitService {
    private final GroupMemberRepository groupMemberRepository;
    private final HabitRepository habitRepository;

    @Transactional
    public HabitResponse createHabit(
            Long groupId,
            Long memberId,
            CreateHabitRequest request
    ) {
        GroupMember groupMember =
                groupMemberRepository
                        .findByGroupIdAndMemberId(
                                groupId,
                                memberId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "해당 그룹의 멤버가 아닙니다."
                                ));

        if (habitRepository.existsByGroupMember_Id(
                groupMember.getId()
        )) {
            throw new IllegalStateException(
                    "이미 습관이 등록되어 있습니다."
            );
        }

        Habit habit = Habit.create(
                groupMember,
                request.title(),
                request.days()
        );

        return new HabitResponse(
                habitRepository.save(habit)
        );
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

    public Optional<Habit> findById(Long habitId) {
        return habitRepository.findById(habitId);
    }
}
