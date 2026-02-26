package app.backend.domain.member;

import app.backend.domain.cohort.Cohort;
import app.backend.domain.cohort.Part;
import app.backend.domain.cohort.Team;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "cohort_members",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_cohort_member_member_cohort", columnNames = {"member_id", "cohort_id"})
		}
)
public class CohortMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id")
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cohort_id")
	private Cohort cohort;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "part_id")
	private Part part;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id")
	private Team team;

	@Column(nullable = false)
	private Integer deposit;

	@Column(nullable = false)
	private Integer excuseCount;

	protected CohortMember() {
	}

	public CohortMember(Member member, Cohort cohort, Part part, Team team, Integer deposit, Integer excuseCount) {
		this.member = member;
		this.cohort = cohort;
		this.part = part;
		this.team = team;
		this.deposit = deposit;
		this.excuseCount = excuseCount;
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public Cohort getCohort() {
		return cohort;
	}

	public Part getPart() {
		return part;
	}

	public Team getTeam() {
		return team;
	}

	public Integer getDeposit() {
		return deposit;
	}

	public Integer getExcuseCount() {
		return excuseCount;
	}

	public void setDeposit(Integer deposit) {
		this.deposit = deposit;
	}

	public void setExcuseCount(Integer excuseCount) {
		this.excuseCount = excuseCount;
	}

	public void reassign(Cohort cohort, Part part, Team team) {
		this.cohort = cohort;
		this.part = part;
		this.team = team;
	}
}
