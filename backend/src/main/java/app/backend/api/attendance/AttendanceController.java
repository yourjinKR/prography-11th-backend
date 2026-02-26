package app.backend.api.attendance;

import app.backend.global.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AttendanceController {

	private final AttendanceService attendanceService;

	public AttendanceController(AttendanceService attendanceService) {
		this.attendanceService = attendanceService;
	}

	@PostMapping("/api/v1/attendances")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AttendanceResponses.AttendanceResponse> checkIn(@Valid @RequestBody AttendanceRequests.CheckInRequest request) {
		return ApiResponse.success(attendanceService.checkIn(request));
	}

	@GetMapping("/api/v1/attendances")
	public ApiResponse<List<AttendanceResponses.MemberAttendanceRecordResponse>> getAttendances(@RequestParam Long memberId) {
		return ApiResponse.success(attendanceService.getAttendances(memberId));
	}

	@PostMapping("/api/v1/admin/attendances")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AttendanceResponses.AttendanceResponse> registerByAdmin(
			@Valid @RequestBody AttendanceRequests.AdminRegisterAttendanceRequest request
	) {
		return ApiResponse.success(attendanceService.registerByAdmin(request));
	}

	@PutMapping("/api/v1/admin/attendances/{id}")
	public ApiResponse<AttendanceResponses.AttendanceResponse> updateByAdmin(
			@PathVariable Long id,
			@Valid @RequestBody AttendanceRequests.AdminUpdateAttendanceRequest request
	) {
		return ApiResponse.success(attendanceService.updateByAdmin(id, request));
	}

	@GetMapping("/api/v1/admin/attendances/sessions/{sessionId}/summary")
	public ApiResponse<List<AttendanceResponses.AdminSessionAttendanceSummaryItem>> getSessionSummary(@PathVariable Long sessionId) {
		return ApiResponse.success(attendanceService.getSessionSummary(sessionId));
	}

	@GetMapping("/api/v1/admin/attendances/members/{memberId}")
	public ApiResponse<AttendanceResponses.AdminMemberAttendanceDetailResponse> getMemberDetail(@PathVariable Long memberId) {
		return ApiResponse.success(attendanceService.getMemberDetail(memberId));
	}

	@GetMapping("/api/v1/admin/attendances/sessions/{sessionId}")
	public ApiResponse<AttendanceResponses.AdminSessionAttendancesResponse> getSessionAttendances(@PathVariable Long sessionId) {
		return ApiResponse.success(attendanceService.getSessionAttendances(sessionId));
	}

	@GetMapping("/api/v1/admin/cohort-members/{cohortMemberId}/deposits")
	public ApiResponse<List<AttendanceResponses.DepositHistoryResponse>> getDepositHistories(@PathVariable Long cohortMemberId) {
		return ApiResponse.success(attendanceService.getDepositHistories(cohortMemberId));
	}
}
