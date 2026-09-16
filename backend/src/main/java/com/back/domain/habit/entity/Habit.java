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
@Table(name = "habit")
public class Habit extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_member_id",
            nullable = false,
            unique = true
    )
    private GroupMember groupMember;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    private int days;
}