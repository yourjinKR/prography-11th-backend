package app.backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
	List<Session> findByCohortId(Long cohortId);
	List<Session> findByCohortIdAndDateBetween(Long cohortId, LocalDate from, LocalDate to);
}
