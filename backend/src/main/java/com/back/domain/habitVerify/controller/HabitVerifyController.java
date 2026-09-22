package com.back.domain.habitVerify.controller;

import com.back.domain.habitVerify.dto.HabitVerifyRequest;
import com.back.domain.habitVerify.dto.HabitVerifyResponse;
import com.back.domain.habitVerify.service.HabitVerifyService;
import com.back.global.security.LoginMemberId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habits/{habitId}/verifications")
@RequiredArgsConstructor
public class HabitVerifyController {

    private final HabitVerifyService habitVerifyService;

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

    @GetMapping
    public ResponseEntity<List<HabitVerifyResponse>> findAll(
            @PathVariable Long habitId,
             @LoginMemberId Long memberId
    ) {

        return ResponseEntity.ok(
                habitVerifyService.findAll(habitId, memberId)
        );
    }

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