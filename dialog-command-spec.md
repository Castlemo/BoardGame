# Remote Dialog Command Specification (Step A)

이 문서는 `taskpack.md` Step A의 결과물로, 네트워크 다이얼로그 전환 시 사용할
`DialogCommandPayload`/`DialogResponsePayload`의 필드를 정의한다. 호스트는 현재
턴 플레이어가 **로컬인지/원격인지** 판별한 뒤,
원격이면 아래 사양에 맞는 CMD 패킷을 송신하고, 응답(REP)을 받아 게임 흐름을
이어간다.

## 공통 규칙

- 모든 `DialogCommandPayload`에는 `requestId`, `dialogType`, `playerIndex`가 포함된다.
- `attributes` 맵에 담기는 값은 문자열이므로,
  정수는 10진 문자열(`123456`), 불리는 `true`/`false`로 직렬화한다.
- `DialogResponsePayload`에는 공통 필드 외에 `result` 문자열과
  필요 시 추가 attribute가 포함된다.
- 응답이 필요 없는 다이얼로그도 클라이언트 쪽 UI 닫힘 시
  `result=ACK`로 회신해 future를 정리한다.

## 입력형 다이얼로그 사양

| DialogType | 유발 상황 | Command Attributes | Response (`result` 및 추가 attrs) | 비고 |
|------------|-----------|--------------------|------------------------------------|------|
| `LEVEL_SELECTION` | 도시/랜드마크 미소유 땅 매입 (`purchaseCity`) | `cityName` (String), `price` (int, 기본 땅 값), `playerCash` (int) | `result=LEVEL_SELECTED` + `selectedLevel` (int, 1~3) / `result=CANCEL` (선택 안함) | `selectedLevel`은 0을 허용하지 않으며, 취소 시 속성 미전송 |
| `TOURIST_PURCHASE` | 관광지 최초 방문 또는 `purchaseCity()`에서 관광지 매입 버튼 | `spotName` (String), `price` (int), `playerCash` (int) | `result=CONFIRM` 혹은 `result=CANCEL` | `CONFIRM`일 때 추가 속성 없음 (규칙 엔진이 추가 비용 계산) |
| `TAKEOVER_CONFIRM` | 통행료 지불 후 도시/관광지 인수 버튼 | `cityName` (또는 관광지명), `ownerName` (String), `level` (int, 관광지는 1), `cost` (int, 인수 비용), `playerCash` (int) | `result=CONFIRM` 혹은 `result=CANCEL` | 도시/관광지 공통. 후속 로직은 `result`만 참고 |
| `TOURIST_CHOICE` | 관광지 주인이 잠금/추가 주사위를 선택할 때 | `spotName` (String) | `result=LOCK` 또는 `result=EXTRA_ROLL` | 추가 속성 없음 |

## 표시/안내형 다이얼로그 (ACK 응답)

다음 다이얼로그는 클라이언트에서 UI만 표시하면 되며 입력은 없다.  
모두 `result=ACK`로 응답하고 추가 attribute는 비운다.

| DialogType | Command Attributes |
|------------|--------------------|
| `CITY_SELECTION` | (없음) – “보드에서 도시를 클릭하세요” 안내만 표시 |
| `DOUBLE_SUPPRESSED` | `diceValue` (int), `consecutive` (int) |
| `ISLAND_STATUS` | `jailTurns` (int) |
| `CHANCE_REWARD` | `amount` (int) |
| `WORLD_TOUR` | (없음) |
| `DUAL_MAGNETIC` | `cityName` (String), `pulledCount` (int) |
| `TOLL_PAYMENT` | `cityName`, `ownerName` (String), `level` (int), `toll` (int), `olympic` (boolean), `playerCash` (int) |
| `TAX_PAYMENT` | `playerCash` (int), `taxAmount` (int) |
| `OLYMPIC` | (없음) |
| `PHASE_DELETE` | `cityName` (String) |
| `DOUBLE_ROLL` | `diceValue` (int), `consecutive` (int) |
| `UPGRADE_GUIDE` | `title` (String), `message` (String) |
| `GAME_OVER` | `winner` (String), `victoryType` (String), `cash` (int) |
| `ERROR` | `title` (String), `message` (String) |

> 참고: `TOLL_PAYMENT`, `TAX_PAYMENT` 등도 현재는 단순 안내이지만,
> 향후 “자동 지불” 외 대안을 추가하고 싶다면 위 표에서
> `result`를 새 enum 값으로 확장하면 된다.

## 응답 처리 패턴

1. 호스트는 `requestDialogFromPlayer(playerIndex, dialogType, attrs)` 호출 시
   위 표에 맞춰 attribute를 구성하고, `CompletableFuture<DialogResponsePayload>`를
   `pendingDialogResponses`에 저장한다.
2. 로컬 플레이어라면 기존 스윙 다이얼로그에서 결과를 읽어
   동일한 `result`/attribute 형태로 future를 완료한다.
3. 원격 플레이어라면 클라이언트가 `DialogResponsePayload`를
   그대로 돌려주므로, GameUI는 `result`와 필요한 attribute만 참조하면 된다.
