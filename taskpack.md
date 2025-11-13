# Remote Dialog Network Taskpack

> 목표: 클라이언트 차례에서 발생하는 **모든 의사결정 다이얼로그**를 호스트가 직접 띄우지 않고,  
> 네트워크 CMD/REQ 패킷을 통해 **클라이언트 화면에서만** 표시하고 응답을 받아 게임 상태를 진행한다.

---

## 1. 문제 요약

- 현재 `GameUI`는 매입/업그레이드/인수 등 대부분의 다이얼로그를 **호스트 로컬 UI**에서 띄운다.
- 네트워크 클라이언트가 자신의 차례를 진행해도 호스트가 대신 버튼을 눌러야 하므로 UX가 깨지고, 실제 결정도 호스트가 강제하게 된다.

## 2. 기대 동작

1. 호스트가 다이얼로그가 필요한 상황을 감지하면, 해당 플레이어 슬롯이 로컬인지/원격인지 판별한다.
2. 원격이면 `DialogCommand`(CMD) 패킷을 발행하고 응답을 `CompletableFuture` 등으로 대기한다.  
   로컬이면 기존 다이얼로그를 띄우고 결과를 즉시 future에 채워 넣는다.
3. 클라이언트(`RemoteGameUI`)는 CMD 패킷을 수신하면 로컬 JDialog를 표시하고, 선택 결과를 `DialogResponse`(REQ)로 호스트에게 돌려준다.
4. 호스트는 응답을 수신하면 대기 중이던 future를 완료하고, 결과에 따라 다음 게임 상태를 이어간다.

---

## 3. 구현 순서

### Step A. 다이얼로그 사양 정의
- 모든 **입력형 다이얼로그**를 목록화하고, 각 타입별 **요청 필드**와 **응답 값**을 작성한다.
- 예시 지침:
  | DialogType | 요청 필드 예시 | 응답 값/필드 |
  |------------|----------------|--------------|
  | `PURCHASE_CITY` | `cityName`, `price`, `playerCash` | `result=CONFIRM/CANCEL` |
  | `UPGRADE_CITY`  | `cityName`, `currentLevel`, `maxLevel`, `cost` | `selectedLevel` (또는 `CANCEL`) |
  | … | … | … |
- 사양이 확정되면 `DialogCommandPayload`/`DialogResponsePayload`에 어떤 key를 넣을지 결정하고 문서화한다.

### Step B. 호스트 흐름 전환 (`GameUI`)
1. 각 다이얼로그 호출부에서 `requestDialogFromPlayer(...)`를 호출하도록 리팩터링한다.
2. `requestDialogFromPlayer`는 다음을 수행해야 한다.
   - 로컬 플레이어라면 기존 다이얼로그를 실행하고, 선택 결과를 `DialogResponsePayload`로 만들어 future를 완료한다.
   - 원격 플레이어라면 `DialogCommandPayload` + `requestId`를 만들고 해당 클라이언트에게 송신, `pendingDialogResponses`에 future를 저장한다.
3. 응답을 받은 뒤에는 기존 `dialog.isConfirmed()` 등 직접 조회 로직을 모두 future 기반 비동기 처리로 바꿔야 한다.  
   (예: `requestDialogFromPlayer(...).thenAccept(response -> handlePurchaseResponse(response))`)

### Step C. 클라이언트 처리 (`RemoteGameUI`)
1. `MessageType.DIALOG_COMMAND` 수신 시:
   - `DialogCommandPayload`를 파싱하고, 타겟 플레이어인지 확인한다.
   - 해당 타입에 맞는 로컬 UI(JDialog)를 띄운다. 기존 다이얼로그를 재사용할 수 있다면 그대로 사용하되, 결과를 event로 가공한다.
2. 사용자가 선택하면 `DialogResponsePayload`를 만들어 `MessageType.DIALOG_RESPONSE`로 호스트에 전송한다.  
   응답에는 반드시 `requestId`와 `result`/추가 필드를 포함한다.

### Step D. 상태 머신/콜백 정리
1. `GameUI`는 응답 future가 완료되기 전까지 현재 상태를 유지하거나, 명시적인 `WAITING_FOR_DIALOG_RESPONSE` 상태로 두어 다른 입력을 막는다.
2. 응답 핸들러 내부에서만 규칙 엔진(`RuleEngine`)을 호출하고, 필요 시 UI/로그를 갱신한다.
3. 에러/타임아웃 정책이 필요하면 future에 타임아웃을 설정하거나, 응답이 없을 경우 기본 선택을 적용하도록 로직을 추가한다.

---

## 4. 검증 체크리스트

- [ ] 모든 다이얼로그 타입이 CMD/REQ 포맷으로 정의되어 있는가?
- [ ] 호스트에서 로컬 플레이어일 때는 기존 다이얼로그가 정상 작동하는가?
- [ ] 원격 플레이어일 때 호스트 UI가 띄워지지 않고, 클라이언트에서만 다이얼로그가 나타나는가?
- [ ] 클라이언트 응답이 없을 경우(연결 끊김 등) 안전하게 종료/기본값 처리가 되는가?
- [ ] 신규 메시지 타입(`DIALOG_COMMAND`, `DIALOG_RESPONSE`)이 양쪽 모두에서 직렬화/역직렬화되는가?

---

### 참고
- 이미 추가된 인프라:
  - `DialogCommandPayload`, `DialogResponsePayload`, `MessageType` 확장.
  - `GameUI`의 `requestDialogFromPlayer(...)`, `pendingDialogResponses`, `handleDialogResponse(...)`.
  - `RemoteGameUI`의 `handleDialogCommand(...)` (현재 로그만 출력하므로 실제 다이얼로그 호출/응답 전송을 붙여야 함).

위 순서를 따라가면 “클라이언트 턴 다이얼로그는 클라이언트 UI에서만 표시되고, 응답만 호스트로 돌아오는” 구조로 안전하게 전환할 수 있다.
