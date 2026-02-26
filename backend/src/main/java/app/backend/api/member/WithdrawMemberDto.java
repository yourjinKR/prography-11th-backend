package app.backend.api.member;

import app.backend.domain.member.MemberStatus;

import java.time.Instant;

public record WithdrawMemberDto(
		Long id,
		String loginId,
		String name,
		MemberStatus status,
		Instant updatedAt
) {
}
