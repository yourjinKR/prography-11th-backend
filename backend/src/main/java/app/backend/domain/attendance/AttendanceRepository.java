package app.backend.domain.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
	boolean existsBySessionIdAndMemberId(Long sessionId, Long memberId);
	Optional<Attendance> findBySessionIdAndMemberId(Long sessionId, Long memberId);
	List<Attendance> findBySessionIdOrderByIdAsc(Long sessionId);
	List<Attendance> findByMemberIdOrderByIdAsc(Long memberId);
}
