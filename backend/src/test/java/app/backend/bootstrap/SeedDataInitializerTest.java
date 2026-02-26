package app.backend.bootstrap;

import app.backend.domain.cohort.CohortRepository;
import app.backend.domain.cohort.PartRepository;
import app.backend.domain.cohort.TeamRepository;
import app.backend.domain.deposit.DepositHistoryRepository;
import app.backend.domain.member.CohortMemberRepository;
import app.backend.domain.member.MemberRepository;
import app.backend.domain.member.MemberRole;
import app.backend.domain.member.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeedDataInitializerTest {

	@Autowired
	private CohortRepository cohortRepository;

	@Autowired
	private PartRepository partRepository;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CohortMemberRepository cohortMemberRepository;

	@Autowired
	private DepositHistoryRepository depositHistoryRepository;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

	@Test
	void seedsDefaultData() {
		assertThat(cohortRepository.count()).isEqualTo(2);
		assertThat(partRepository.count()).isEqualTo(10);
		assertThat(teamRepository.count()).isEqualTo(3);

		var admin = memberRepository.findByLoginId("admin");
		assertThat(admin).isPresent();
		assertThat(admin.get().getStatus()).isEqualTo(MemberStatus.ACTIVE);
		assertThat(admin.get().getRole()).isEqualTo(MemberRole.ADMIN);
		assertThat(passwordEncoder.matches("admin1234", admin.get().getPassword())).isTrue();

		assertThat(cohortMemberRepository.count()).isEqualTo(1);
		assertThat(depositHistoryRepository.count()).isEqualTo(1);
	}
}
