# 스펙 정합성 점검 기록

## 점검 일시
- 2026-02-25

## 이슈 목록
1. 회원 기수 변경 시 기존 기수의 `part/team`가 그대로 재사용될 수 있는 문제
2. 시드 데이터 표기(`10th/11th`, `Admin`)가 스펙 표기(`10기/11기`, `관리자`)와 다른 문제
3. 공통 에러 `message`가 영문으로 내려가 스펙 메시지와 차이 날 수 있는 문제
4. `GET /admin/attendances/sessions/{sessionId}/summary`가 `sessionId`를 집계에 반영하지 않던 문제

## 조치 내용
1. 기수 변경(`cohortId` 변경) 시 `part/team`는 새 기수 기준으로만 반영되도록 수정
- 새 기수로 바뀌면 `partId/teamId` 미전달 시 `null` 처리
- 새 기수로 바뀌면 이전 기수 소속 `part/team` 재사용 금지

2. 시드 데이터 표기 수정
- Cohort name: `10기`, `11기`
- Admin name: `관리자`

3. 에러 메시지 한국어 표준 문구로 정비
- 스펙 표의 의미와 맞는 한국어 메시지로 통일

4. 세션 출결 요약 집계 기준 수정
- `sessionId` 단위 출결만 집계하도록 변경

## 검증
- `./gradlew test` 통과
