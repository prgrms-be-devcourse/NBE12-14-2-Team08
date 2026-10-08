package com.back.domain.member.controller;

import com.back.domain.member.dto.CreateMemberRequest;
import com.back.domain.member.dto.MemberResponse;
import com.back.domain.member.dto.UpdateMemberRequest;
import com.back.domain.member.service.MemberService;
import com.back.global.security.LoginMemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 API", description = "새로운 회원 가입 및 로그인한 본인의 프로필 정보 조회, 수정, 탈퇴 계정 관리")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원 API는 요청과 응답을 담당하고, 저장·비밀번호 처리·탈퇴 조건은 MemberService에 위임한다.
    // 가입 요청은 @Valid로 DTO 제약을 검증하며, 성공 시 회원 정보와 HTTP 201을 반환한다.
    @Operation(
            summary = "회원 가입",
            description = "서비스 이용을 위한 새로운 회원 계정을 등록합니다.\n\n" +
                    "- 회원 가입 성공 시 생성된 회원 프로필 정보를 반환합니다."
    )
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<MemberResponse> join(
            @Valid @RequestBody CreateMemberRequest request
    ) {
        MemberResponse response = memberService.join(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

    }

    // 요청에서 회원 ID를 받지 않고 @LoginMemberId로 인증된 회원의 ID를 주입받아 본인 정보를 조회한다.
    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 회원 프로필 정보를 조회합니다. (본인만 가능)"
    )
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @LoginMemberId Long memberId
    ) {
        MemberResponse response = memberService.getMember(memberId);

        return ResponseEntity.ok(response);
    }

    // 인증된 본인의 정보만 수정한다. 닉네임 변경과 기존 비밀번호 확인은 서비스에서 처리한다.
    // 현재 수정 요청에는 @Valid가 없으므로 가입 요청과 동일한 DTO 검증이 적용되는 것은 아니다.
    @Operation(
            summary = "내 정보 수정",
            description = "현재 로그인한 사용자의 닉네임, 비밀번호 등 회원 정보를 수정합니다. (본인만 가능)"
    )
    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateMyInfo(
            @LoginMemberId Long memberId,
            @RequestBody UpdateMemberRequest request
    ) {
        MemberResponse response = memberService.updateMember(memberId, request);

        return ResponseEntity.ok(response);
    }

    // 서비스에서 진행 중인 방 참여 여부를 확인한 뒤 회원을 WITHDRAWN 상태로 전환한다.
    // 실제 회원 행을 삭제하는 방식이 아니며, 성공 응답은 본문 없는 HTTP 204이다.
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 회원 계정을 삭제합니다. (본인만 가능)"
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @LoginMemberId Long memberId
    ) {
        memberService.deleteMember(memberId);

        return ResponseEntity.noContent().build();
    }
}
