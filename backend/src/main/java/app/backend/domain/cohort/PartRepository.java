package app.backend.domain.cohort;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartRepository extends JpaRepository<Part, Long> {
	List<Part> findByCohortId(Long cohortId);
}
