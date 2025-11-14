# Remote Dialog Stabilization Taskpack

> 목표: Step A~C 구현된 네트워크 다이얼로그 흐름을 실제 플레이 환경에서 안정화하고,
> 예외 상황·테스트 루틴을 통해 회귀 없이 배포 가능하도록 다듬는다.

---

## 1. Step D 마무리 점검 (상태 머신/타임아웃)

| Task | Description | Owner | ETA |
|------|-------------|-------|-----|
| D-1 | `GameUI` 상태 흐름 재검토: `WAITING_FOR_DIALOG_RESPONSE`에서 허용되는 이벤트 정의, 응답 완료 시 이전 단계(ROLL/ACTION) 복귀 확인 | Core | 0.5d |
| D-2 | 다이얼로그 fallback 동작 QA: 원격 응답 미수신(네트워크 끊김)·로컬 예외 발생 시 `buildFallbackDialogResponse`가 의도한 기본 선택(LOCK/CANCEL 등)으로 이어지는지 케이스별 검증 | QA | 0.5d |
| D-3 | 타임아웃 상수(`15s`)를 설정값으로 분리 (`game.properties` 등)하고, 호스트 UI에서 변경할 수 있는 Hook 제공 | Core | 0.25d |

### 검증 포인트
- 응답 대기 중 모든 버튼이 비활성화되며, 원격/로컬 입력 모두 무시되는지.
- fallback 이후 RuleEngine 호출이 한 번만 수행되는지 (중복 처리 방지 로그 확인).
- 예외 발생 시 `showErrorDialog` → `endTurn` 흐름이 반복 호출되지 않는지.

---

## 2. RemoteGameUI 신뢰성 강화

| Task | Description | Owner | ETA |
|------|-------------|-------|-----|
| R-1 | `handleDialogCommand` Receive Queue: 프레임 미초기화 상태에서 CMD가 누락되지 않도록 현재 큐 사이즈/메모리 사용 추적, 필요 시 상한/경고 로그 추가 | Net | 0.25d |
| R-2 | Duplicate response guard: 동일 `requestId`로 응답을 중복 전송하지 않도록 `inFlightRequests` 맵을 두고, 응답 전송 시 제거 | Net | 0.5d |
| R-3 | 클라이언트 다이얼로그에서 창 닫기/ESC 등 비정상 종료 시 기본 응답(`LOCK`/`CANCEL`)을 명시적으로 기록하여 호스트 로그에 이유 출력 | UI | 0.25d |

### 검증 포인트
- 클라이언트 슬롯 미배정 상태에서 CMD를 무시하는지 확인 (현재 guard 유지).
- `pendingDialogCommands`가 장시간 쌓일 경우 UI 프리즈 여부 점검.

---

## 3. 통합 테스트 & 자동화

| Task | Description | Owner | ETA |
|------|-------------|-------|-----|
| T-1 | 테스트 시나리오 스크립트화: `scripts/mock-client.sh` 같은 CLI 도구로 특정 다이얼로그 응답을 자동화, 회귀 테스트에 사용 | QA | 1d |
| T-2 | 로컬/원격 혼합 플레이 시나리오(2P 로컬 + 1P 원격)에서 모든 다이얼로그가 적절히 분기되는지 수동 테스트 체크리스트 작성 | QA | 0.5d |
| T-3 | 네트워크 단절/재연결 중 다이얼로그 요청이 살아있는 케이스 처리: `HostNetworkService`에 응답 타임아웃 도달 시 SLF4J 경고, `RemoteGameUI`는 재접속 후 미해결 CMD 폐기 | Net/Core | 1d |

---

## 4. 후속 개선 아이디어 (우선순위 낮음)

1. **Dialog batching**: 같은 턴에 여러 다이얼로그가 연속 발생할 경우(관광지 구매 → 선택) 클라이언트 입장에서 UX가 부자연스럽지 않도록 전환 애니메이션/토스트 안내를 추가.
2. **Analytics Hook**: 어떤 다이얼로그에서 취소/확정이 많이 발생하는지 수집해 밸런스 튜닝에 활용.
3. **Headless Testing**: Swing 없이도 CMD→RESP를 검증할 수 있는 headless 모드(`RemoteGameUIHeadless`) 추가해 CI에서 빠르게 돌릴 수 있도록 준비.

---

## 5. 운영 체크리스트
- [ ] `DialogResponsePayload` 로그 샘플 10개 이상 확보 (원격/로컬 각각)하여 requestId/attributes 직렬화가 올바른지 검증
- [ ] 호스트 UI 로그에 “응답 대기 타임아웃” 메시지가 지나치게 자주 찍히지 않는지(발생 시 네트워크 상태 확인)
- [ ] 클라이언트 측에서 다이얼로그 강제 종료 시 사용자에게 “응답이 전송되지 않아 기본값이 적용된다”는 안내 Toast 추가 여부 검토

---

**타임라인 제안**
- Day 1: Step D QA (D-1~D-3) + Remote 신뢰성 (R-1). 저녁에 부분 회귀 테스트.
- Day 2: R-2/R-3 + 테스트 자동화 T-1 초안. 오후에 멀티플레이 수동 검증.
- Day 3: T-2/T-3 마무리 및 운영 체크리스트 점검 → PR 작성.

