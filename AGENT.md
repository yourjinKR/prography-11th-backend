# Prography 11th Backend - AGENT Working Guide

## 1) 목적
- 이 문서는 `11th-assignment-be-api-spec` 문서를 기준으로 과제를 스펙 기반으로 개발하기 위한 작업 기준서다.
- 이후 구현/테스트/문서화 작업은 이 문서를 체크리스트처럼 사용한다.

## 2) 수정 금지 범위 (절대 불가)
- `11th-assignment-be-api-spec/**`
- 위 경로의 모든 파일은 과제 제공 문서이므로 읽기 전용으로 취급한다.

## 3) 개발 고정 조건
- Language/Runtime: Java 17+
- Framework: Spring Boot
- DB: H2
- 인증/인가:
  - JWT/세션 구현 없음
  - 로그인은 `loginId + password` 검증만 수행
  - 비밀번호는 BCrypt 해시 저장/검증
- Base URL: `/api/v1`
- 공통 응답 포맷:
  - 성공: `{"success": true, "data": ..., "error": null}`
  - 실패: `{"success": false, "data": null, "error": {"code": "...", "message": "..."}}`

## 4) 구현 범위
- 총 25개 API
  - 필수 16개
  - 가점 9개

### 필수 API 16
1. `POST /api/v1/auth/login`
2. `GET /api/v1/members/{id}`
3. `POST /api/v1/admin/members`
4. `GET /api/v1/admin/members`
5. `GET /api/v1/admin/members/{id}`
6. `PUT /api/v1/admin/members/{id}`
7. `DELETE /api/v1/admin/members/{id}`
8. `GET /api/v1/admin/cohorts`
9. `GET /api/v1/admin/cohorts/{cohortId}`
10. `GET /api/v1/sessions`
11. `GET /api/v1/admin/sessions`
12. `POST /api/v1/admin/sessions`
13. `PUT /api/v1/admin/sessions/{id}`
14. `DELETE /api/v1/admin/sessions/{id}`
15. `POST /api/v1/admin/sessions/{sessionId}/qrcodes`
16. `PUT /api/v1/admin/qrcodes/{qrCodeId}`

### 가점 API 9
17. `POST /api/v1/attendances`
18. `GET /api/v1/attendances`
19. `GET /api/v1/members/{memberId}/attendance-summary`
20. `POST /api/v1/admin/attendances`
21. `PUT /api/v1/admin/attendances/{id}`
22. `GET /api/v1/admin/attendances/sessions/{sessionId}/summary`
23. `GET /api/v1/admin/attendances/members/{memberId}`
24. `GET /api/v1/admin/attendances/sessions/{sessionId}`
25. `GET /api/v1/admin/cohort-members/{cohortMemberId}/deposits`

## 5) 도메인 규칙 핵심
- 회원
  - `loginId` 유니크
  - 삭제는 soft-delete (`WITHDRAWN`)
  - 회원 등록 시 CohortMember 및 초기 보증금 100,000 생성
- 기수
  - 현재 운영 기수는 11기로 고정(관련 조회/검증에 일관 적용)
- 세션
  - 생성 시 QR 자동 생성
  - 삭제는 soft-delete (`CANCELLED`)
  - `CANCELLED`는 수정 불가, 회원용 세션 목록에서 제외
- QR
  - `hashValue`는 UUID 기반
  - 유효시간 24시간
  - 세션당 활성 QR 1개만 유지
  - 갱신 시 기존 QR 즉시 만료 + 신규 생성
- 출석/벌금/보증금
  - 출석 상태: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`
  - 벌금:
    - `PRESENT`: 0
    - `ABSENT`: 10,000
    - `LATE`: `min(lateMinutes * 500, 10000)`
    - `EXCUSED`: 0
  - 벌금 발생 시 보증금 차감 + DepositHistory 기록
  - 보증금 부족 시 `DEPOSIT_INSUFFICIENT`
  - 출석 수정 시 기존/신규 벌금 차이(diff)만큼 자동 조정
    - diff > 0: 추가 차감(PENALTY)
    - diff < 0: 환급(REFUND)
- 공결(EXCUSED)
  - 기수 내 최대 3회
  - 초과 시 `EXCUSE_LIMIT_EXCEEDED`

## 6) 에러 코드 구현 기준
- `api-spec/common.md`의 에러 코드를 표준으로 사용한다.
- 최소 구현 대상(자주 사용):
  - `INVALID_INPUT`, `INTERNAL_ERROR`
  - `LOGIN_FAILED`, `MEMBER_WITHDRAWN`, `MEMBER_NOT_FOUND`
  - `DUPLICATE_LOGIN_ID`, `COHORT_NOT_FOUND`, `PART_NOT_FOUND`, `TEAM_NOT_FOUND`, `COHORT_MEMBER_NOT_FOUND`
  - `SESSION_NOT_FOUND`, `SESSION_ALREADY_CANCELLED`, `SESSION_NOT_IN_PROGRESS`
  - `QR_NOT_FOUND`, `QR_INVALID`, `QR_EXPIRED`, `QR_ALREADY_ACTIVE`
  - `ATTENDANCE_NOT_FOUND`, `ATTENDANCE_ALREADY_CHECKED`
  - `EXCUSE_LIMIT_EXCEEDED`, `DEPOSIT_INSUFFICIENT`

## 7) 작업 순서 (권장)
1. 프로젝트 부트스트랩/패키지 구조/공통 응답+예외 처리
2. 엔티티/enum/리포지토리 + seed-data 반영
3. 필수 API 16개 구현
4. 필수 API 단위/통합 테스트
5. 가점 API 9개 구현
6. 가점 API 단위/통합 테스트
7. README + docs(ERD, 아키텍처, AI 사용 내역) 정리

## 8) 완료 기준 (DoD)
- 서버 실행 가능
- seed data 로드 상태에서 `admin/admin1234` 로그인 성공
- 필수 API 16개 스펙대로 동작
- 가점 API 9개 구현 시 스펙대로 동작
- 서비스/도메인 단위 테스트 포함
- README에 실행 방법과 환경 구성 포함

## 9) 작업 메모 규칙
- 구현/수정 시 반드시 해당 스펙 문서 경로를 커밋 메시지나 작업 로그에 남긴다.
- 예:
  - `api-spec/member/04-check-in.md`
  - `api-spec/admin/15-update-attendance.md`

## 10) 다음 작업 시작점
- 우선순위: 공통 응답/예외 처리 -> 도메인 모델/enum -> 필수 API부터 순차 구현
