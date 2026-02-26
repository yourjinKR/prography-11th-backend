package app.backend.api.member;

import app.backend.api.common.PageResponse;
import app.backend.domain.member.MemberStatus;
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

@RestController
@RequestMapping("/api/v1/admin/members")
public class AdminMemberController {

	private final MemberService memberService;

	public AdminMemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminMemberDto> create(@Valid @RequestBody CreateMemberRequest request) {
		return ApiResponse.success(memberService.create(request));
	}

	@GetMapping
	public ApiResponse<PageResponse<AdminMemberDto>> getDashboard(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String searchType,
			@RequestParam(required = false) String searchValue,
			@RequestParam(required = false) Integer generation,
			@RequestParam(required = false) String partName,
			@RequestParam(required = false) String teamName,
			@RequestParam(required = false) MemberStatus status
	) {
		return ApiResponse.success(memberService.getDashboard(page, size, searchType, searchValue, generation, partName, teamName, status));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminMemberDto> getDetail(@PathVariable Long id) {
		return ApiResponse.success(memberService.getDetail(id));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminMemberDto> update(@PathVariable Long id, @RequestBody UpdateMemberRequest request) {
		return ApiResponse.success(memberService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<WithdrawMemberDto> delete(@PathVariable Long id) {
		return ApiResponse.success(memberService.withdraw(id));
	}
}
