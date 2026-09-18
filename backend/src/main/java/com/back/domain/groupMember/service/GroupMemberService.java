package com.back.domain.groupMember.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.GroupLimitExceededException;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMemberService {
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void joinGroup(Long memberId, GroupRequest.Join request) {
        Group group = groupRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new NoSuchElementException("유효하지 않거나 존재하지 않는 초대 코드입니다."));

        if (groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), memberId)) {
            return;
        }

        long currentMemberCount = groupMemberRepository.countByGroupId(group.getId());
        if (currentMemberCount >= group.getMemberLimit()) {
            throw new GroupLimitExceededException("그룹의 최대 인원이 초과되어 입장할 수 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        GroupMember groupMember = GroupMember.builder()
                .group(group)
                .member(member)
                .role(GroupMemberRole.MEMBER)
                .build();

        groupMemberRepository.save(groupMember);
    }

    public List<GroupMemberResponse.Simple> getGroupMembers (Long groupId, Long memberId) {
        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw new NoSuchElementException("해당 그룹의 접근 권한이 없거나 존재하지 않는 그룹입니다.");
        }

        return groupMemberRepository.findByGroupIdWithHabit(groupId);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long memberId) {
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new NoSuchElementException("해당 그룹의 멤버가 아닙니다."));

        if (groupMember.getRole() == GroupMemberRole.OWNER) {
            throw new IllegalStateException("방장은 그룹을 탈퇴할 수 없습니다. 방 삭제 기능을 이용해 주세요.");
        }

        groupMemberRepository.delete(groupMember);
    }
}