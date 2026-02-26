package app.backend.api.session;

import app.backend.domain.session.SessionStatus;
import app.backend.global.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class AdminSessionController {

	private final SessionService sessionService;

	public AdminSessionController(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	@GetMapping("/api/v1/admin/sessions")
	public ApiResponse<List<SessionDtos.AdminSessionDto>> getSessions(
			@RequestParam(required = false) LocalDate dateFrom,
			@RequestParam(required = false) LocalDate dateTo,
			@RequestParam(required = false) SessionStatus status
	) {
		return ApiResponse.success(sessionService.getAdminSessions(dateFrom, dateTo, status));
	}

	@PostMapping("/api/v1/admin/sessions")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SessionDtos.AdminSessionDto> create(@Valid @RequestBody CreateSessionRequest request) {
		return ApiResponse.success(sessionService.create(request));
	}

	@PutMapping("/api/v1/admin/sessions/{id}")
	public ApiResponse<SessionDtos.AdminSessionDto> update(@PathVariable Long id, @RequestBody UpdateSessionRequest request) {
		return ApiResponse.success(sessionService.update(id, request));
	}

	@DeleteMapping("/api/v1/admin/sessions/{id}")
	public ApiResponse<SessionDtos.AdminSessionDto> delete(@PathVariable Long id) {
		return ApiResponse.success(sessionService.delete(id));
	}

	@PostMapping("/api/v1/admin/sessions/{sessionId}/qrcodes")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SessionDtos.QrCodeDto> createQrCode(@PathVariable Long sessionId) {
		return ApiResponse.success(sessionService.createQrCode(sessionId));
	}

	@PutMapping("/api/v1/admin/qrcodes/{qrCodeId}")
	public ApiResponse<SessionDtos.QrCodeDto> renewQrCode(@PathVariable Long qrCodeId) {
		return ApiResponse.success(sessionService.renewQrCode(qrCodeId));
	}
}
