package com.back.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MockAuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler){
        // 임시로 사용할 헤더 (예: X-User-Id)
        String userId = request.getHeader("X-User-Id");

        // 헤더가 없으면 기본 유저(1L)로 처리
        long memberId = (userId != null && !userId.isBlank()) ? Long.parseLong(userId) : 1L;
        request.setAttribute("loginMemberId", memberId);
        return true;
    }
}
