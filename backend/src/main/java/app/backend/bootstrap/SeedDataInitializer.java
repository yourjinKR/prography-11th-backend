package app.backend.bootstrap;

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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SeedDataInitializer implements ApplicationRunner {

	private static final int INITIAL_DEPOSIT = 100_000;
	private static final String ADMIN_LOGIN_ID = "admin";
	private static final String ADMIN_PASSWORD = "admin1234";

	private final CohortRepository cohortRepository;
	private final PartRepository partRepository;
	private final TeamRepository teamRepository;
	private final MemberRepository memberRepository;
	private final CohortMemberRepository cohortMemberRepository;
	private final DepositHistoryRepository depositHistoryRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public SeedDataInitializer(
			CohortRepository cohortRepository,
			PartRepository partRepository,
			TeamRepository teamRepository,
			MemberRepository memberRepository,
			CohortMemberRepository cohortMemberRepository,
			DepositHistoryRepository depositHistoryRepository,
			BCryptPasswordEncoder passwordEncoder
	) {
		this.cohortRepository = cohortRepository;
		this.partRepository = partRepository;
		this.teamRepository = teamRepository;
		this.memberRepository = memberRepository;
		this.cohortMemberRepository = cohortMemberRepository;
		this.depositHistoryRepository = depositHistoryRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (cohortRepository.count() > 0) {
			return;
		}

		Cohort cohort10 = cohortRepository.save(new Cohort(10, "10기"));
		Cohort cohort11 = cohortRepository.save(new Cohort(11, "11기"));

		List<String> partNames = List.of("SERVER", "WEB", "iOS", "ANDROID", "DESIGN");
		partNames.forEach(name -> partRepository.save(new Part(cohort10, name)));
		List<Part> cohort11Parts = partNames.stream()
				.map(name -> partRepository.save(new Part(cohort11, name)))
				.toList();

		List<Team> teams = List.of(
				teamRepository.save(new Team(cohort11, "Team A")),
				teamRepository.save(new Team(cohort11, "Team B")),
				teamRepository.save(new Team(cohort11, "Team C"))
		);

		Member admin = memberRepository.save(new Member(
				ADMIN_LOGIN_ID,
				passwordEncoder.encode(ADMIN_PASSWORD),
				"관리자",
				"010-0000-0000",
				MemberStatus.ACTIVE,
				MemberRole.ADMIN
		));

		CohortMember adminCohortMember = cohortMemberRepository.save(new CohortMember(
				admin,
				cohort11,
				cohort11Parts.get(0),
				teams.get(0),
				INITIAL_DEPOSIT,
				0
		));

		depositHistoryRepository.save(new DepositHistory(
				adminCohortMember,
				DepositType.INITIAL,
				INITIAL_DEPOSIT,
				INITIAL_DEPOSIT,
				null,
				"Initial deposit"
		));
	}
}
