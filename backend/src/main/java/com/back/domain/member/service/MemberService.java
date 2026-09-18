package com.back.domain.member.service;

import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.dto.CreateMemberRequest;
import com.back.domain.member.dto.LoginRequest;
import com.back.domain.member.dto.MemberResponse;
import com.back.domain.member.dto.UpdateMemberRequest;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;
    private final AuthTokenService authTokenService;

    @Transactional
    public MemberResponse join(CreateMemberRequest request) {
        if (memberRepository.existsByUsername(request.username())) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.create(
                request.nickname(),
                request.username(),
                encodedPassword
        );

        Member savedMember = memberRepository.save(member);

        String refreshToken =
                authTokenService.createRefreshToken(
                        savedMember.getId()
                );

        savedMember.updateRefreshToken(refreshToken);

        return MemberResponse.from(savedMember);
    }

    public Long authenticate(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(
                        () -> new UnauthorizedException(
                                "아이디 또는 비밀번호가 일치하지 않습니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                member.getPassword()
        )) {
            throw new UnauthorizedException(
                    "아이디 또는 비밀번호가 일치하지 않습니다."
            );
        }

        return member.getId();
    }

    public MemberResponse getMember(Long memberId) {
        Member member = findMember(memberId);
        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = findMember(memberId);

        if (request.nickname() != null && !request.nickname().isBlank()) {
            member.updateNickname(request.nickname());
        }

        if (request.password() != null && !request.password().isBlank()) {
            String encodedPassword = passwordEncoder.encode(request.password());
            member.updatePassword(encodedPassword);
        }

        return MemberResponse.from(member);
    }

    // 로그인 시 DB에 저장된 리프레시 토큰 조회
    public String getRefreshToken(Long memberId) {
        Member member = findMember(memberId);

        String refreshToken = member.getRefreshToken();

        if (refreshToken == null
                || refreshToken.isBlank()) {
            throw new UnauthorizedException(
                    "저장된 리프레시 토큰이 없습니다."
            );
        }

        return refreshToken;
    }

    // 재발급 요청의 리프레시 토큰과 DB 저장값 비교
    public boolean matchesRefreshToken(
            Long memberId,
            String refreshToken
    ) {
        Member member = findMember(memberId);

        return member.matchesRefreshToken(refreshToken);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member member = findMember(memberId);

        if (groupMemberRepository.existsByMemberId(memberId)) {
            throw new IllegalStateException(
                    "참여 중인 방이 있어 회원 탈퇴를 할 수 없습니다."
            );
        }

        memberRepository.delete(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));
    }
}