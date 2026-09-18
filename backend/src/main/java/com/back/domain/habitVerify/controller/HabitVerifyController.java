package com.back.domain.habitVerify.controller;

import com.back.domain.habitVerify.dto.HabitVerifyRequest;
import com.back.domain.habitVerify.dto.HabitVerifyResponse;
import com.back.domain.habitVerify.service.HabitVerifyService;
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
            @Valid @RequestBody HabitVerifyRequest request
    ) {

        HabitVerifyResponse response =
                habitVerifyService.create(
                        habitId, request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<HabitVerifyResponse>> findAll(
            @PathVariable Long habitId
    ) {

        return ResponseEntity.ok(
                habitVerifyService.findAll(habitId)
        );
    }

    @PutMapping("/{verificationId}")
    public ResponseEntity<HabitVerifyResponse> update(
            @PathVariable Long habitId,
            @PathVariable Long verificationId,
            @Valid @RequestBody HabitVerifyRequest request
    ) {

        return ResponseEntity.ok(
                habitVerifyService.update(
                        habitId,
                        verificationId,
                        request
                )
        );
    }

    @DeleteMapping("/{verificationId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long habitId,
            @PathVariable Long verificationId
    ) {

        habitVerifyService.delete(
                habitId,
                verificationId
        );

        return ResponseEntity.noContent().build();
    }
}