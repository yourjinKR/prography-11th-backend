package app.backend.domain.session;

import app.backend.domain.cohort.Cohort;
import app.backend.domain.common.BaseTimeEntity;
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

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
public class Session extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cohort_id")
	private Cohort cohort;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private LocalDate date;

	@Column(nullable = false)
	private LocalTime time;

	@Column(nullable = false, length = 200)
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus status;

	protected Session() {
	}

	public Session(Cohort cohort, String title, LocalDate date, LocalTime time, String location, SessionStatus status) {
		this.cohort = cohort;
		this.title = title;
		this.date = date;
		this.time = time;
		this.location = location;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public Cohort getCohort() {
		return cohort;
	}

	public String getTitle() {
		return title;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getTime() {
		return time;
	}

	public String getLocation() {
		return location;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public void update(String title, LocalDate date, LocalTime time, String location, SessionStatus status) {
		if (title != null) {
			this.title = title;
		}
		if (date != null) {
			this.date = date;
		}
		if (time != null) {
			this.time = time;
		}
		if (location != null) {
			this.location = location;
		}
		if (status != null) {
			this.status = status;
		}
	}

	public void cancel() {
		this.status = SessionStatus.CANCELLED;
	}
}
