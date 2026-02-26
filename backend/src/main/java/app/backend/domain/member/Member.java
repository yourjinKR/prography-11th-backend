package app.backend.domain.member;

import app.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String loginId;

	@Column(nullable = false, length = 255)
	private String password;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberRole role;

	protected Member() {
	}

	public Member(String loginId, String password, String name, String phone, MemberStatus status, MemberRole role) {
		this.loginId = loginId;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.status = status;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getLoginId() {
		return loginId;
	}

	public String getPassword() {
		return password;
	}

	public String getName() {
		return name;
	}

	public String getPhone() {
		return phone;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public MemberRole getRole() {
		return role;
	}

	public void updateProfile(String name, String phone) {
		if (name != null) {
			this.name = name;
		}
		if (phone != null) {
			this.phone = phone;
		}
	}

	public void withdraw() {
		this.status = MemberStatus.WITHDRAWN;
	}
}
