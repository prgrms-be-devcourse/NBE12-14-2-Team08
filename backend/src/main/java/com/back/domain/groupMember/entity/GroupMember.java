package com.back.domain.groupMember.entity;

import com.back.domain.group.entity.Group;
import com.back.domain.member.entity.Member;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "group_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_member",
                        columnNames = {"group_id", "member_id"}
                )
        }
)
public class GroupMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupMemberRole role;

    public static GroupMember create(Group group, Member member, GroupMemberRole role) {
        GroupMember groupMember = new GroupMember();
        groupMember.group = group;
        groupMember.member = member;
        groupMember.role = role;
        return groupMember;
    }
}