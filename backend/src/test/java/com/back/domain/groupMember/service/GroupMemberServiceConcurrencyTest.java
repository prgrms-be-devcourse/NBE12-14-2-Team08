package com.back.domain.groupMember.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.back.domain.group.dto.GroupRequest;
import com.back.domain.group.entity.Group;
import com.back.domain.group.repository.GroupRepository;
import com.back.domain.groupMember.repository.GroupMemberRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.GroupLimitExceededException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
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
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Group group;
    private List<Member> applicants;
    private static final int MEMBER_LIMIT = 5;
    private static final int APPLICANT_COUNT = 10;
    private static final String RAW_PASSWORD = "test1234";

    @BeforeEach
    void setUp() {
        // 정원 5명짜리 그룹 하나 생성 (방장 제외 여유는 기존 로직에 맞춰 조정)
        group = groupRepository.save(Group.builder()
            .title("동시성 테스트 그룹")
            .password(passwordEncoder.encode(RAW_PASSWORD))
            .inviteCode("concur01")
            .memberLimit(MEMBER_LIMIT)
            .build());

        // 입장 시도할 회원 10명 미리 생성
        applicants = IntStream.range(0, APPLICANT_COUNT)
            .mapToObj(i -> memberRepository.save(
                Member.create("concurUser" + i, "concurUser" + i, passwordEncoder.encode("pw1234!"))))
            .toList();
    }

    @Test
    void 동시에_정원을_초과해_입장해도_정확히_정원만큼만_성공한다() throws InterruptedException {
        int threadCount = APPLICANT_COUNT;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);   // 스레드 준비 동기화용
        CountDownLatch startLatch = new CountDownLatch(1);             // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        GroupRequest.Join joinRequest = new GroupRequest.Join(RAW_PASSWORD);

        for (Member applicant : applicants) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();  // 모든 스레드가 동시에 출발하도록
                    groupMemberService.joinGroup(group.getInviteCode(), applicant.getId(), joinRequest);
                    successCount.incrementAndGet();
                } catch (GroupLimitExceededException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet(); // 예상 못 한 예외는 로그로 구분해보는 게 좋음
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();      // 전원 대기 상태 확인
        startLatch.countDown();  // 한꺼번에 출발
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(MEMBER_LIMIT);
        assertThat(failCount.get()).isEqualTo(APPLICANT_COUNT - MEMBER_LIMIT);

        long actualCount = groupMemberRepository.countByGroupId(group.getId());
        assertThat(actualCount).isEqualTo(MEMBER_LIMIT);  // DB 실제 값까지 검증
    }
}