package com.back.domain.group.controller;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.dto.SettlementResponse;
import com.back.domain.group.entity.GroupStatus;
import com.back.domain.group.service.GroupService;
import com.back.domain.group.service.SettlementService;
import com.back.global.security.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "그룹 API", description = "챌린지 그룹의 생성, 상세 조회, 정보 수정 및 방 삭제")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final SettlementService settlementService;

    @Operation(
            summary = "그룹 생성",
            description = "새로운 챌린지 그룹을 생성합니다.\n\n" +
                    "- 그룹 생성 성공 시 개설된 방의 고유 ID 및 세부 정보를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<GroupResponse.Detail> createGroup(
            @LoginMemberId Long memberId,
            @Valid @RequestBody GroupRequest.Create request) {
        GroupResponse.Detail response = groupService.createGroup(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "가입한 그룹 목록 조회",
            description = "로그인한 회원이 속해있는 전체 그룹 목록을 조회합니다.\n\n" +
                    "- 상태 값(status) 파라미터에 따라 현재 진행 중이거나 마감된 그룹을 필터링할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<List<GroupResponse.Simple>> getGroupSimpleList(
            @LoginMemberId Long memberId,
            @RequestParam(value = "status", defaultValue = "ACTIVE") GroupStatus status) {
        List<GroupResponse.Simple> response = groupService.getGroupSimpleList(memberId, status);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "그룹 결산 데이터 조회",
            description = "챌린지가 마감된 특정 그룹의 최종 정산 데이터를 확인합니다. (그룹 가입자만 조회 가능)\n\n" +
                    "- 해당 방의 전체 챌린지 성공률, 멤버별 달성 통계, 최종 정산 내역을 종합하여 반환합니다."
    )
    @GetMapping("/{groupId}/settlement")
    public ResponseEntity<SettlementResponse> getSettlement(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId
    ) {
        SettlementResponse response =
                settlementService.getSettlement(groupId, memberId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "그룹 상세 조회",
            description = "특정 그룹의 상세 프로필 및 방 정보를 확인합니다. (그룹 가입자만 조회 가능)\n\n" +
                    "- 지정된 그룹 ID를 기반으로 현재 방 상태, 개설 정보 등을 리턴합니다."
    )
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse.Detail> getGroupDetail(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        GroupResponse.Detail response = groupService.getGroupDetail(groupId, memberId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "그룹 정보 수정",
            description = "특정 그룹의 설정 값을 업데이트합니다. (그룹 방장만 가능)\n\n" +
                    "- 챌린지가 활성화된 상태에서만 방의 이름 및 세부 설명 등을 변경할 수 있습니다."
    )
    @PutMapping("/{groupId}")
    public ResponseEntity<Void> updateGroup(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId,
            @Valid @RequestBody GroupRequest.Update request) {

        groupService.updateGroup(groupId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "그룹 삭제",
            description = "특정 그룹을 영구 삭제합니다. (그룹 방장만 가능)\n\n" +
                    "- 방 삭제 시 해당 그룹 내에 누적되어 있던 멤버 참여 기록 및 데이터가 함께 파기됩니다."
    )
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @LoginMemberId Long memberId) {
        groupService.deleteGroup(groupId, memberId);
        return ResponseEntity.noContent().build();
    }
}