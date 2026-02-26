package app.backend.domain.cohort;

import app.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "parts")
public class Part extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cohort_id")
	private Cohort cohort;

	@Column(nullable = false, length = 50)
	private String name;

	protected Part() {
	}

	public Part(Cohort cohort, String name) {
		this.cohort = cohort;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Cohort getCohort() {
		return cohort;
	}

	public String getName() {
		return name;
	}
}
