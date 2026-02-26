package app.backend.api.attendance;

import app.backend.domain.attendance.Attendance;
import app.backend.domain.attendance.AttendanceRepository;
import app.backend.domain.attendance.AttendanceStatus;
import app.backend.domain.cohort.Cohort;
import app.backend.domain.cohort.CurrentCohortResolver;
import app.backend.domain.deposit.DepositHistory;
import app.backend.domain.deposit.DepositHistoryRepository;
import app.backend.domain.deposit.DepositType;
import app.backend.domain.member.CohortMember;
import app.backend.domain.member.CohortMemberRepository;
import app.backend.domain.member.Member;
import app.backend.domain.member.MemberRepository;
import app.backend.domain.member.MemberStatus;
import app.backend.domain.session.QrCode;
import app.backend.domain.session.QrCodeRepository;
import app.backend.domain.session.Session;
import app.backend.domain.session.SessionRepository;
import app.backend.domain.session.SessionStatus;
import app.backend.global.error.ApiException;
import app.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AttendanceService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final AttendanceRepository attendanceRepository;
	private final QrCodeRepository qrCodeRepository;
	private final SessionRepository sessionRepository;
	private final MemberRepository memberRepository;
	private final CohortMemberRepository cohortMemberRepository;
	private final DepositHistoryRepository depositHistoryRepository;
	private final CurrentCohortResolver currentCohortResolver;

	public AttendanceService(
			AttendanceRepository attendanceRepository,
			QrCodeRepository qrCodeRepository,
			SessionRepository sessionRepository,
			MemberRepository memberRepository,
			CohortMemberRepository cohortMemberRepository,
			DepositHistoryRepository depositHistoryRepository,
			CurrentCohortResolver currentCohortResolver
	) {
		this.attendanceRepository = attendanceRepository;
		this.qrCodeRepository = qrCodeRepository;
		this.sessionRepository = sessionRepository;
		this.memberRepository = memberRepository;
		this.cohortMemberRepository = cohortMemberRepository;
		this.depositHistoryRepository = depositHistoryRepository;
		this.currentCohortResolver = currentCohortResolver;
	}

	@Transactional
	public AttendanceResponses.AttendanceResponse checkIn(AttendanceRequests.CheckInRequest request) {
		Instant now = Instant.now();
		QrCode qrCode = qrCodeRepository.findByHashValue(request.hashValue())
				.orElseThrow(() -> new ApiException(ErrorCode.QR_INVALID));
		if (qrCode.getExpiresAt().isBefore(now)) {
			throw new ApiException(ErrorCode.QR_EXPIRED);
		}

		Session session = qrCode.getSession();
		if (session.getStatus() != SessionStatus.IN_PROGRESS) {
			throw new ApiException(ErrorCode.SESSION_NOT_IN_PROGRESS);
		}

		Member member = memberRepository.findById(request.memberId())
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (member.getStatus() == MemberStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.MEMBER_WITHDRAWN);
		}

		if (attendanceRepository.existsBySessionIdAndMemberId(session.getId(), member.getId())) {
			throw new ApiException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
		}

		CohortMember cohortMember = resolveCurrentCohortMember(member.getId());

		lateInfo lateInfo = calculateCheckInStatus(session, now);
		int penalty = calculatePenalty(lateInfo.status(), lateInfo.lateMinutes());

		Attendance attendance = attendanceRepository.save(new Attendance(
				session,
				member,
				qrCode,
				lateInfo.status(),
				lateInfo.lateMinutes(),
				penalty,
				null,
				now
		));

		applyDiff(cohortMember, penalty, attendance, "QR check-in");
		return AttendanceResponses.AttendanceResponse.from(attendance);
	}

	@Transactional(readOnly = true)
	public List<AttendanceResponses.MemberAttendanceRecordResponse> getAttendances(Long memberId) {
		memberRepository.findById(memberId).orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		return attendanceRepository.findByMemberIdOrderByIdAsc(memberId).stream()
				.map(AttendanceResponses.MemberAttendanceRecordResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AttendanceResponses.MemberAttendanceSummaryResponse getMemberSummary(Long memberId) {
		memberRepository.findById(memberId).orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		List<Attendance> attendances = attendanceRepository.findByMemberIdOrderByIdAsc(memberId);
		int present = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
		int absent = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
		int late = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
		int excused = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
		int totalPenalty = attendances.stream().mapToInt(Attendance::getPenaltyAmount).sum();

		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		Integer deposit = cohortMemberRepository.findByMemberIdAndCohortId(memberId, cohort.getId())
				.map(CohortMember::getDeposit)
				.orElse(null);

		return new AttendanceResponses.MemberAttendanceSummaryResponse(
				memberId, present, absent, late, excused, totalPenalty, deposit
		);
	}

	@Transactional
	public AttendanceResponses.AttendanceResponse registerByAdmin(AttendanceRequests.AdminRegisterAttendanceRequest request) {
		Session session = sessionRepository.findById(request.sessionId())
				.orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
		Member member = memberRepository.findById(request.memberId())
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (attendanceRepository.existsBySessionIdAndMemberId(session.getId(), member.getId())) {
			throw new ApiException(ErrorCode.ATTENDANCE_ALREADY_CHECKED);
		}

		CohortMember cohortMember = resolveCurrentCohortMember(member.getId());
		AttendanceStatus status = request.status();
		Integer lateMinutes = normalizeLateMinutes(status, request.lateMinutes());
		int penalty = calculatePenalty(status, lateMinutes);

		adjustExcuseCountForUpdate(cohortMember, null, status);

		Attendance attendance = attendanceRepository.save(new Attendance(
				session,
				member,
				null,
				status,
				lateMinutes,
				penalty,
				request.reason(),
				null
		));
		applyDiff(cohortMember, penalty, attendance, "Admin attendance register");
		return AttendanceResponses.AttendanceResponse.from(attendance);
	}

	@Transactional
	public AttendanceResponses.AttendanceResponse updateByAdmin(Long attendanceId, AttendanceRequests.AdminUpdateAttendanceRequest request) {
		Attendance attendance = attendanceRepository.findById(attendanceId)
				.orElseThrow(() -> new ApiException(ErrorCode.ATTENDANCE_NOT_FOUND));
		CohortMember cohortMember = resolveCurrentCohortMember(attendance.getMember().getId());

		AttendanceStatus newStatus = request.status();
		Integer newLateMinutes = normalizeLateMinutes(newStatus, request.lateMinutes());
		int newPenalty = calculatePenalty(newStatus, newLateMinutes);
		int diff = newPenalty - attendance.getPenaltyAmount();

		adjustExcuseCountForUpdate(cohortMember, attendance.getStatus(), newStatus);
		applyDiff(cohortMember, diff, attendance, "Admin attendance update");

		attendance.update(newStatus, newLateMinutes, newPenalty, request.reason());
		return AttendanceResponses.AttendanceResponse.from(attendance);
	}

	@Transactional(readOnly = true)
	public List<AttendanceResponses.AdminSessionAttendanceSummaryItem> getSessionSummary(Long sessionId) {
		sessionRepository.findById(sessionId).orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		List<CohortMember> cohortMembers = cohortMemberRepository.findByCohortId(cohort.getId());
		List<Attendance> sessionAttendances = attendanceRepository.findBySessionIdOrderByIdAsc(sessionId);

		return cohortMembers.stream().map(cm -> {
			List<Attendance> attendances = sessionAttendances.stream()
					.filter(a -> a.getMember().getId().equals(cm.getMember().getId()))
					.toList();
			int present = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
			int absent = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
			int late = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
			int excused = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.EXCUSED).count();
			int totalPenalty = attendances.stream().mapToInt(Attendance::getPenaltyAmount).sum();
			return new AttendanceResponses.AdminSessionAttendanceSummaryItem(
					cm.getMember().getId(),
					cm.getMember().getName(),
					present,
					absent,
					late,
					excused,
					totalPenalty,
					cm.getDeposit()
			);
		}).toList();
	}

	@Transactional(readOnly = true)
	public AttendanceResponses.AdminMemberAttendanceDetailResponse getMemberDetail(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		CohortMember cohortMember = cohortMemberRepository.findByMemberIdAndCohortId(memberId, cohort.getId()).orElse(null);

		List<AttendanceResponses.AttendanceResponse> attendances = attendanceRepository.findByMemberIdOrderByIdAsc(memberId).stream()
				.map(AttendanceResponses.AttendanceResponse::from)
				.toList();

		return new AttendanceResponses.AdminMemberAttendanceDetailResponse(
				member.getId(),
				member.getName(),
				cohortMember != null ? cohortMember.getCohort().getGeneration() : null,
				cohortMember != null && cohortMember.getPart() != null ? cohortMember.getPart().getName() : null,
				cohortMember != null && cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null,
				cohortMember != null ? cohortMember.getDeposit() : null,
				cohortMember != null ? cohortMember.getExcuseCount() : null,
				attendances
		);
	}

	@Transactional(readOnly = true)
	public AttendanceResponses.AdminSessionAttendancesResponse getSessionAttendances(Long sessionId) {
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
		List<AttendanceResponses.AttendanceResponse> attendances = attendanceRepository.findBySessionIdOrderByIdAsc(sessionId).stream()
				.map(AttendanceResponses.AttendanceResponse::from)
				.toList();
		return new AttendanceResponses.AdminSessionAttendancesResponse(session.getId(), session.getTitle(), attendances);
	}

	@Transactional(readOnly = true)
	public List<AttendanceResponses.DepositHistoryResponse> getDepositHistories(Long cohortMemberId) {
		cohortMemberRepository.findById(cohortMemberId).orElseThrow(() -> new ApiException(ErrorCode.COHORT_MEMBER_NOT_FOUND));
		return depositHistoryRepository.findByCohortMemberIdOrderByCreatedAtAsc(cohortMemberId).stream()
				.map(history -> new AttendanceResponses.DepositHistoryResponse(
						history.getId(),
						history.getCohortMember().getId(),
						history.getType(),
						history.getAmount(),
						history.getBalanceAfter(),
						history.getAttendance() != null ? history.getAttendance().getId() : null,
						history.getDescription(),
						history.getCreatedAt()
				))
				.toList();
	}

	private CohortMember resolveCurrentCohortMember(Long memberId) {
		Cohort cohort = currentCohortResolver.resolveCurrentCohort();
		return cohortMemberRepository.findByMemberIdAndCohortId(memberId, cohort.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.COHORT_MEMBER_NOT_FOUND));
	}

	private lateInfo calculateCheckInStatus(Session session, Instant now) {
		Instant sessionTime = LocalDateTime.of(session.getDate(), session.getTime())
				.atZone(SEOUL)
				.toInstant();
		if (!now.isAfter(sessionTime)) {
			return new lateInfo(AttendanceStatus.PRESENT, null);
		}

		long diffSeconds = Duration.between(sessionTime, now).getSeconds();
		int lateMinutes = (int) Math.max(1, (diffSeconds + 59) / 60);
		return new lateInfo(AttendanceStatus.LATE, lateMinutes);
	}

	private Integer normalizeLateMinutes(AttendanceStatus status, Integer lateMinutes) {
		if (status != AttendanceStatus.LATE) {
			return null;
		}
		if (lateMinutes == null || lateMinutes < 0) {
			throw new ApiException(ErrorCode.INVALID_INPUT);
		}
		return lateMinutes;
	}

	private int calculatePenalty(AttendanceStatus status, Integer lateMinutes) {
		return switch (status) {
			case PRESENT, EXCUSED -> 0;
			case ABSENT -> 10_000;
			case LATE -> {
				if (lateMinutes == null || lateMinutes < 0) {
					throw new ApiException(ErrorCode.INVALID_INPUT);
				}
				yield Math.min(lateMinutes * 500, 10_000);
			}
		};
	}

	private void adjustExcuseCountForUpdate(CohortMember cohortMember, AttendanceStatus oldStatus, AttendanceStatus newStatus) {
		if (oldStatus != AttendanceStatus.EXCUSED && newStatus == AttendanceStatus.EXCUSED) {
			if (cohortMember.getExcuseCount() >= 3) {
				throw new ApiException(ErrorCode.EXCUSE_LIMIT_EXCEEDED);
			}
			cohortMember.setExcuseCount(cohortMember.getExcuseCount() + 1);
		}
		if (oldStatus == AttendanceStatus.EXCUSED && newStatus != AttendanceStatus.EXCUSED) {
			cohortMember.setExcuseCount(Math.max(0, cohortMember.getExcuseCount() - 1));
		}
	}

	private void applyDiff(CohortMember cohortMember, int diff, Attendance attendance, String descriptionPrefix) {
		if (diff == 0) {
			return;
		}

		if (diff > 0) {
			if (cohortMember.getDeposit() < diff) {
				throw new ApiException(ErrorCode.DEPOSIT_INSUFFICIENT);
			}
			cohortMember.setDeposit(cohortMember.getDeposit() - diff);
			depositHistoryRepository.save(new DepositHistory(
					cohortMember,
					DepositType.PENALTY,
					-diff,
					cohortMember.getDeposit(),
					attendance,
					descriptionPrefix + " penalty " + diff
			));
			return;
		}

		int refund = -diff;
		cohortMember.setDeposit(cohortMember.getDeposit() + refund);
		depositHistoryRepository.save(new DepositHistory(
				cohortMember,
				DepositType.REFUND,
				refund,
				cohortMember.getDeposit(),
				attendance,
				descriptionPrefix + " refund " + refund
		));
	}

	private record lateInfo(AttendanceStatus status, Integer lateMinutes) {
	}
}
