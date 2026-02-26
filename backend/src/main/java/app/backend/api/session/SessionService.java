package app.backend.api.session;

import app.backend.domain.attendance.Attendance;
import app.backend.domain.attendance.AttendanceRepository;
import app.backend.domain.attendance.AttendanceStatus;
import app.backend.domain.cohort.Cohort;
import app.backend.domain.cohort.CurrentCohortResolver;
import app.backend.domain.session.QrCode;
import app.backend.domain.session.QrCodeRepository;
import app.backend.domain.session.Session;
import app.backend.domain.session.SessionRepository;
import app.backend.domain.session.SessionStatus;
import app.backend.global.error.ApiException;
import app.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

	private final SessionRepository sessionRepository;
	private final QrCodeRepository qrCodeRepository;
	private final AttendanceRepository attendanceRepository;
	private final CurrentCohortResolver currentCohortResolver;

	public SessionService(
			SessionRepository sessionRepository,
			QrCodeRepository qrCodeRepository,
			AttendanceRepository attendanceRepository,
			CurrentCohortResolver currentCohortResolver
	) {
		this.sessionRepository = sessionRepository;
		this.qrCodeRepository = qrCodeRepository;
		this.attendanceRepository = attendanceRepository;
		this.currentCohortResolver = currentCohortResolver;
	}

	@Transactional(readOnly = true)
	public List<SessionDtos.SessionSummaryDto> getMemberSessions() {
		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		return sessionRepository.findByCohortId(cohort.getId()).stream()
				.filter(session -> session.getStatus() != SessionStatus.CANCELLED)
				.map(SessionDtos.SessionSummaryDto::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SessionDtos.AdminSessionDto> getAdminSessions(LocalDate dateFrom, LocalDate dateTo, SessionStatus status) {
		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		List<Session> sessions = sessionRepository.findByCohortId(cohort.getId());
		return sessions.stream()
				.filter(session -> dateFrom == null || !session.getDate().isBefore(dateFrom))
				.filter(session -> dateTo == null || !session.getDate().isAfter(dateTo))
				.filter(session -> status == null || session.getStatus() == status)
				.map(this::toAdminDto)
				.toList();
	}

	@Transactional
	public SessionDtos.AdminSessionDto create(CreateSessionRequest request) {
		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		Session session = sessionRepository.save(new Session(
				cohort,
				request.title(),
				request.date(),
				request.time(),
				request.location(),
				SessionStatus.SCHEDULED
		));
		createQrCode(session);
		return toAdminDto(session);
	}

	@Transactional
	public SessionDtos.AdminSessionDto update(Long sessionId, UpdateSessionRequest request) {
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
		if (session.getStatus() == SessionStatus.CANCELLED) {
			throw new ApiException(ErrorCode.SESSION_ALREADY_CANCELLED);
		}
		session.update(request.title(), request.date(), request.time(), request.location(), request.status());
		return toAdminDto(session);
	}

	@Transactional
	public SessionDtos.AdminSessionDto delete(Long sessionId) {
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
		if (session.getStatus() == SessionStatus.CANCELLED) {
			throw new ApiException(ErrorCode.SESSION_ALREADY_CANCELLED);
		}
		session.cancel();
		expireActiveQrs(session.getId());
		return toAdminDto(session);
	}

	@Transactional
	public SessionDtos.QrCodeDto createQrCode(Long sessionId) {
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

		if (qrCodeRepository.existsBySessionIdAndExpiresAtAfter(sessionId, Instant.now())) {
			throw new ApiException(ErrorCode.QR_ALREADY_ACTIVE);
		}

		QrCode qrCode = createQrCode(session);
		return new SessionDtos.QrCodeDto(qrCode.getId(), session.getId(), qrCode.getHashValue(), qrCode.getCreatedAt(), qrCode.getExpiresAt());
	}

	@Transactional
	public SessionDtos.QrCodeDto renewQrCode(Long qrCodeId) {
		QrCode qrCode = qrCodeRepository.findById(qrCodeId)
				.orElseThrow(() -> new ApiException(ErrorCode.QR_NOT_FOUND));

		qrCode.setExpiresAt(Instant.now());
		QrCode renewed = createQrCode(qrCode.getSession());
		return new SessionDtos.QrCodeDto(
				renewed.getId(),
				renewed.getSession().getId(),
				renewed.getHashValue(),
				renewed.getCreatedAt(),
				renewed.getExpiresAt()
		);
	}

	private QrCode createQrCode(Session session) {
		String hashValue = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
		return qrCodeRepository.save(new QrCode(session, hashValue, expiresAt));
	}

	private SessionDtos.AdminSessionDto toAdminDto(Session session) {
		List<Attendance> attendances = attendanceRepository.findBySessionIdOrderByIdAsc(session.getId());
		int present = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
		int absent = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
		int late = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
		int excused = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
		boolean qrActive = session.getStatus() != SessionStatus.CANCELLED
				&& qrCodeRepository.existsBySessionIdAndExpiresAtAfter(session.getId(), Instant.now());

		return new SessionDtos.AdminSessionDto(
				session.getId(),
				session.getCohort().getId(),
				session.getTitle(),
				session.getDate(),
				session.getTime(),
				session.getLocation(),
				session.getStatus(),
				new SessionDtos.AttendanceSummary(present, absent, late, excused, attendances.size()),
				qrActive,
				session.getCreatedAt(),
				session.getUpdatedAt()
		);
	}

	private void expireActiveQrs(Long sessionId) {
		Instant now = Instant.now();
		qrCodeRepository.findBySessionIdAndExpiresAtAfter(sessionId, now)
				.forEach(qr -> qr.setExpiresAt(now));
	}
}
