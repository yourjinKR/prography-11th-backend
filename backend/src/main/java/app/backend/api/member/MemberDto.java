package app.backend.api.member;

import app.backend.domain.member.Member;
import app.backend.domain.member.MemberRole;
import app.backend.domain.member.MemberStatus;

import java.time.Instant;

public record MemberDto(
		Long id,
		String loginId,
		String name,
		String phone,
		MemberStatus status,
		MemberRole role,
		Instant createdAt,
		Instant updatedAt
) {
	public static MemberDto from(Member member) {
		return new MemberDto(
				member.getId(),
				member.getLoginId(),
				member.getName(),
				member.getPhone(),
				member.getStatus(),
				member.getRole(),
				member.getCreatedAt(),
				member.getUpdatedAt()
		);
	}
}
