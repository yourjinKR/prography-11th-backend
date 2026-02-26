package app.backend.api.session;

import app.backend.domain.session.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateSessionRequest(
		String title,
		LocalDate date,
		LocalTime time,
		String location,
		SessionStatus status
) {
}
