package app.backend.domain.cohort;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
	Optional<Cohort> findByGeneration(Integer generation);
}
