package com.back.domain.habitVerify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.back.domain.group.entity.Group;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.entity.GroupMemberRole;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.global.exception.BusinessRuleException;
import com.back.global.exception.ForbiddenException;
import com.back.global.storage.StorageService;
import com.back.global.storage.UploadUrlResponse;
import com.back.global.storage.VerificationImageType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HabitVerifyServiceUploadTest {

    @Mock
    private HabitRepository habitRepository;
    @Mock
    private HabitVerifyRepository habitVerifyRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private HabitVerifyService habitVerifyService;

    private Group group;
    private Habit habit;

    @BeforeEach
    void setUp() {
        Member member = Member.create("일반회원", "member", "pass1234");
        ReflectionTestUtils.setField(member, "id", 2L);

        group = Group.builder()
            .title("아침 기상 챌린지")
            .penalty("커피 1잔 쏘기")
            .memberLimit(10)
            .build();

        GroupMember groupMember = GroupMember.create(group, member, GroupMemberRole.MEMBER);
        habit = Habit.create(groupMember, "기상 후 스트레칭", "10분 스트레칭", 3);
        ReflectionTestUtils.setField(habit, "id", 50L);
    }

    @Test
    @DisplayName("내 진행 중인 습관이면 습관 사진 업로드 URL을 발급한다")
    void 업로드_URL_발급_성공() {
        UploadUrlResponse expected = new UploadUrlResponse("https://upload", "https://public/2/50/a.png");
        given(habitRepository.findByIdAndGroupMember_Member_Id(50L, 2L)).willReturn(Optional.of(habit));
        given(storageService.createUploadUrl(VerificationImageType.HABIT, 2L, 50L, "today.png")).willReturn(expected);

        UploadUrlResponse response = habitVerifyService.createUploadUrl(2L, 50L, "today.png");

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("내 습관이 아니면 ForbiddenException (403)")
    void 내_습관_아님_403() {
        given(habitRepository.findByIdAndGroupMember_Member_Id(50L, 999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> habitVerifyService.createUploadUrl(999L, 50L, "today.png"))
            .isInstanceOf(ForbiddenException.class);

        verify(storageService, never()).createUploadUrl(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("실패(FAILED)한 습관이면 IllegalStateException (409)")
    void 실패한_습관_409() {
        habit.fail();
        given(habitRepository.findByIdAndGroupMember_Member_Id(50L, 2L)).willReturn(Optional.of(habit));

        assertThatThrownBy(() -> habitVerifyService.createUploadUrl(2L, 50L, "today.png"))
            .isInstanceOf(IllegalStateException.class);

        verify(storageService, never()).createUploadUrl(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("종료된 방이면 BusinessRuleException (400)")
    void 종료된_방_400() {
        group.finish();
        given(habitRepository.findByIdAndGroupMember_Member_Id(50L, 2L)).willReturn(Optional.of(habit));

        assertThatThrownBy(() -> habitVerifyService.createUploadUrl(2L, 50L, "today.png"))
            .isInstanceOf(BusinessRuleException.class);

        verify(storageService, never()).createUploadUrl(any(), anyLong(), anyLong(), anyString());
    }
}
