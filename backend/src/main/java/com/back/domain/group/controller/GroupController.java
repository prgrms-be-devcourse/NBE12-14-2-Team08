package com.back.domain.group.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.service.GroupService;
import com.back.global.security.LoginMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse.Detail> createGroup(
            @LoginMemberId Long memberId,
            @Valid @RequestBody GroupRequest.Create request) {
        GroupResponse.Detail response = groupService.createGroup(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse.Simple>> getGroupSimpleList(
            @LoginMemberId Long memberId,
            @RequestParam(value = "status", defaultValue = "ACTIVE") GroupStatus status) {
        List<GroupResponse.Simple> response = groupService.getGroupSimpleList(memberId, status);
        return ResponseEntity.ok(response);
    }

    //초대 코드로 방 정보 일부 조회
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<GroupResponse.InvitePreview> getInvitePreview(
            @PathVariable String inviteCode
    ) {
        GroupResponse.InvitePreview response =
                groupService.getInvitePreview(inviteCode);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse.Detail> getGroupDetail(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        GroupResponse.Detail response = groupService.getGroupDetail(groupId, memberId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId,
            @Valid @RequestBody GroupRequest.Update request) {

        groupService.updateGroup(groupId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        groupService.deleteGroup(groupId, memberId);
        return ResponseEntity.noContent().build();
    }
}