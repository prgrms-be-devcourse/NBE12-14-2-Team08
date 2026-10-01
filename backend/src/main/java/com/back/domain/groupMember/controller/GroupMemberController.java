package com.back.domain.groupMember.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.service.GroupService;
import com.back.domain.groupMember.dto.GroupMemberResponse;
import com.back.domain.groupMember.service.GroupMemberService;
import com.back.global.security.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "그룹 멤버 API", description = "초대 코드를 이용한 가입/조회, 멤버 목록 및 상세 조회, 탈퇴/추방 및 방장 위임")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupMemberController {
    private final GroupMemberService groupMemberService;
    private final GroupService groupService;

    @Operation(
            summary = "초대 코드로 그룹 조회",
            description = "초대 코드를 이용해 해당 그룹의 정보를 확인합니다. (그룹 가입 여부와 관계없이 조회 가능)\n\n" +
                    "- 초대 링크 진입 시 방에 가입하기 전, 방의 개요 및 상세 프로필을 미리보기 화면으로 조회합니다."
    )
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<GroupResponse.Detail> getGroupByInviteCode(
            @PathVariable String inviteCode,
            @LoginMemberId Long memberId) {

        GroupResponse.Detail response = groupService.getGroupByInviteCode(inviteCode, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "초대 코드로 그룹 가입",
            description = "초대 코드를 사용하여 특정 그룹에 멤버로 가입합니다.\n\n" +
                    "- 가입 성공 시 유저 닉네임 등이 반영된 그룹 멤버 권한이 부여되며, 해당 그룹 ID를 반환합니다."
    )
    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<GroupMemberResponse.JoinSuccess> joinGroup(
            @PathVariable String inviteCode,
            @LoginMemberId Long memberId,
            @Valid @RequestBody GroupRequest.Join request) {
        Long groupId = groupMemberService.joinGroup(inviteCode, memberId, request);
        return ResponseEntity.ok(new GroupMemberResponse.JoinSuccess(groupId));
    }

    @Operation(
            summary = "그룹 멤버 목록 조회",
            description = "특정 그룹에 가입된 전체 멤버 목록을 조회합니다. (그룹 가입자만 조회 가능)\n\n" +
                    "- 각 멤버의 현재 실천 중인 습관 정보(이름, 설명)와 그룹 내 역할 등을 반환합니다."
    )
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse.Simple>> getGroupMembers(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        List<GroupMemberResponse.Simple> response = groupMemberService.getGroupMembers(groupId, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "그룹 멤버 상세 조회",
            description = "특정 그룹에 가입된 멤버의 상세 정보를 조회합니다. (그룹 가입자만 조회 가능)\n\n" +
                    "- 해당 멤버의 누적 벌칙 횟수와 그룹 내 역할 등을 반환합니다."
    )
    @GetMapping("/{groupId}/members/{groupMemberId}")
    public ResponseEntity<GroupMemberResponse.Detail> getGroupMemberDetail(
            @PathVariable Long groupId,
            @PathVariable Long groupMemberId,
            @LoginMemberId Long memberId) {
        GroupMemberResponse.Detail response = groupMemberService.getGroupMemberDetail(groupId, groupMemberId, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "그룹 탈퇴",
            description = "특정 그룹에서 완전히 탈퇴합니다. (그룹 가입자만 가능)\n\n" +
                    "- 활성화된 그룹: 참여 정보와 관련 데이터가 즉시 하드 삭제(Hard Delete)됩니다.\n" +
                    "- 마감된 그룹: 기록 유지를 위해 데이터 삭제 없이 소프트 딜리트(Soft Delete) 처리만 진행됩니다."
    )
    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        groupMemberService.leaveGroup(groupId, memberId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "그룹 멤버 추방",
            description = "그룹 내 특정 멤버를 강제 퇴장시킵니다. (그룹 방장만 가능)\n\n" +
                    "- 챌린지가 진행 중인 활성화된 그룹에서만 처리가 가능합니다.\n" +
                    "- 마감된 그룹에서는 멤버를 추방할 수 없습니다."
    )
    @DeleteMapping("/{groupId}/members/{groupMemberId}/kick")
    public ResponseEntity<Void> kickMember(
            @PathVariable Long groupId,
            @PathVariable Long groupMemberId,
            @LoginMemberId Long memberId) {
        groupMemberService.kickMember(groupId, groupMemberId, memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "그룹 방장 위임",
            description = "해당 그룹의 방장 권한을 다른 멤버에게 위임합니다. (그룹 방장만 가능)\n\n" +
                    "- 위임 즉시 본인의 권한은 일반 가입자로 강등되며, 대상 멤버가 새로운 OWNER 권한을 갖게 됩니다."
    )
    @PatchMapping("/{groupId}/members/{groupMemberId}/delegate")
    public ResponseEntity<Void> transferOwner(
            @PathVariable Long groupId,
            @PathVariable Long groupMemberId,
            @LoginMemberId Long memberId) {
        groupMemberService.transferOwner(groupId, groupMemberId, memberId);
        return ResponseEntity.ok().build();
    }
}