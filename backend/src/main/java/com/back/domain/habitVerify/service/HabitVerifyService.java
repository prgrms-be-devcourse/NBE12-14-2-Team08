package com.back.domain.habitVerify.service;

import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.dto.HabitVerifyRequest;
import com.back.domain.habitVerify.dto.HabitVerifyResponse;
import com.back.domain.habitVerify.entity.HabitVerify;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitVerifyService {

    private final HabitRepository habitRepository;
    private final HabitVerifyRepository habitVerifyRepository;

    @Transactional
    public HabitVerifyResponse create(
            Long habitId,
            HabitVerifyRequest request,
            Long memberId

    ) {

        Habit habit = habitRepository.findByIdAndGroupMember_Member_Id(habitId, memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 습관입니다."
                        )
                );

        boolean alreadyExists =
                habitVerifyRepository.existsByHabitIdAndVerifyDate(
                        habitId,
                        request.verifyDate()
                );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "해당 날짜에 이미 인증 기록이 있습니다."
            );
        }

        HabitVerify habitVerify = HabitVerify.create(
                habit, request.verifyDate(),
                request.status(), request.description(),
                request.imageUrl()
        );

        HabitVerify savedHabitVerify =
                habitVerifyRepository.save(habitVerify);

        return HabitVerifyResponse.from(savedHabitVerify);
    }

    @Transactional(readOnly = true)
    public List<HabitVerifyResponse> findAll(
            Long habitId, Long memberId
    ){ habitRepository.findByIdAndGroupMember_Member_Id(
            habitId,
            memberId
    ).orElseThrow(() ->
            new IllegalArgumentException("권한이 없는 습관입니다.")
    );
        return habitVerifyRepository
                .findAllByHabitIdOrderByVerifyDateDesc(habitId)
                .stream()
                .map(HabitVerifyResponse::from)
                .toList();
    }

    @Transactional
    public HabitVerifyResponse update(
            Long habitId,
            Long verificationId,
            HabitVerifyRequest request,
            Long memberId
    ) {

        HabitVerify habitVerify =
                habitVerifyRepository
                        .findByIdAndHabitId(
                                verificationId,
                                habitId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인증 기록입니다."
                                )
                        );

        boolean alreadyExists =
                habitVerifyRepository
                        .existsByHabitIdAndVerifyDateAndIdNot(
                                habitId,
                                request.verifyDate(),
                                verificationId
                        );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "해당 날짜에 이미 인증 기록이 있습니다."
            );
        }

        habitVerify.update(
                request.verifyDate(),
                request.status(),
                request.description(),
                request.imageUrl()
        );

        return HabitVerifyResponse.from(habitVerify);
    }

    @Transactional
    public void delete(
            Long habitId,
            Long verificationId,
            Long memberId
    ) {
        habitRepository.findByIdAndGroupMember_Member_Id(
                habitId,
                memberId
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "권한이 없는 습관입니다."
                )
        );
        HabitVerify habitVerify =
                habitVerifyRepository
                        .findByIdAndHabitId(
                                verificationId,
                                habitId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인증 기록입니다."
                                )
                        );

        habitVerifyRepository.delete(habitVerify);
    }
}