package com.back.domain.member.service;

import com.back.domain.group.entity.GroupStatus;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberStatus;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.dto.CreateMemberRequest;
import com.back.domain.member.dto.LoginRequest;
import com.back.domain.member.dto.MemberResponse;
import com.back.domain.member.dto.UpdateMemberRequest;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberStatus;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.back.domain.group.entity.Group;
import java.util.NoSuchElementException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;
    private final AuthTokenService authTokenService;

    // 비밀번호는 해시로 저장하고, 회원 ID가 발급된 뒤 재발급용 토큰을 생성한다.
    // 현재 정책은 가입 시 저장한 Refresh Token을 로그인 시 다시 반환하는 방식이다.
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

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new UnauthorizedException("탈퇴한 회원은 로그인할 수 없습니다.");
        }

        return member.getId();
    }

    public MemberResponse getMember(Long memberId) {
        Member member = findMember(memberId);
        return MemberResponse.from(member);
    }

    // 닉네임만 수정할 때는 비밀번호 대조가 필요 없으며, 새 비밀번호가 있을 때만 대조한다.
    // 예외가 발생하면 같은 트랜잭션에서 앞서 변경한 닉네임도 함께 롤백된다.
    @Transactional
    public MemberResponse updateMember(Long memberId, UpdateMemberRequest request) {
        Member member = findMember(memberId);

        if (request.nickname() != null && !request.nickname().isBlank()) {
            member.updateNickname(request.nickname());
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new IllegalArgumentException("기존 비밀번호를 입력해주세요.");
            }

            if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
                throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
            }

            String encodedPassword = passwordEncoder.encode(request.newPassword());
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

        return member.getStatus() == MemberStatus.ACTIVE && member.matchesRefreshToken(refreshToken);
    }

    // 실제 회원 행을 삭제하지 않고 WITHDRAWN 상태로 전환해 기존 기록의 연결을 유지한다.
    // 진행 중인 방에서 먼저 나와야 탈퇴할 수 있고, 완료된 방의 참여 정보는 leave 처리한다.
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = findMember(memberId);

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다.");
        }

        List<GroupMember> groupMembers =
                groupMemberRepository.findAllByMemberId(memberId);
        LocalDate today = LocalDate.now();

        boolean hasActiveGroup = groupMembers.stream()
                .anyMatch(gm -> gm.getStatus() == GroupMemberStatus.ACTIVE
                        && isActiveGroup(gm.getGroup(), today));

        if (hasActiveGroup) {
            throw new IllegalStateException(
                    "진행 중인 방이 있어 회원 탈퇴를 할 수 없습니다."
            );
        }

        groupMembers.stream()
                .filter(gm -> !isActiveGroup(gm.getGroup(), today))
                .forEach(GroupMember::leave);

        member.withdraw();
    }

    // 종료 스케줄러가 아직 실행되지 않아 상태가 ACTIVE여도, 날짜가 지났으면 진행 방에서 제외한다.
    // 마감 당일은 여전히 진행 중이며, deadline이 없는 방도 진행 중으로 취급한다.
    private boolean isActiveGroup(Group group, LocalDate today) {
        return group.getStatus() == GroupStatus.ACTIVE
                && (group.getDeadline() == null
                || !today.isAfter(group.getDeadline()));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));
    }
}
