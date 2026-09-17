package com.back.domain.habitVerify.entity;

import com.back.domain.habit.entity.Habit;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "habit_verify",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_habit_verify_date",
                        columnNames = {"habit_id", "verify_date"}
                )
        }
)
public class HabitVerify extends BaseEntity {

    @Column(nullable = false)
    private LocalDate verifyDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitVerifyStatus status;

    private String description;

    private String imageUrl;

    public static HabitVerify create(
            Habit habit, LocalDate verifyDate,
            HabitVerifyStatus status, String description
    ) {
        HabitVerify habitVerify = new HabitVerify();

        habitVerify.habit = habit;habitVerify.verifyDate = verifyDate;
        habitVerify.status = status;habitVerify.description = description;

        return habitVerify;
    }

    public void update(
            LocalDate verifyDate, HabitVerifyStatus status, String description) {
        this.verifyDate = verifyDate;
        this.status = status;
        this.description = description;
    }
}