package app.backend.domain.cohort;

import app.backend.global.error.ApiException;
import app.backend.global.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CurrentCohortResolver {

	private static final int CURRENT_GENERATION = 11;

	private final CohortRepository cohortRepository;

	public CurrentCohortResolver(CohortRepository cohortRepository) {
		this.cohortRepository = cohortRepository;
	}

	public Cohort resolveCurrentCohort() {
		return cohortRepository.findByGeneration(CURRENT_GENERATION)
				.orElseThrow(() -> new ApiException(ErrorCode.COHORT_NOT_FOUND));
	}
}
