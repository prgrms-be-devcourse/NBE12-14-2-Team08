package com.back.domain.group.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "groups")
public class Group extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String description;

    private LocalDate deadline;

    private String penalty;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String inviteCode;

    private int memberLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupStatus status = GroupStatus.ACTIVE;

    public void updateGroup(String title, String description, LocalDate deadline, String penalty, String password,
                            int memberLimit) {

        if (title != null && !title.isBlank()) {
            this.title = title;
        }

        if (description != null) {
            this.description = description;
        }

        if (deadline != null) {
            this.deadline = deadline;
        }

        if (penalty != null) {
            this.penalty = penalty;
        }

        if (password != null && !password.isBlank()) {
            this.password = password;
        }

        if (memberLimit > 0) {
            this.memberLimit = memberLimit;
        }
    }

    public void finish() {
        this.status = GroupStatus.FINISH;
    }

}