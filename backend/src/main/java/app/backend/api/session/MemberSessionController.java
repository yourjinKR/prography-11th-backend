package app.backend.api.session;

import app.backend.global.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class MemberSessionController {

	private final SessionService sessionService;

	public MemberSessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@GetMapping
	public ApiResponse<List<SessionDtos.SessionSummaryDto>> getSessions() {
		return ApiResponse.success(sessionService.getMemberSessions());
	}
}
