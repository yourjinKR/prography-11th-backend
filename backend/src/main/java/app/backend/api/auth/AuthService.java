package app.backend.api.auth;

import app.backend.api.member.MemberDto;
import app.backend.domain.member.Member;
import app.backend.domain.member.MemberRepository;
import app.backend.domain.member.MemberStatus;
import app.backend.global.error.ApiException;
import app.backend.global.error.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder passwordEncoder;

	public AuthService(MemberRepository memberRepository, BCryptPasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public MemberDto login(LoginRequest request) {
		Member member = memberRepository.findByLoginId(request.loginId())
				.orElseThrow(() -> new ApiException(ErrorCode.LOGIN_FAILED));

		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new ApiException(ErrorCode.LOGIN_FAILED);
		}

		if (member.getStatus() == MemberStatus.WITHDRAWN) {
			throw new ApiException(ErrorCode.MEMBER_WITHDRAWN);
		}

		return MemberDto.from(member);
	}
}
