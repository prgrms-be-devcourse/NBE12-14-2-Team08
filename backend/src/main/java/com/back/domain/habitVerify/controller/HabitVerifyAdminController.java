package com.back.domain.habitVerify.controller;

import com.back.domain.habitVerify.dto.HabitVerifyResponse;
import com.back.domain.habitVerify.dto.HabitVerifySummaryResponse;
import com.back.domain.habitVerify.service.HabitVerifyService;
import com.back.global.security.LoginMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HabitVerifyAdminController {

    private final HabitVerifyService habitVerifyService;

    @GetMapping("/groups/{groupId}/habit-verifications/pending")
    public List<HabitVerifySummaryResponse> getPending(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId
    ) {
        return habitVerifyService.getPendingByGroup(
                memberId, groupId
        );
    }

    @PatchMapping("/habit-verifications/{id}/approve")
    public HabitVerifyResponse approve(
            @LoginMemberId Long memberId,
            @PathVariable Long id
    ) {
        return habitVerifyService.approve(
                memberId, id
        );
    }

    @PatchMapping("/habit-verifications/{id}/reject")
    public HabitVerifyResponse reject(
            @LoginMemberId Long memberId,
            @PathVariable Long id
    ) {
        return habitVerifyService.reject(
                memberId, id
        );
    }
}