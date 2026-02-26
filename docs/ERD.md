# ERD

![ERD](erd.png)

## 1. 데이터 모델 개요
이 프로젝트는 출석 관리 도메인을 중심으로 다음 4개 축으로 모델링했다.
- 회원/기수 축: `Member`, `Cohort`, `CohortMember`, `Part`, `Team`
- 일정/QR 축: `Session`, `QrCode`
- 출석 축: `Attendance`
- 보증금 이력 축: `DepositHistory`

핵심은 `CohortMember`를 기준으로 기수 단위 상태(보증금, 공결횟수)를 분리한 점이다.

---

## 2. 엔티티 설명

### 2.1 Member
- 역할: 시스템 사용자(관리자/일반회원) 기본 계정 정보
- 주요 컬럼:
  - `loginId` (unique)
  - `password` (BCrypt 해시)
  - `status` (`ACTIVE`, `INACTIVE`, `WITHDRAWN`)
  - `role` (`ADMIN`, `MEMBER`)
- 비고:
  - 회원 탈퇴는 물리 삭제가 아니라 `WITHDRAWN` 상태 전환

### 2.2 Cohort
- 역할: 기수 정보(예: 10기, 11기)
- 주요 컬럼:
  - `generation` (unique)
  - `name`
- 비고:
  - 현재 운영 기수는 11기 고정 정책을 서비스 레이어에서 적용

### 2.3 Part
- 역할: 기수별 파트(SERVER, WEB, iOS, ANDROID, DESIGN)
- 주요 컬럼:
  - `cohort_id` (FK)
  - `name`
- 비고:
  - 파트는 기수 소속 데이터이므로 `Cohort`와 N:1

### 2.4 Team
- 역할: 기수별 팀 정보(Team A/B/C)
- 주요 컬럼:
  - `cohort_id` (FK)
  - `name`

### 2.5 CohortMember
- 역할: 특정 회원의 특정 기수 소속 상태
- 주요 컬럼:
  - `member_id` (FK)
  - `cohort_id` (FK)
  - `part_id` (nullable FK)
  - `team_id` (nullable FK)
  - `deposit`
  - `excuseCount`
- 제약:
  - `(member_id, cohort_id)` unique
- 모델링 의도:
  - 보증금/공결횟수는 회원 전역 속성이 아니라 기수별 속성으로 분리

### 2.6 Session
- 역할: 정기 모임 일정
- 주요 컬럼:
  - `cohort_id` (FK)
  - `title`, `date`, `time`, `location`
  - `status` (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- 비고:
  - 삭제는 물리 삭제가 아니라 `CANCELLED` 상태 전환

### 2.7 QrCode
- 역할: 출석 체크용 QR 토큰
- 주요 컬럼:
  - `session_id` (FK)
  - `hashValue` (UUID 문자열)
  - `expiresAt`
- 제약:
  - `hashValue` unique
- 비고:
  - 세션당 활성 QR 1개 정책은 DB 제약 대신 서비스 로직으로 보장

### 2.8 Attendance
- 역할: 회원-일정 단위 출석 결과
- 주요 컬럼:
  - `session_id` (FK)
  - `member_id` (FK)
  - `qrcode_id` (nullable FK)
  - `status` (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`)
  - `lateMinutes`, `penaltyAmount`, `reason`, `checkedInAt`
- 제약:
  - `(session_id, member_id)` unique
- 비고:
  - QR 체크인과 관리자 수동 등록을 동일 테이블로 통합

### 2.9 DepositHistory
- 역할: 보증금 변동 감사 로그
- 주요 컬럼:
  - `cohort_member_id` (FK)
  - `attendance_id` (nullable FK)
  - `type` (`INITIAL`, `PENALTY`, `REFUND`)
  - `amount`, `balanceAfter`, `description`
- 비고:
  - 보증금 변화는 모두 이력으로 남겨 추적 가능하도록 설계

---

## 3. 관계 요약
- `Member 1:N CohortMember`
- `Cohort 1:N CohortMember`
- `Cohort 1:N Part`
- `Cohort 1:N Team`
- `Cohort 1:N Session`
- `Session 1:N QrCode`
- `Session 1:N Attendance`
- `Member 1:N Attendance`
- `QrCode 1:N Attendance` (optional reference)
- `CohortMember 1:N DepositHistory`
- `Attendance 1:N DepositHistory` (optional reference)

---

## 4. 데이터 정합성 규칙
- 로그인 ID 중복 금지: `members.login_id` unique
- 기수 중복 금지: `cohorts.generation` unique
- 동일 기수 중복 소속 금지: `(member_id, cohort_id)` unique
- 동일 일정 중복 출석 금지: `(session_id, member_id)` unique
- QR 해시 중복 금지: `qrcodes.hash_value` unique

서비스 레이어 규칙:
- 세션 삭제는 `CANCELLED` 상태 전환
- 회원 탈퇴는 `WITHDRAWN` 상태 전환
- 출석 수정 시 벌금 차이(`diff`)만큼 보증금 자동 조정
- 보증금 부족 시 `DEPOSIT_INSUFFICIENT`
- 공결 횟수 최대 3회

---

## 5. 모델링 의사결정 메모
- 출석/벌금/보증금은 기수 문맥이 강해 `CohortMember` 중심 모델 채택
- 운영 확장성(기수 증가) 대비를 위해 기수별 파트/팀 테이블 분리
- 감사 가능성을 위해 금전 변화는 `DepositHistory`로 이벤트성 저장
