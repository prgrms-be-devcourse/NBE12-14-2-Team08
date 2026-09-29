package com.back.domain.habitVerify.controller;

import com.back.domain.habitVerify.dto.HabitVerifyRequest;
import com.back.domain.habitVerify.dto.HabitVerifyResponse;
import com.back.domain.habitVerify.service.HabitVerifyService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "습관 인증 API", description = "일반 유저의 일일 습관 인증 기록 등록, 목록 조회, 수정, 반려 건 재제출 및 삭제")
@RestController
@RequestMapping("/api/habits/{habitId}/verifications")
@RequiredArgsConstructor
public class HabitVerifyController {

    private final HabitVerifyService habitVerifyService;

    @Operation(
            summary = "습관 인증 등록",
            description = "특정 습관에 대한 실천 인증 기록을 새로 등록합니다. (그룹 가입자만 가능)\n\n" +
                    "- 오늘 날짜의 실천 내용, 사진 URL 등을 첨부하여 검토 대기(PENDING) 상태로 제출합니다."
    )
    @PostMapping
    public ResponseEntity<HabitVerifyResponse> create(
            @PathVariable Long habitId,
            @Valid @RequestBody HabitVerifyRequest request,
            @LoginMemberId Long memberId
    ) {

        HabitVerifyResponse response =
                habitVerifyService.create(
                        habitId,
                        request,
                        memberId
                );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "습관 인증 목록 조회",
            description = "특정 습관에 누적된 전체 인증 기록 목록을 조회합니다. (그룹 가입자만 조회 가능)"
    )
    @GetMapping
    public ResponseEntity<List<HabitVerifyResponse>> findAll(
            @PathVariable Long habitId,
             @LoginMemberId Long memberId
    ) {

        return ResponseEntity.ok(
                habitVerifyService.findAll(habitId, memberId)
        );
    }

    @Operation(
            summary = "습관 인증 수정",
            description = "제출한 특정 인증 기록의 내용을 수정합니다. (본인만 가능)\n\n" +
                    "- 방장의 심사가 진행되기 전(PENDING 상태)인 인증 건에 한해서만 수정이 가능합니다."
    )
    @PutMapping("/{verificationId}")
    public ResponseEntity<HabitVerifyResponse> update(
            @PathVariable Long habitId,
            @PathVariable Long verificationId,
            @Valid @RequestBody HabitVerifyRequest request,
            @LoginMemberId Long memberId
    ) {

        return ResponseEntity.ok(
                habitVerifyService.update(
                        habitId,
                        verificationId,
                        request,
                        memberId
                )
        );
    }

    @Operation(
            summary = "습관 인증 삭제",
            description = "제출한 특정 인증 기록을 삭제합니다. (본인만 가능)\n\n" +
                    "- 검토 대기(PENDING) 상태인 인증 건만 삭제가 가능합니다."
    )
    @PatchMapping("/{verificationId}/resubmit")
    public ResponseEntity<HabitVerifyResponse> resubmit(
            @PathVariable Long habitId,
            @PathVariable Long verificationId,
            @Valid @RequestBody HabitVerifyRequest request,
            @LoginMemberId Long memberId
    ) {
        return ResponseEntity.ok(
                habitVerifyService.resubmit(
                        habitId,
                        verificationId,
                        request,
                        memberId
                )
        );
    }

    @Operation(
            summary = "습관 인증 삭제",
            description = "제출한 특정 인증 기록을 삭제합니다. (본인만 가능)\n\n" +
                    "- 검토 대기(PENDING) 상태인 인증 건만 삭제가 가능합니다."
    )
    @DeleteMapping("/{verificationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long habitId,
            @PathVariable Long verificationId,
            @LoginMemberId Long memberId
    ) {
        habitVerifyService.delete(
                habitId,
                verificationId,
                memberId
        );

        return ResponseEntity.noContent().build();
    }
}