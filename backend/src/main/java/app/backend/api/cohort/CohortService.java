package app.backend.api.cohort;

import app.backend.domain.cohort.Cohort;
import app.backend.domain.cohort.CohortRepository;
import app.backend.domain.cohort.PartRepository;
import app.backend.domain.cohort.TeamRepository;
import app.backend.global.error.ApiException;
import app.backend.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CohortService {

	private final CohortRepository cohortRepository;
	private final PartRepository partRepository;
	private final TeamRepository teamRepository;

	public CohortService(CohortRepository cohortRepository, PartRepository partRepository, TeamRepository teamRepository) {
		this.cohortRepository = cohortRepository;
		this.partRepository = partRepository;
		this.teamRepository = teamRepository;
	}

	@Transactional(readOnly = true)
	public List<CohortSummaryDto> getCohorts() {
		return cohortRepository.findAll().stream()
				.map(c -> new CohortSummaryDto(c.getId(), c.getGeneration(), c.getName(), c.getCreatedAt()))
				.toList();
	}

	@Transactional(readOnly = true)
	public CohortDetailDto getCohortDetail(Long cohortId) {
		Cohort cohort = cohortRepository.findById(cohortId)
				.orElseThrow(() -> new ApiException(ErrorCode.COHORT_NOT_FOUND));

		List<NamedItem> parts = partRepository.findByCohortId(cohortId).stream()
				.map(part -> new NamedItem(part.getId(), part.getName()))
				.toList();
		List<NamedItem> teams = teamRepository.findByCohortId(cohortId).stream()
				.map(team -> new NamedItem(team.getId(), team.getName()))
				.toList();

		return new CohortDetailDto(cohort.getId(), cohort.getGeneration(), cohort.getName(), parts, teams, cohort.getCreatedAt());
	}

	public record CohortSummaryDto(Long id, Integer generation, String name, Instant createdAt) {
	}

	public record NamedItem(Long id, String name) {
	}

	public record CohortDetailDto(
			Long id,
			Integer generation,
			String name,
			List<NamedItem> parts,
			List<NamedItem> teams,
			Instant createdAt
	) {
	}
}
