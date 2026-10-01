package com.back.global.storage;

import com.back.domain.habit.repository.HabitRepository;
import com.back.global.security.LoginMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final HabitRepository habitRepository;

    @PostMapping("/habits/{habitId}/upload-url")
    public UploadUrlResponse createUploadUrl(
            @PathVariable Long habitId,
            @RequestBody UploadUrlRequest request,
            @LoginMemberId Long memberId
    ) {
        habitRepository.findByIdAndGroupMember_Member_Id(
                habitId,
                memberId
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "권한이 없는 습관입니다."
                )
        );

        return storageService.createHabitUploadUrl(
                memberId,
                habitId,
                request.filename()
        );
    }
}