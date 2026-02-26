package app.backend.api.cohort;

import app.backend.global.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/cohorts")
public class AdminCohortController {

	private final CohortService cohortService;

	public AdminCohortController(CohortService cohortService) {
		this.cohortService = cohortService;
	}

	@GetMapping
	public ApiResponse<List<CohortService.CohortSummaryDto>> getCohorts() {
		return ApiResponse.success(cohortService.getCohorts());
	}

	@GetMapping("/{cohortId}")
	public ApiResponse<CohortService.CohortDetailDto> getDetail(@PathVariable Long cohortId) {
		return ApiResponse.success(cohortService.getCohortDetail(cohortId));
	}
}
