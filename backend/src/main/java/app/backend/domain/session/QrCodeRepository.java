package app.backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QrCodeRepository extends JpaRepository<QrCode, Long> {
	Optional<QrCode> findByHashValue(String hashValue);
	boolean existsBySessionIdAndExpiresAtAfter(Long sessionId, Instant now);
	Optional<QrCode> findTopBySessionIdAndExpiresAtAfterOrderByExpiresAtDesc(Long sessionId, Instant now);
	List<QrCode> findBySessionIdAndExpiresAtAfter(Long sessionId, Instant now);
}
