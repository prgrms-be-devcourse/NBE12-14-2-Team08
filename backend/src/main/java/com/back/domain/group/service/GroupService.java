package com.back.domain.group.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ForbiddenException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public GroupResponse.Detail createGroup(Long memberId, GroupRequest.Create request) {
        String inviteCode = generateUniqueInviteCode();

        String encodedPassword = passwordEncoder.encode(request.password());

        Group group = Group.builder()
                .title(request.title())
                .description(request.description())
                .deadline(request.deadline())
                .penalty(request.penalty())
                .password(encodedPassword)
                .inviteCode(inviteCode)
                .memberLimit(request.memberLimit())
                .build();

        Group savedGroup = groupRepository.save(group);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        GroupMember owner = GroupMember.builder()
                .group(savedGroup)
                .member(member)
                .role(GroupMemberRole.OWNER)
                .build();

        groupMemberRepository.save(owner);

        return GroupResponse.Detail.from(savedGroup);
    }

    public List<GroupResponse.Simple> getGroupSimpleList(Long memberId) {
        List<GroupMember> groupMembers = groupMemberRepository.findByMemberId(memberId);

        return groupMembers.stream()
                .map(GroupMember::getGroup)
                .map(GroupResponse.Simple::from)
                .toList();
    }

    public GroupResponse.Detail getGroupDetail(Long groupId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw new ForbiddenException("해당 그룹의 접근 권한이 없습니다.");
        }

        return GroupResponse.Detail.from(group);
    }

    @Transactional
    public void updateGroup(Long groupId, Long memberId, GroupRequest.Update request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new ForbiddenException("해당 그룹의 접근 권한이 없습니다."));

        if (groupMember.getRole() != GroupMemberRole.OWNER) {
            throw new ForbiddenException("그룹 수정은 방장만 가능합니다.");
        }

        String encodedPassword = null;
        if (request.password() != null && !request.password().isBlank()) {
            encodedPassword = passwordEncoder.encode(request.password());
        }

        group.updateGroup(
                request.title(),
                request.description(),
                request.deadline(),
                request.penalty(),
                encodedPassword,
                request.memberLimit()
        );
    }

    @Transactional
    public void deleteGroup(Long groupId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new ForbiddenException("해당 그룹의 권한이 없습니다."));

        if (groupMember.getRole() != GroupMemberRole.OWNER) {
            throw new ForbiddenException("그룹 삭제는 방장만 가능합니다.");
        }

        groupRepository.delete(group);
    }

    private String generateUniqueInviteCode() {
        String inviteCode;
        do {
            inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (groupRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }
}