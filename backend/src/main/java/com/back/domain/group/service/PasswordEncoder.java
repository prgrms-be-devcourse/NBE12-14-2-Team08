package com.back.domain.group.service;

import org.springframework.stereotype.Component;

// 임시 시큐리티
@Component
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}