package com.back.domain.penaltyverify.entity;

import com.back.domain.groupMember.entity.GroupMember;
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
    name = "penalty_verify",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_penalty_verify",
            columnNames = {
                "group_member_id",
                "habit_id"
            }
        )
    }
)
public class PenaltyVerify extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_member_id", nullable = false)
    private GroupMember groupMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "verify_date")
    private LocalDate verifyDate;

    @Column(nullable = false)
    private String habitTitle;
    // 이전 벌칙 스냅샷
    @Column(nullable = false)
    private String penaltyText;

    private String imageUrl;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PenaltyVerifyStatus status;

    public static PenaltyVerify create(
            GroupMember groupMember,
            Habit habit
    ) {
        PenaltyVerify penaltyVerify = new PenaltyVerify();

        penaltyVerify.groupMember = groupMember;
        penaltyVerify.habit = habit;
        penaltyVerify.verifyDate = null;

        penaltyVerify.habitTitle = habit.getTitle();
        penaltyVerify.penaltyText = groupMember.getGroup().getPenalty();

        penaltyVerify.status = PenaltyVerifyStatus.REQUIRED;

        return penaltyVerify;
    }

    public static PenaltyVerify submit(GroupMember groupMember, Habit habit, LocalDate verifyDate,
        String habitTitle, String penaltyText, String description, String imageUrl) {

        PenaltyVerify pv = new PenaltyVerify();
        pv.groupMember = groupMember;
        pv.habit = habit;
        pv.verifyDate = verifyDate;
        pv.habitTitle = habitTitle;
        pv.penaltyText = penaltyText;
        pv.description = description;
        pv.imageUrl = imageUrl;
        pv.status = PenaltyVerifyStatus.PENDING;
        return pv;
    }

    public void approve() {
        if (this.status != PenaltyVerifyStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태가 아닙니다. 현재 상태 : " + this.status);
        }
        this.status = PenaltyVerifyStatus.APPROVED;
    }

    public void reject() {
        if (this.status != PenaltyVerifyStatus.PENDING) {
            throw new IllegalStateException("승인 대기 상태가 아닙니다. 현재 상태 : " + this.status);
        }
        this.status = PenaltyVerifyStatus.REJECTED;
    }


}