package com.back.domain.groupMember.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.EntityNotFoundException;
import com.back.global.exception.ForbiddenException;
import com.back.global.exception.GroupLimitExceededException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMemberService {
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;

    private final HabitRepository habitRepository;
    private final HabitVerifyRepository habitVerifyRepository;
    private final PenaltyVerifyRepository penaltyVerifyRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void joinGroup(String inviteCode, Long memberId, GroupRequest.Join request) {
        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않거나 존재하지 않는 초대 코드입니다."));

        if (!passwordEncoder.matches(request.password(), group.getPassword())) {
            throw new ForbiddenException("그룹 비밀번호가 일치하지 않습니다.");
        }

        if (group.getStatus() == GroupStatus.FINISH || LocalDate.now().isAfter(group.getDeadline())) {
            throw new BusinessRuleException("이미 종료된 그룹입니다.");
        }

        if (groupMemberRepository.existsByGroupIdAndMemberId(group.getId(), memberId)) {
            return;
        }

        long currentMemberCount = groupMemberRepository.countByGroupId(group.getId());
        if (currentMemberCount >= group.getMemberLimit()) {
            throw new GroupLimitExceededException("그룹의 최대 인원이 초과되어 입장할 수 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));

        GroupMember groupMember = GroupMember.builder()
                .group(group)
                .member(member)
                .role(GroupMemberRole.MEMBER)
                .build();

        groupMemberRepository.save(groupMember);
    }

    public List<GroupMemberResponse.Simple> getGroupMembers(Long groupId, Long memberId) {
        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw new ForbiddenException("해당 그룹의 접근 권한이 없습니다.");
        }

        return groupMemberRepository.findByGroupIdWithHabit(groupId);
    }

    public GroupMemberResponse.Detail getGroupMemberDetail(Long groupId, Long groupMemberId, Long memberId) {
        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw new ForbiddenException("해당 그룹의 접근 권한이 없습니다.");
        }

        return groupMemberRepository.findMemberDetailWithPenaltyCount(groupId, groupMemberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 해당 그룹의 가입 멤버가 아닙니다."));
    }

    @Transactional
    public void leaveGroup(Long groupId, Long memberId) {
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 그룹의 멤버가 아닙니다."));

        Group group = groupMember.getGroup();
        if (groupMember.getRole() == GroupMemberRole.OWNER) {
            if (group.getStatus() == GroupStatus.ACTIVE) {
                throw new BusinessRuleException("방장은 그룹 활성화 상태에서 그룹을 바로 탈퇴할 수 없습니다. 권한 위임 후 탈퇴해 주세요.");
            }
        }

        if (group.getStatus() == GroupStatus.ACTIVE) {
            deleteGroupMemberDataBulk(groupMember.getId());

            groupMemberRepository.delete(groupMember);
        } else if (group.getStatus() == GroupStatus.FINISH) {
            groupMember.leave();
        }
    }

    @Transactional
    public void kickMember(Long groupId, Long groupMemberId, Long memberId) {
        GroupMember owner = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 그룹의 멤버가 아닙니다."));

        if (owner.getRole() != GroupMemberRole.OWNER) {
            throw new ForbiddenException("그룹 멤버 추방은 방장만 가능합니다.");
        }

        GroupMember kickTarget = groupMemberRepository.findById(groupMemberId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 해당 그룹의 멤버가 아닙니다."));

        if (!kickTarget.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("해당 그룹에 속한 멤버가 아닙니다.");
        }

        if (kickTarget.getId().equals(owner.getId())) {
            throw new BusinessRuleException("방장 스스로를 추방할 수 없습니다. 방 삭제를 이용해 주세요.");
        }

        Group group = kickTarget.getGroup();
        if (group.getStatus() == GroupStatus.FINISH || LocalDate.now().isAfter(group.getDeadline())) {
            throw new BusinessRuleException("이미 종료된 그룹은 멤버를 추방할 수 없습니다.");
        }


        deleteGroupMemberDataBulk(kickTarget.getId());

        groupMemberRepository.delete(kickTarget);
    }

    @Transactional
    public void deleteGroupMemberDataBulk(Long groupMemberId) {
        List<Long> habitIds = habitRepository.findIdsByGroupMemberId(groupMemberId);

        if (!habitIds.isEmpty()) {
            habitVerifyRepository.deleteByHabitIds(habitIds);
        }

        penaltyVerifyRepository.deleteByGroupMemberId(groupMemberId);

        if (!habitIds.isEmpty()) {
            habitRepository.deleteByIds(habitIds);
        }
    }

    @Transactional
    public void transferOwner(Long groupId, Long groupMemberId, Long memberId) {
        GroupMember currentOwner = groupMemberRepository.findByGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 그룹의 멤버가 아닙니다."));

        if (currentOwner.getRole() != GroupMemberRole.OWNER) {
            throw new ForbiddenException("방장 권한 위임은 현재 방장만 가능합니다.");
        }

        GroupMember nextOwner = groupMemberRepository.findById(groupMemberId)
                .orElseThrow(() -> new EntityNotFoundException("권한을 위임할 대상 멤버가 존재하지 않습니다."));

        if (!nextOwner.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("해당 그룹에 속한 멤버가 아닙니다.");
        }

        if (nextOwner.getId().equals(currentOwner.getId())) {
            throw new BusinessRuleException("자기 자신에게 방장 권한을 위임할 수 없습니다.");
        }

        currentOwner.changeRole(GroupMemberRole.MEMBER);
        nextOwner.changeRole(GroupMemberRole.OWNER);
    }
}