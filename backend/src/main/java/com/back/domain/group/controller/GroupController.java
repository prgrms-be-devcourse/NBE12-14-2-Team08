package com.back.domain.group.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.dto.GroupResponse.Simple;
import com.back.domain.group.service.GroupService;
import com.back.global.security.LoginMemberId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse.Detail> createGroup(@LoginMemberId Long memberId,
                                                            @Valid @RequestBody GroupRequest.Create request) {
        GroupResponse.Detail response = groupService.createGroup(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse.Simple>> getGroupSimpleList(@LoginMemberId Long memberId) {
        List<GroupResponse.Simple> response = groupService.getGroupSimpleList(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse.Detail> getGroupDetail(@LoginMemberId Long memberId,
                                                               @PathVariable Long groupId) {
        GroupResponse.Detail response = groupService.getGroupDetail(memberId, groupId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupRequest.Update request) {

        groupService.updateGroup(memberId, groupId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@LoginMemberId Long memberId, @PathVariable Long groupId) {
        groupService.deleteGroup(memberId, groupId);
        return ResponseEntity.noContent().build();
    }
}