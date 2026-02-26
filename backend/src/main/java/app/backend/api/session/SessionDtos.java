package app.backend.api.session;

import app.backend.domain.session.Session;
import app.backend.domain.session.SessionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class SessionDtos {

	private SessionDtos() {
	}

	public record SessionSummaryDto(
			Long id,
			String title,
			LocalDate date,
			LocalTime time,
			String location,
			SessionStatus status,
			Instant createdAt,
			Instant updatedAt
	) {
		public static SessionSummaryDto from(Session session) {
			return new SessionSummaryDto(
					session.getId(),
					session.getTitle(),
					session.getDate(),
					session.getTime(),
					session.getLocation(),
					session.getStatus(),
					session.getCreatedAt(),
					session.getUpdatedAt()
			);
		}
	}

	public record AttendanceSummary(
			int present,
			int absent,
			int late,
			int excused,
			int total
	) {
	}

	public record AdminSessionDto(
			Long id,
			Long cohortId,
			String title,
			LocalDate date,
			LocalTime time,
			String location,
			SessionStatus status,
			AttendanceSummary attendanceSummary,
			boolean qrActive,
			Instant createdAt,
			Instant updatedAt
	) {
	}

	public record QrCodeDto(
			Long id,
			Long sessionId,
			String hashValue,
			Instant createdAt,
			Instant expiresAt
	) {
	}
}
