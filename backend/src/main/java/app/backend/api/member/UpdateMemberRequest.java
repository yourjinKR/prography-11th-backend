package app.backend.api.member;

public record UpdateMemberRequest(
		String name,
		String phone,
		Long cohortId,
		Long partId,
		Long teamId
) {
}
