package com.back.domain.group.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Value("${app.invite-base-url}")
    private String baseInviteUrl;

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
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        GroupMember owner = GroupMember.builder()
                .group(savedGroup)
                .member(member)
                .role(GroupMemberRole.OWNER)
                .build();

        groupMemberRepository.save(owner);

        return GroupResponse.Detail.from(savedGroup, baseInviteUrl);
    }

    public List<GroupResponse.Simple> getGroupSimpleList(Long memberId) {
        return groupRepository.findMyGroupsWithCount(memberId);
    }

    @Transactional
    public GroupResponse.Detail getGroupDetail(Long groupId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 그룹입니다."));

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw new ForbiddenException("해당 그룹의 접근 권한이 없습니다.");
        }

        if (group.getStatus() == GroupStatus.ACTIVE && LocalDate.now().isAfter(group.getDeadline())) {
            group.finish();
        }

        return GroupResponse.Detail.from(group, baseInviteUrl);
    }

    @Transactional
    public void updateGroup(Long groupId, Long memberId, GroupRequest.Update request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 그룹입니다."));

        if (group.getStatus() == GroupStatus.FINISH || LocalDate.now().isAfter(group.getDeadline())) {
            throw new BusinessRuleException("이미 종료된 그룹은 수정할 수 없습니다.");
        }

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
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 그룹입니다."));

        if (group.getStatus() == GroupStatus.FINISH || LocalDate.now().isAfter(group.getDeadline())) {
            throw new BusinessRuleException("이미 종료된 그룹은 삭제할 수 없습니다.");
        }

        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new ForbiddenException("해당 그룹의 권한이 없습니다."));

        if (groupMember.getRole() != GroupMemberRole.OWNER) {
            throw new ForbiddenException("그룹 삭제는 방장만 가능합니다.");
        }

        groupRepository.delete(group);
    }

    @Transactional
    @Scheduled(cron = "1 0 0 * * *")
    public void autoCloseExpiredGroups() {
        LocalDate today = LocalDate.now();

        groupRepository.bulkFinishExpiredGroups(today);
    }

    private String generateUniqueInviteCode() {
        String inviteCode;
        do {
            inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (groupRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }
}