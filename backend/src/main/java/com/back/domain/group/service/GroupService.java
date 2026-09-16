package com.back.domain.group.service;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

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

        // TODO: 방장 권한 검증

        return GroupResponse.Detail.from(savedGroup);
    }

    public List<GroupResponse.Simple> getGroupSimpleList(Long memberId) {
        // TODO: 내가 가입한 방만 조회하도록

        return groupRepository.findAll().stream()
                .map(GroupResponse.Simple::from)
                .toList();
    }

    public GroupResponse.Detail getGroupDetail(Long memberId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        // TODO: 내가 가입한 방만 조회하도록

        return GroupResponse.Detail.from(group);
    }

    @Transactional
    public void updateGroup(Long memberId, Long groupId, GroupRequest.Update request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        // TODO: 방장 권한 검증

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
    public void deleteGroup(Long memberId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 그룹입니다."));

        // TODO: 방장 권한 검증

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

// 임시 시큐리티
interface PasswordEncoder {
    String encode(CharSequence rawPassword);
}