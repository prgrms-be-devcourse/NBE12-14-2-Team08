package com.back.domain.habit.controller;

import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.dto.HabitResponse;
import com.back.domain.habit.dto.UpdateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.service.HabitService;
import com.back.domain.penaltyverify.service.PenaltyVerifyService;
import com.back.global.security.LoginMemberId;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/habits/fail/{habitId}")
    public ResponseEntity<Void> failHabit(
            @LoginMemberId Long memberId,
            @PathVariable Long habitId
    ) {
        habitService.failHabit(
                memberId,
                habitId
        );

        return ResponseEntity.ok().build();
    }


    @GetMapping("/habits/{id}")
    public HabitResponse detail(@PathVariable Long id) {
        Habit habit = habitService.findById(id).get();

        return new HabitResponse(habit);
    }

    @GetMapping("/groups/{groupId}/habits/active")
    public ResponseEntity<HabitResponse> getActiveHabit(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(
                habitService.getActiveHabit(memberId, groupId)
        );
    }

    @GetMapping("/groups/{groupId}/habits/fail")
    public ResponseEntity<List<HabitResponse>> getFailedHabits(
            @LoginMemberId Long memberId,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(
                habitService.getFailedHabits(memberId, groupId)
        );
    }

}
