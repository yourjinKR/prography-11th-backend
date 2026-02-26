package app.backend.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CohortMemberRepository extends JpaRepository<CohortMember, Long> {
	Optional<CohortMember> findByMemberIdAndCohortId(Long memberId, Long cohortId);
	Optional<CohortMember> findFirstByMemberIdOrderByIdDesc(Long memberId);
	List<CohortMember> findByCohortId(Long cohortId);
}
