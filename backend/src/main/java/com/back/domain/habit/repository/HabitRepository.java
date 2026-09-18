package com.back.domain.habit.repository;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    Optional<Habit> findByIdAndGroupMember_Member_Id(
            Long habitId,
            Long memberId
    );

    boolean existsByGroupMember_IdAndStatus(
            Long groupMemberId,
            HabitStatus status
    );


    Optional<Habit> findByGroupMember_IdAndGroupMember_Group_IdAndStatus(
            Long memberId,
            Long groupId,
            HabitStatus status
    );

    List<Habit> findAllByGroupMember_IdAndGroupMember_Group_IdAndStatusOrderByCreateDateDesc(
            Long memberId,
            Long groupId,
            HabitStatus status
    );
}
