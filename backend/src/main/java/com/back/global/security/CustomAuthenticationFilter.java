package com.back.global.security;

import com.back.domain.member.service.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthTokenService authTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = resolveAccessToken(request);

        if (accessToken != null
                && authTokenService.validateToken(accessToken)
                && authTokenService.isAccessToken(accessToken)) {

            Long memberId =
                    authTokenService.getMemberId(accessToken);

            request.setAttribute("loginMemberId", memberId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,
                            List.of()
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(
            HttpServletRequest request
    ) {
        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);


        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }


        return authorizationHeader.substring(
                BEARER_PREFIX.length()
        );
    }
}