package com.back.domain.member.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(
            name = "refresh_token",
            unique = true,
            length = 512
    )
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    private Member(String nickname, String username, String password){
        this.nickname = nickname;
        this.username = username;
        this.password = password;
    }

    public static Member create(String nickname, String username, String encodedPassword) {
        return new Member(nickname, username, encodedPassword);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword){
        this.password = encodedPassword;
    }
    
    // 회원가입 시 생성된 리프레시 토큰 저장
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    // 재발급 요청의 리프레시 토큰과 DB 저장값 비교
    public boolean matchesRefreshToken(String refreshToken) {
        return this.refreshToken != null
                && this.refreshToken.equals(refreshToken);
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
        this.refreshToken = null;
    }
}