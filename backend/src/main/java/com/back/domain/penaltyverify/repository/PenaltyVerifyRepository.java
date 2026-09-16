package com.back.domain.penaltyverify.repository;

import com.back.domain.penaltyverify.entity.PenaltyVerify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PenaltyVerifyRepository extends JpaRepository<PenaltyVerify, Long> {

}
