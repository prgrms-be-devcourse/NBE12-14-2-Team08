package com.back.domain.habit.controller;

import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.service.HabitService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/habits")
@SecurityRequirement(name = "bearerAuth")
public class HabitController {

    private final HabitService habitService;

    @GetMapping("/{id")
    public void detail(@PathVariable Long id) {
        Habit habit = habitService.findById(id).get();


    }
}
