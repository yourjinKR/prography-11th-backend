package app.backend.api.member;

import app.backend.api.common.PageResponse;
import app.backend.domain.cohort.Cohort;
import app.backend.domain.cohort.CohortRepository;
import app.backend.domain.cohort.Part;
import app.backend.domain.cohort.PartRepository;
import app.backend.domain.cohort.Team;
import app.backend.domain.cohort.TeamRepository;
import app.backend.domain.deposit.DepositHistory;
import app.backend.domain.deposit.DepositHistoryRepository;
import app.backend.domain.deposit.DepositType;
import app.backend.domain.member.CohortMember;
import app.backend.domain.member.CohortMemberRepository;
import app.backend.domain.member.Member;
import app.backend.domain.member.MemberRepository;
import app.backend.domain.member.MemberRole;
import app.backend.domain.member.MemberStatus;
import app.backend.global.error.ApiException;
import app.backend.global.error.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MemberService {

	private static final int INITIAL_DEPOSIT = 100_000;

	private final MemberRepository memberRepository;
	private final CohortRepository cohortRepository;
	private final PartRepository partRepository;
	private final TeamRepository teamRepository;
	private final CohortMemberRepository cohortMemberRepository;
	private final DepositHistoryRepository depositHistoryRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public MemberService(
			MemberRepository memberRepository,
			CohortRepository cohortRepository,
			PartRepository partRepository,
			TeamRepository teamRepository,
			CohortMemberRepository cohortMemberRepository,
			DepositHistoryRepository depositHistoryRepository,
			BCryptPasswordEncoder passwordEncoder
	) {
		this.memberRepository = memberRepository;
		this.cohortRepository = cohortRepository;
		this.partRepository = partRepository;
		this.teamRepository = teamRepository;
		this.cohortMemberRepository = cohortMemberRepository;
		this.depositHistoryRepository = depositHistoryRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public MemberDto getMember(Long id) {
		Member member = memberRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		return MemberDto.from(member);
	}

	@Transactional
	public AdminMemberDto create(CreateMemberRequest request) {
		if (memberRepository.findByLoginId(request.loginId()).isPresent()) {
			throw new ApiException(ErrorCode.DUPLICATE_LOGIN_ID);
		}

		Cohort cohort = cohortRepository.findById(request.cohortId())
				.orElseThrow(() -> new ApiException(ErrorCode.COHORT_NOT_FOUND));
		Part part = resolvePart(request.partId(), cohort.getId());
		Team team = resolveTeam(request.teamId(), cohort.getId());

		Member member = memberRepository.save(new Member(
				request.loginId(),
				passwordEncoder.encode(request.password()),
				request.name(),
				request.phone(),
				MemberStatus.ACTIVE,
				MemberRole.MEMBER
		));

		CohortMember cohortMember = cohortMemberRepository.save(new CohortMember(
				member, cohort, part, team, INITIAL_DEPOSIT, 0
		));

		depositHistoryRepository.save(new DepositHistory(
				cohortMember, DepositType.INITIAL, INITIAL_DEPOSIT, INITIAL_DEPOSIT, null, "Initial deposit"
		));

		return toAdminDto(member, cohortMember);
	}

	@Transactional(readOnly = true)
	public PageResponse<AdminMemberDto> getDashboard(
			int page,
			int size,
			String searchType,
			String searchValue,
			Integer generation,
			String partName,
			String teamName,
			MemberStatus status
	) {
		List<Member> members = memberRepository.findAll();
		List<Member> filtered = new ArrayList<>();
		for (Member member : members) {
			if (status != null && member.getStatus() != status) {
				continue;
			}
			if (!matchesSearch(member, searchType, searchValue)) {
				continue;
			}
			filtered.add(member);
		}

		List<AdminMemberDto> enriched = filtered.stream()
				.map(member -> {
					Optional<CohortMember> cohortMemberOpt = cohortMemberRepository.findFirstByMemberIdOrderByIdDesc(member.getId());
					return cohortMemberOpt.map(cm -> toAdminDto(member, cm))
							.orElseGet(() -> AdminMemberDto.of(member, null, null, null, null));
				})
				.filter(dto -> generation == null || generation.equals(dto.generation()))
				.filter(dto -> partName == null || (dto.partName() != null && dto.partName().equalsIgnoreCase(partName)))
				.filter(dto -> teamName == null || (dto.teamName() != null && dto.teamName().equalsIgnoreCase(teamName)))
				.sorted(Comparator.comparing(AdminMemberDto::id))
				.toList();

		int from = Math.max(0, page * size);
		if (from >= enriched.size()) {
			return new PageResponse<>(List.of(), page, size, enriched.size(), calcTotalPages(enriched.size(), size));
		}
		int to = Math.min(enriched.size(), from + size);
		return new PageResponse<>(enriched.subList(from, to), page, size, enriched.size(), calcTotalPages(enriched.size(), size));
	}

	@Transactional(readOnly = true)
	public AdminMemberDto getDetail(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		Optional<CohortMember> cohortMemberOpt = cohortMemberRepository.findFirstByMemberIdOrderByIdDesc(memberId);
		return cohortMemberOpt.map(cm -> toAdminDto(member, cm))
				.orElseGet(() -> AdminMemberDto.of(member, null, null, null, null));
	}

	@Transactional
	public AdminMemberDto update(Long memberId, UpdateMemberRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

		member.updateProfile(request.name(), request.phone());

		if (request.cohortId() != null || request.partId() != null || request.teamId() != null) {
			CohortMember cohortMember = cohortMemberRepository.findFirstByMemberIdOrderByIdDesc(memberId)
					.orElse(null);

			Cohort targetCohort;
			if (request.cohortId() != null) {
				targetCohort = cohortRepository.findById(request.cohortId())
						.orElseThrow(() -> new ApiException(ErrorCode.COHORT_NOT_FOUND));
			} else if (cohortMember != null) {
				targetCohort = cohortMember.getCohort();
			} else {
				throw new ApiException(ErrorCode.COHORT_NOT_FOUND);
			}

			boolean cohortChanged = cohortMember == null || !cohortMember.getCohort().getId().equals(targetCohort.getId());
			Part part = request.partId() != null
					? resolvePart(request.partId(), targetCohort.getId())
					: (cohortChanged ? null : (cohortMember != null ? cohortMember.getPart() : null));
			Team team = request.teamId() != null
					? resolveTeam(request.teamId(), targetCohort.getId())
					: (cohortChanged ? null : (cohortMember != null ? cohortMember.getTeam() : null));

			if (cohortChanged) {
				cohortMember = cohortMemberRepository.save(new CohortMember(member, targetCohort, part, team, INITIAL_DEPOSIT, 0));
				depositHistoryRepository.save(new DepositHistory(
						cohortMember, DepositType.INITIAL, INITIAL_DEPOSIT, INITIAL_DEPOSIT, null, "Initial deposit"
				));
			} else {
				cohortMember.reassign(targetCohort, part, team);
			}
		}

		CohortMember latest = cohortMemberRepository.findFirstByMemberIdOrderByIdDesc(member.getId()).orElse(null);
		if (latest == null) {
			return AdminMemberDto.of(member, null, null, null, null);
		}
		return toAdminDto(member, latest);
	}

	@Transactional
	public WithdrawMemberDto withdraw(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
		if (member.getStatus() == MemberStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.MEMBER_ALREADY_WITHDRAWN);
		}
		member.withdraw();
		return new WithdrawMemberDto(member.getId(), member.getLoginId(), member.getName(), member.getStatus(), member.getUpdatedAt());
	}

	private AdminMemberDto toAdminDto(Member member, CohortMember cohortMember) {
		return AdminMemberDto.of(
				member,
				cohortMember.getCohort().getGeneration(),
				cohortMember.getPart() != null ? cohortMember.getPart().getName() : null,
				cohortMember.getTeam() != null ? cohortMember.getTeam().getName() : null,
				cohortMember.getDeposit()
		);
	}

	private Part resolvePart(Long partId, Long cohortId) {
		if (partId == null) {
			return null;
		}
		Part part = partRepository.findById(partId).orElseThrow(() -> new ApiException(ErrorCode.PART_NOT_FOUND));
		if (!part.getCohort().getId().equals(cohortId)) {
			throw new ApiException(ErrorCode.PART_NOT_FOUND);
		}
		return part;
	}

	private Team resolveTeam(Long teamId, Long cohortId) {
		if (teamId == null) {
			return null;
		}
		Team team = teamRepository.findById(teamId).orElseThrow(() -> new ApiException(ErrorCode.TEAM_NOT_FOUND));
		if (!team.getCohort().getId().equals(cohortId)) {
			throw new ApiException(ErrorCode.TEAM_NOT_FOUND);
		}
		return team;
	}

	private boolean matchesSearch(Member member, String searchType, String searchValue) {
		if (searchType == null || searchValue == null || searchValue.isBlank()) {
			return true;
		}
		String keyword = searchValue.toLowerCase(Locale.ROOT);
		return switch (searchType) {
			case "name" -> member.getName().toLowerCase(Locale.ROOT).contains(keyword);
			case "loginId" -> member.getLoginId().toLowerCase(Locale.ROOT).contains(keyword);
			case "phone" -> member.getPhone().toLowerCase(Locale.ROOT).contains(keyword);
			default -> throw new ApiException(ErrorCode.INVALID_INPUT);
		};
	}

	private int calcTotalPages(int totalElements, int size) {
		if (size <= 0) {
			return 0;
		}
		return (int) Math.ceil((double) totalElements / size);
	}
}
