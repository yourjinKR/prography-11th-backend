package app.backend.domain.cohort;

import app.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cohorts")
public class Cohort extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private Integer generation;

	@Column(nullable = false, length = 50)
	private String name;

	protected Cohort() {
	}

	public Cohort(Integer generation, String name) {
		this.generation = generation;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public Integer getGeneration() {
		return generation;
	}

	public String getName() {
		return name;
	}
}
