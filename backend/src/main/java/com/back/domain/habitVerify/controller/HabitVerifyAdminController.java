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
import com.back.domain.habitVerify.dto.BulkActionRequest;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HabitVerifyAdminController {

    private final HabitVerifyService habitVerifyService;

    @GetMapping("/groups/{groupId}/habits/verifications/pending")
    public List<HabitVerifySummaryResponse> getPending(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId
    ) {
        return habitVerifyService.getPendingByGroup(
                memberId, groupId
        );
    }

    @PatchMapping("/habits/verifications/{id}/approve")
    public HabitVerifyResponse approve(
            @LoginMemberId Long memberId,
            @PathVariable Long id
    ) {
        return habitVerifyService.approve(
                memberId, id
        );
    }

    @PatchMapping("/habits/verifications/{id}/reject")
    public HabitVerifyResponse reject(
            @LoginMemberId Long memberId,
            @PathVariable Long id
    ) {
        return habitVerifyService.reject(
                memberId, id
        );
    }

    @PatchMapping("/groups/{groupId}/habits/verifications/bulk-approve")
    public List<HabitVerifyResponse> bulkApprove(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId,
            @RequestBody BulkActionRequest request
    ) {
        return habitVerifyService.bulkApprove(
                memberId,
                groupId,
                request
        );
    }

    @PatchMapping("/groups/{groupId}/habits/verifications/bulk-reject")
    public List<HabitVerifyResponse> bulkReject(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId,
            @RequestBody BulkActionRequest request
    ) {
        return habitVerifyService.bulkReject(
                memberId,
                groupId,
                request
        );
    }
}