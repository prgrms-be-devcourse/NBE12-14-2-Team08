package com.back.domain.penaltyverify.controller;

import com.back.domain.penaltyverify.dto.BulkActionRequest;
import com.back.domain.penaltyverify.dto.PenaltyVerifyDetailResponse;
import com.back.domain.penaltyverify.dto.PenaltyVerifySummaryResponse;
import com.back.domain.penaltyverify.dto.SubmitPenaltyVerifyRequest;
import com.back.domain.penaltyverify.service.PenaltyVerifyService;
import com.back.global.security.LoginMemberId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PenaltyVerifyController {

    private final PenaltyVerifyService penaltyVerifyService;


    @PostMapping("/habits/{habitId}/penalties")
    public ResponseEntity<PenaltyVerifyDetailResponse> submit(
        @LoginMemberId Long memberId,
        @PathVariable Long habitId,
        @Valid @RequestBody SubmitPenaltyVerifyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(penaltyVerifyService.submit(memberId, habitId, request));
    }

    @GetMapping("/penalties/{id}")
    public PenaltyVerifyDetailResponse getDetail(@PathVariable Long id) {
        return penaltyVerifyService.getDetail(id);
    }

    @GetMapping("/groups/{groupId}/penalties/count")
    public ResponseEntity<Map<String, Long>> getCount(@PathVariable Long groupId,
        @LoginMemberId Long memberId){
        long count =  penaltyVerifyService.getCount(groupId, memberId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/groups/{groupId}/members/{groupMemberId}/penalties")
    public List<PenaltyVerifySummaryResponse> getPenaltyList(
        @PathVariable Long groupId, @PathVariable Long groupMemberId
    ){
        return penaltyVerifyService.getPenaltiesByGroupMember(groupMemberId);
    }

    // 단건 승인
    @PatchMapping("/penalties/{id}/approve")
    public PenaltyVerifyDetailResponse approve(
        @LoginMemberId Long memberId,
        @PathVariable Long id
    ) {
        return penaltyVerifyService.approve(memberId, id);
    }

    // 단건 거절
    @PatchMapping("/penalties/{id}/reject")
    public PenaltyVerifyDetailResponse reject(
        @LoginMemberId Long memberId,
        @PathVariable Long id
    ) {
        return penaltyVerifyService.reject(memberId, id);
    }

    @GetMapping("/groups/{groupId}/penalties/pending")
    public List<PenaltyVerifySummaryResponse> getPending(
        @LoginMemberId Long memberId,
        @PathVariable Long groupId) {

        return penaltyVerifyService.getPendingByGroup(memberId, groupId);
    }

    @PatchMapping("/groups/{groupId}/penalties/bulk-approve")
    public List<PenaltyVerifyDetailResponse> bulkApprove(
        @LoginMemberId Long memberId,
        @PathVariable Long groupId,
        @RequestBody BulkActionRequest request
    ) {
        return penaltyVerifyService.bulkApprove(memberId, groupId, request);
    }

    @PatchMapping("/groups/{groupId}/penalties/bulk-reject")
    public List<PenaltyVerifyDetailResponse> bulkReject(
        @LoginMemberId Long memberId,
        @PathVariable Long groupId,
        @RequestBody BulkActionRequest request
    ) {
        return penaltyVerifyService.bulkReject(memberId, groupId, request);
    }

    @DeleteMapping("/penalties/{id}")
    public ResponseEntity<Void> delete(
        @LoginMemberId Long memberId,
        @PathVariable Long id) {
        penaltyVerifyService.delete(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
