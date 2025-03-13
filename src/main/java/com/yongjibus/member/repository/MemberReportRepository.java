package com.yongjibus.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yongjibus.member.domain.MemberReport;

public interface MemberReportRepository extends JpaRepository<MemberReport, Long> {
  
}
