package com.back.domain.habit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.group.service.GroupService;
import com.back.domain.groupMember.entity.GroupMember;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.dto.CreateHabitRequest;
import com.back.domain.habit.entity.Habit;
import com.back.domain.habit.entity.HabitStatus;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

// 개발용 파일 DB(db_dev)를 건드리지 않도록 메모리 DB를 쓰는 test 프로필로 실행한다.
@SpringBootTest
@ActiveProfiles("test")
class HabitServiceConcurrencyTest {

    private static final int THREAD_COUNT = 8;

    @Autowired private HabitService habitService;
    @Autowired private GroupService groupService;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMemberRepository groupMemberRepository;
    @Autowired private HabitRepository habitRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Member owner;
    private Long groupId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = memberRepository.save(
                Member.create("habit-" + suffix, "habit-" + suffix, passwordEncoder.encode("pw1234!")));

        GroupResponse.Detail created = groupService.createGroup(
                owner.getId(),
                new GroupRequest.Create("습관 동시성 그룹", "설명", LocalDate.now().plusDays(7), "벌칙", "pw1234", 5));
        groupId = created.id();
    }

    @AfterEach
    void tearDown() {
        List<GroupMember> members = groupMemberRepository.findAll().stream()
                .filter(gm -> gm.getGroup().getId().equals(groupId))
                .toList();
        habitRepository.deleteAll(
                habitRepository.findAllByGroupMember_Group_IdAndStatus(groupId, HabitStatus.ACTIVE));
        groupMemberRepository.deleteAll(members);
        groupRepository.deleteById(groupId);
        memberRepository.deleteById(owner.getId());
    }

    @Test
    @DisplayName("같은 멤버가 동시에 습관을 여러 번 생성해도 활성 습관은 1개만 만들어진다")
    void 동시에_습관을_생성해도_활성_습관은_하나만_생긴다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();
        AtomicInteger unexpectedCount = new AtomicInteger();

        CreateHabitRequest request = new CreateHabitRequest("동시성 습관", "설명", 3);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    habitService.createHabit(groupId, owner.getId(), request);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    unexpectedCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        List<Habit> activeHabits =
                habitRepository.findAllByGroupMember_Group_IdAndStatus(groupId, HabitStatus.ACTIVE);

        assertThat(unexpectedCount.get()).isZero();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(THREAD_COUNT - 1);
        assertThat(activeHabits).hasSize(1);

        // 활성 습관이 중복되면 단건 조회가 예외(500)를 내던 문제의 회귀 확인
        assertThatCode(() -> habitService.getActiveHabit(owner.getId(), groupId)).doesNotThrowAnyException();
    }
}
