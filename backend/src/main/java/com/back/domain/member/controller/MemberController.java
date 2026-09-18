package com.back.domain.member.controller;

import com.back.domain.member.dto.CreateMemberRequest;
import com.back.domain.member.dto.MemberResponse;
import com.back.domain.member.dto.UpdateMemberRequest;
import com.back.domain.member.service.MemberService;
import com.back.global.security.LoginMemberId;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @LoginMemberId Long memberId
    ) {
        MemberResponse response = memberService.getMember(memberId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateMyInfo(
            @LoginMemberId Long memberId,
            @RequestBody UpdateMemberRequest request
    ) {
        MemberResponse response = memberService.updateMember(memberId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @LoginMemberId Long memberId
    ) {
        memberService.deleteMember(memberId);

        return ResponseEntity.noContent().build();
    }
}