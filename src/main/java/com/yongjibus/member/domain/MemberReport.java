package com.yongjibus.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberReport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_member_id")
  private Member reportedMember;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private Long roomId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id")
  private Member reporter;

  @Builder
  public MemberReport(Member reportedMember, String reason, Long roomId, Member reporter) {
    this.reportedMember = reportedMember;
    this.reason = reason;
    this.roomId = roomId;
    this.reporter = reporter;
  }

  public void setReportedMember(Member reportedMember) {
    this.reportedMember = reportedMember;
  }
}