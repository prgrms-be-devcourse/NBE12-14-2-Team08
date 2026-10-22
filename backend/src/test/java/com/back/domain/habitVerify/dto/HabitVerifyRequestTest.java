package com.back.domain.habitVerify.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HabitVerifyRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("사진 URL이 없거나 비어 있으면 검증에 실패한다 (습관 인증 사진은 필수)")
    void 사진_없음(String imageUrl) {
        Set<ConstraintViolation<HabitVerifyRequest>> violations =
                validator.validate(new HabitVerifyRequest("인증", imageUrl));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage)
                .containsExactly(tuple("imageUrl", "인증 사진은 필수입니다."));
    }

    @Test
    @DisplayName("사진 URL이 있으면 설명이 없어도 검증을 통과한다")
    void 사진_있음() {
        Set<ConstraintViolation<HabitVerifyRequest>> violations =
                validator.validate(new HabitVerifyRequest(null, "https://storage/a.png"));

        assertThat(violations).isEmpty();
    }
}
