package com.back.domain.habit.entity;

import com.back.domain.groupMember.entity.GroupMember;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "habit",
        indexes = {
                @Index(
                        name = "idx_habit_group_member",
                        columnList = "group_member_id"
                )
        }
)
public class Habit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_member_id",
            nullable = false
    )
    private GroupMember groupMember;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private int days;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HabitStatus status;

    public static Habit create(
            GroupMember groupMember,
            String title,
            String description,
            int days
    ) {
        Habit habit = new Habit();
        habit.groupMember = groupMember;
        habit.title = title;
        habit.description = description;
        habit.days = days;
        habit.status = HabitStatus.ACTIVE;
        return habit;
    }


    public void update(Integer days) {
        if (days != null) {
            if (days < 1 || days > 7) {
                throw new IllegalArgumentException(
                        "실천 일수는 1~7이어야 합니다."
                );
            }

            this.days = days;
        }
    }

    public void fail() {
        if (this.status != HabitStatus.ACTIVE) {
            throw new IllegalStateException(
                    "활성화된 습관만 실패 처리할 수 있습니다."
            );
        }

        this.status = HabitStatus.FAILED;
    }

}