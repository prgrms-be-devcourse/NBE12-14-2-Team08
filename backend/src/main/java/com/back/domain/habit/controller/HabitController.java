package com.back.domain.habit.controller;

import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.dto.HabitResponse;
import com.back.domain.habit.dto.UpdateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.service.HabitService;
import com.back.global.security.LoginMemberId;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class HabitController {

    private final HabitService habitService;

    @PostMapping("/groups/{groupId}/habits")
    public HabitResponse createHabit(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateHabitRequest request
    ) {
        return habitService.createHabit(
                groupId,
                memberId,
                request
        );
    }

    @GetMapping("/habits/{id}")
    public HabitResponse detail(@PathVariable Long id) {
        Habit habit = habitService.findById(id).get();

        return new HabitResponse(habit);
    }

    @PatchMapping("/habits/{habitId}")
    public ResponseEntity<HabitResponse> updateHabit(
            @LoginMemberId Long memberId,
            @PathVariable Long habitId,
            @RequestBody UpdateHabitRequest request
    ) {
        HabitResponse response = habitService.updateHabit(
                habitId,
                memberId,
                request
        );

        return ResponseEntity.ok(response);
    }
}
