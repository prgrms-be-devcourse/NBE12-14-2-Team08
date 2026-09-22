package com.back.domain.groupMember.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.service.GroupMemberService;
import com.back.global.security.LoginMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupMemberController {
    private final GroupMemberService groupMemberService;

    @PostMapping("/join")
    public ResponseEntity<Void> joinGroup(
            @LoginMemberId Long memberId,
            @Valid @RequestBody GroupRequest.Join request) {
        groupMemberService.joinGroup(memberId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse.Simple>> getGroupMembers(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        List<GroupMemberResponse.Simple> response = groupMemberService.getGroupMembers(groupId, memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}/members/{groupMemberId}")
    public ResponseEntity<GroupMemberResponse.Detail> getGroupMemberDetail(
            @PathVariable Long groupId,
            @PathVariable Long groupMemberId,
            @LoginMemberId Long memberId) {
        GroupMemberResponse.Detail response = groupMemberService.getGroupMemberDetail(groupId, groupMemberId, memberId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        groupMemberService.leaveGroup(groupId, memberId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{groupMemberId}/kick")
    public ResponseEntity<Void> kickMember(
            @PathVariable Long groupId,
            @PathVariable Long groupMemberId,
            @LoginMemberId Long memberId) {
        groupMemberService.kickMember(groupId, groupMemberId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/members/{groupMemberId}/delegate")
    public ResponseEntity<Void> transferOwner(
            @PathVariable Long groupId,
            @PathVariable Long groupMemberId,
            @LoginMemberId Long memberId) {
        groupMemberService.transferOwner(groupId, groupMemberId, memberId);
        return ResponseEntity.ok().build();
    }
}