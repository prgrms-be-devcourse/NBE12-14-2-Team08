package com.back.domain.groupMember.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.dto.GroupResponse;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.group.service.GroupService;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.habit.repository.HabitRepository;
import com.back.domain.habitVerify.repository.HabitVerifyRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.penaltyverify.repository.PenaltyVerifyRepository;
import com.back.global.exception.GroupLimitExceededException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class GroupMemberServiceConcurrencyTest {

    @Autowired
    private GroupMemberService groupMemberService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private HabitRepository habitRepository;
    @Autowired
    private HabitVerifyRepository habitVerifyRepository;
    @Autowired
    private PenaltyVerifyRepository penaltyVerifyRepository;

    private Group group;
    private List<Member> applicants;
    private static final int MEMBER_LIMIT = 5;
    private static final int APPLICANT_COUNT = 10;
    private static final String RAW_PASSWORD = "test1234";

    @BeforeEach
    void setUp() {
        String uniqueInviteCode = "concur-" + UUID.randomUUID().toString().substring(0, 8);

        // 방장이 될 회원 생성
        Member owner = memberRepository.save(
            Member.create("ownerUser-" + uniqueInviteCode, "ownerUser-" + uniqueInviteCode, passwordEncoder.encode("pw1234!")));

        // 실제 서비스 로직으로 그룹 생성 → 방장 GroupMember가 함께 자동 생성됨
        GroupRequest.Create createRequest = new GroupRequest.Create(
            "동시성 테스트 그룹", "설명", LocalDate.now().plusDays(7), "벌칙",
            RAW_PASSWORD, MEMBER_LIMIT);
        GroupResponse.Detail created = groupService.createGroup(owner.getId(), createRequest);

        group = groupRepository.findById(created.id()).orElseThrow();

        // createGroup은 초대코드를 자체 생성하므로, 실제 발급된 코드를 applicants 쪽에도 맞춰 씀
        applicants = IntStream.range(0, APPLICANT_COUNT)
            .mapToObj(i -> memberRepository.save(
                Member.create("concurUser" + i + "-" + uniqueInviteCode,
                    "concurUser" + i + "-" + uniqueInviteCode,
                    passwordEncoder.encode("pw1234!"))))
            .toList();
    }

    @AfterEach
    void tearDown() {
        penaltyVerifyRepository.deleteAll();
        habitVerifyRepository.deleteAll();
        habitRepository.deleteAll();
        groupMemberRepository.deleteAll();
        groupRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void 동시에_정원을_초과해_입장해도_방장을_포함해_정확히_정원만큼만_성공한다() throws InterruptedException {
        int expectedSuccess = MEMBER_LIMIT - 1;  // 방장 1명이 이미 자리를 차지하고 있으므로

        int threadCount = APPLICANT_COUNT;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        GroupRequest.Join joinRequest = new GroupRequest.Join(RAW_PASSWORD);

        for (Member applicant : applicants) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    groupMemberService.joinGroup(group.getInviteCode(), applicant.getId(), joinRequest);
                    successCount.incrementAndGet();
                } catch (GroupLimitExceededException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
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

        assertThat(successCount.get()).isEqualTo(expectedSuccess);
        assertThat(failCount.get()).isEqualTo(APPLICANT_COUNT - expectedSuccess);

        long actualCount = groupMemberRepository.countByGroupId(group.getId());
        assertThat(actualCount).isEqualTo(MEMBER_LIMIT);  // 방장 1 + 성공한 4명 = 5
    }
}