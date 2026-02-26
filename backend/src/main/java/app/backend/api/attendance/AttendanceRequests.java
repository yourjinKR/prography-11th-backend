package app.backend.api.attendance;

import app.backend.domain.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttendanceRequests {

	private AttendanceRequests() {
	}

	public record CheckInRequest(
			@NotBlank String hashValue,
			@NotNull Long memberId
	) {
	}

	public record AdminRegisterAttendanceRequest(
			@NotNull Long sessionId,
			@NotNull Long memberId,
			@NotNull AttendanceStatus status,
			Integer lateMinutes,
			String reason
	) {
	}

	public record AdminUpdateAttendanceRequest(
			@NotNull AttendanceStatus status,
			Integer lateMinutes,
			String reason
	) {
	}
}
