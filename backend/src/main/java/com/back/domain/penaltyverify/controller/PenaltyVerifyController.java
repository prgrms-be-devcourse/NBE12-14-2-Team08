package com.back.domain.penaltyverify.controller;

import com.back.domain.penaltyverify.dto.BulkActionRequest;
import com.back.domain.penaltyverify.dto.PenaltyVerifyDetailResponse;
import com.back.domain.penaltyverify.dto.PenaltyVerifySummaryResponse;
import com.back.domain.penaltyverify.dto.SubmitPenaltyVerifyRequest;
import com.back.domain.penaltyverify.service.PenaltyStorageService;
import com.back.domain.penaltyverify.service.PenaltyVerifyService;
import com.back.global.security.LoginMemberId;
import com.back.global.storage.UploadUrlRequest;
import com.back.global.storage.UploadUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    private final PenaltyStorageService penaltyStorageService;

    @PostMapping("/habits/{habitId}/penalties")
    public ResponseEntity<PenaltyVerifyDetailResponse> submit(
        @LoginMemberId Long memberId,
        @PathVariable Long habitId,
        @Valid @RequestBody SubmitPenaltyVerifyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(penaltyVerifyService.submit(memberId, habitId, request));
    }

    @Operation(summary = "벌칙 인증 사진 업로드용 서명 URL 발급")
    @PostMapping("/penalties/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(
        @LoginMemberId Long memberId,
        @Valid @RequestBody UploadUrlRequest request
    ) {
        UploadUrlResponse response = penaltyStorageService.createUploadUrl(memberId, request.filename());
        return ResponseEntity.ok(response);
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
        return penaltyVerifyService.getPenaltiesByGroupMember(groupId, groupMemberId);
    }

    // 단건 승인
    @PatchMapping("/penalties/{id}/approve")
    public PenaltyVerifyDetailResponse approve(
        @LoginMemberId Long memberId,
        @PathVariable Long id
    ) {
        return penaltyVerifyService.approve(memberId, id);
    }

    @PatchMapping("/penalties/{id}/reject")
    public PenaltyVerifyDetailResponse reject(
        @LoginMemberId Long memberId,
        @PathVariable Long id
    ) {
        return penaltyVerifyService.reject(memberId, id);
    }

    @GetMapping("/groups/{groupId}/penalties/pending")
    public List<PenaltyVerifySummaryResponse> getPending(@PathVariable Long groupId) {
        return penaltyVerifyService.getPendingByGroup(groupId);
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
