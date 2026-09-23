package com.back.domain.habit.repository;

import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 스케줄러가 주기적으로 순회하며 자동 승인/판정을 적용할 대상 조회용
    List<Habit> findAllByStatus(HabitStatus status);

    // 그룹 단위 주간 판정 대상(그룹 내 활성 습관 전체) 조회용
    List<Habit> findAllByGroupMember_Group_IdAndStatus(Long groupId, HabitStatus status);
    @Query("select h.id from Habit h where h.groupMember.id = :groupMemberId")
    List<Long> findIdsByGroupMemberId(@Param("groupMemberId") Long groupMemberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Habit h where h.id in :habitIds")
    void deleteByIds(@Param("habitIds") List<Long> habitIds);
}
