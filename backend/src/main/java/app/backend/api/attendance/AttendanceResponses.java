package app.backend.api.attendance;

import app.backend.domain.attendance.Attendance;
import app.backend.domain.attendance.AttendanceStatus;
import app.backend.domain.deposit.DepositType;

import java.time.Instant;
import java.util.List;

public class AttendanceResponses {

	private AttendanceResponses() {
	}

	public record AttendanceResponse(
			Long id,
			Long sessionId,
			Long memberId,
			AttendanceStatus status,
			Integer lateMinutes,
			Integer penaltyAmount,
			String reason,
			Instant checkedInAt,
			Instant createdAt,
			Instant updatedAt
	) {
		public static AttendanceResponse from(Attendance attendance) {
			return new AttendanceResponse(
					attendance.getId(),
					attendance.getSession().getId(),
					attendance.getMember().getId(),
					attendance.getStatus(),
					attendance.getLateMinutes(),
					attendance.getPenaltyAmount(),
					attendance.getReason(),
					attendance.getCheckedInAt(),
					attendance.getCreatedAt(),
					attendance.getUpdatedAt()
			);
		}
	}

	public record MemberAttendanceRecordResponse(
			Long id,
			Long sessionId,
			String sessionTitle,
			AttendanceStatus status,
			Integer lateMinutes,
			Integer penaltyAmount,
			String reason,
			Instant checkedInAt,
			Instant createdAt
	) {
		public static MemberAttendanceRecordResponse from(Attendance attendance) {
			return new MemberAttendanceRecordResponse(
					attendance.getId(),
					attendance.getSession().getId(),
					attendance.getSession().getTitle(),
					attendance.getStatus(),
					attendance.getLateMinutes(),
					attendance.getPenaltyAmount(),
					attendance.getReason(),
					attendance.getCheckedInAt(),
					attendance.getCreatedAt()
			);
		}
	}

	public record MemberAttendanceSummaryResponse(
			Long memberId,
			int present,
			int absent,
			int late,
			int excused,
			int totalPenalty,
			Integer deposit
	) {
	}

	public record AdminSessionAttendanceSummaryItem(
			Long memberId,
			String memberName,
			int present,
			int absent,
			int late,
			int excused,
			int totalPenalty,
			int deposit
	) {
	}

	public record AdminMemberAttendanceDetailResponse(
			Long memberId,
			String memberName,
			Integer generation,
			String partName,
			String teamName,
			Integer deposit,
			Integer excuseCount,
			List<AttendanceResponse> attendances
	) {
	}

	public record AdminSessionAttendancesResponse(
			Long sessionId,
			String sessionTitle,
			List<AttendanceResponse> attendances
	) {
	}

	public record DepositHistoryResponse(
			Long id,
			Long cohortMemberId,
			DepositType type,
			Integer amount,
			Integer balanceAfter,
			Long attendanceId,
			String description,
			Instant createdAt
	) {
	}
}
