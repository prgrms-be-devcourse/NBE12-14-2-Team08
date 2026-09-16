package com.back.domain.habit.service;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.dto.HabitResponse;
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
                        .findByGroup_IdAndMember_Id(
                                groupId.intValue(),
                                memberId.intValue()
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

    public Optional<Habit> findById(Long habitId) {
        return habitRepository.findById(habitId);
    }
}
