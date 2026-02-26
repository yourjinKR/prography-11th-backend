package app.backend.domain.attendance;

import app.backend.domain.common.BaseTimeEntity;
import app.backend.domain.member.Member;
import app.backend.domain.session.QrCode;
import app.backend.domain.session.Session;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
		name = "attendances",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_attendance_session_member", columnNames = {"session_id", "member_id"})
		}
)
public class Attendance extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id")
	private Session session;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id")
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "qrcode_id")
	private QrCode qrCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AttendanceStatus status;

	@Column
	private Integer lateMinutes;

	@Column(nullable = false)
	private Integer penaltyAmount;

	@Column(length = 300)
	private String reason;

	@Column
	private Instant checkedInAt;

	protected Attendance() {
	}

	public Attendance(
			Session session,
			Member member,
			QrCode qrCode,
			AttendanceStatus status,
			Integer lateMinutes,
			Integer penaltyAmount,
			String reason,
			Instant checkedInAt
	) {
		this.session = session;
		this.member = member;
		this.qrCode = qrCode;
		this.status = status;
		this.lateMinutes = lateMinutes;
		this.penaltyAmount = penaltyAmount;
		this.reason = reason;
		this.checkedInAt = checkedInAt;
	}

	public Long getId() {
		return id;
	}

	public Session getSession() {
		return session;
	}

	public Member getMember() {
		return member;
	}

	public QrCode getQrCode() {
		return qrCode;
	}

	public AttendanceStatus getStatus() {
		return status;
	}

	public Integer getLateMinutes() {
		return lateMinutes;
	}

	public Integer getPenaltyAmount() {
		return penaltyAmount;
	}

	public String getReason() {
		return reason;
	}

	public Instant getCheckedInAt() {
		return checkedInAt;
	}

	public void update(AttendanceStatus status, Integer lateMinutes, Integer penaltyAmount, String reason) {
		this.status = status;
		this.lateMinutes = lateMinutes;
		this.penaltyAmount = penaltyAmount;
		this.reason = reason;
	}
}
