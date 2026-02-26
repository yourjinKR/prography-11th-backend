package app.backend.api.member;

import app.backend.domain.member.Member;
import app.backend.domain.member.MemberRole;
import app.backend.domain.member.MemberStatus;

import java.time.Instant;

public record AdminMemberDto(
		Long id,
		String loginId,
		String name,
		String phone,
		MemberStatus status,
		MemberRole role,
		Integer generation,
		String partName,
		String teamName,
		Integer deposit,
		Instant createdAt,
		Instant updatedAt
) {
	public static AdminMemberDto of(Member member, Integer generation, String partName, String teamName, Integer deposit) {
		return new AdminMemberDto(
				member.getId(),
				member.getLoginId(),
				member.getName(),
				member.getPhone(),
				member.getStatus(),
				member.getRole(),
				generation,
				partName,
				teamName,
				deposit,
				member.getCreatedAt(),
				member.getUpdatedAt()
		);
	}
}
