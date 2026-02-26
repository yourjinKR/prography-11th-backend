package app.backend.api.member;

import app.backend.api.attendance.AttendanceResponses;
import app.backend.api.attendance.AttendanceService;
import app.backend.global.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

	private final MemberService memberService;
	private final AttendanceService attendanceService;

	public MemberController(MemberService memberService, AttendanceService attendanceService) {
		this.memberService = memberService;
		this.attendanceService = attendanceService;
	}

	@GetMapping("/{id}")
	public ApiResponse<MemberDto> getMember(@PathVariable Long id) {
		return ApiResponse.success(memberService.getMember(id));
	}

	@GetMapping("/{memberId}/attendance-summary")
	public ApiResponse<AttendanceResponses.MemberAttendanceSummaryResponse> getAttendanceSummary(@PathVariable Long memberId) {
		return ApiResponse.success(attendanceService.getMemberSummary(memberId));
	}
}
