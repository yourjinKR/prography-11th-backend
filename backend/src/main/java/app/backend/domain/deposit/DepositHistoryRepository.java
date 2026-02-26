package app.backend.domain.deposit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {
	List<DepositHistory> findByCohortMemberIdOrderByCreatedAtAsc(Long cohortMemberId);
}
