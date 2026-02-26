package app.backend.domain.cohort;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
	List<Team> findByCohortId(Long cohortId);
}
