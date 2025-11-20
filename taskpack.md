# 모두의 마블 코드 플로우 taskpack

가벼운 온보딩용으로 주요 흐름과 책임을 정리했습니다. 네트워크 멀티플레이 기준(호스트 권위 모델)과 로컬 플레이가 동일한 GameUI 로직을 공유합니다.

## 1) 진입과 모드 선택
- `Main.main` → `showGameModeSelection()`에서 로컬/네트워크 선택.
- 로컬: `startLocalGame()`가 `GameUI(numPlayers, initialCash)`를 바로 띄움.
- 네트워크: `showNetworkMenu()` → `createRoom()`(호스트) 또는 `joinRoom()`(게스트).
  - 호스트: `GameServer` 시작 → 자기 자신을 `GameClient`로 연결 → `LobbyPanel` 표시.
  - 게스트: `GameClient.connect()` 성공 후 `PLAYER_JOIN` 응답에서 `LobbyPanel`을 띄움.

## 2) 호스트/클라이언트 역할
- 호스트만 실제 게임 로직을 실행; 모든 행동/이벤트를 계산 후 브로드캐스트.
- 클라이언트는 액션 의도(ROLL, BUY, UPGRADE 등)를 서버로 보내고, 서버가 호스트 `GameUI`에 위임 (`Main.handleServerSideAction`).
- 상태 전파: `GameStateSnapshot`를 `MessageType.GAME_STATE_UPDATE`로 브로드캐스트; 클라이언트 `GameUI.applyNetworkSnapshot`이 UI와 내부 상태를 덮어씀.
- 이벤트 전파: 호스트 `pushNetworkEvent`가 이벤트(`CHANCE_EVENT`, `TOLL_EVENT`, `PHASE_DELETE` 등)를 전송, 클라이언트는 `handleRemote*` 계열에서 다이얼로그/로그만 표시.

## 3) GameUI 상태 머신 (공통)
- 상태: `WAITING_FOR_ROLL` → `ANIMATING_MOVEMENT` → `WAITING_FOR_ACTION` 등.
- 턴 시작(`startTurn`):
  - 파산/무인도/전국철도 여부 확인 후 버튼/보드 인터랙션 활성화 설정.
  - 3턴마다 `executePhaseDelete()`로 보드 이벤트(페이즈 딜리트) 처리.
- 주사위:
  - `DiceGauge`로 구간 선택(S1~S4) + 홀짝 모드(ODD/EVEN).
  - 더블 억제 확률 적용 → `DiceAnimationPanel` 애니메이션 후 이동.
- 이동/도착:
  - `movePlayer` → 모션 타이머로 경유 타일마다 애니메이션, 최종 타일은 `currentTile`.
  - `handleTileLanding`이 타입별 분기:
    - CITY: 미소유 시 구매/레벨선택, 소유 시 업그레이드 또는 인수, 타인 소유 시 통행료/인수.
    - TOURIST_SPOT: 구매만 가능, 통행료는 고정 2.0x, 잠금/인수 규칙 포함.
    - ISLAND: `jailTurns=2`, 더블 무효 처리.
    - CHANCE: 랜덤 보상(기본 300,000) + 머니 이펙트.
    - TAX: 보유 현금의 10% 지불(`RuleEngine.taxRate`).
    - OLYMPIC: 선택 도시 통행료 2배 부스트, 더블 무효.
    - WORLD_TOUR/RAILROAD: 다음 턴 원하는 칸 이동 티켓 지급.
- 턴 종료: 더블 체크(최대 3회; 억제 로직 포함), 파산 처리, 다음 플레이어로 이동.

## 4) 룰 계산 핵심 (`RuleEngine`)
- 급여/탈출: 출발지 월급 500,000, 무인도 보석금 200,000.
- 도시 가격/레벨: 누적 비용 `calculateLevelCost` (1=100%, 2=130%, 3=160%).
- 통행료: `baseToll × tollMultiplier[level]` → 색 독점 시 1.5배 → 올림픽 부스트 시 2배.
- 관광지: 가격의 2배 통행료, 잠금/인수 예외 처리.
- 페이즈 딜리트: 3턴마다 이벤트 발동(네트워크 이벤트로도 공유).

## 5) UI 구성
- `GameFrame`에 주요 패널 장착:
  - `BoardPanel`: 보드 렌더링 + 타일 클릭(전국철도/선택형 이벤트).
  - `ActionPanel`: 주사위/매입/업그레이드/인수/패스/보석금 버튼 + `DiceGauge`.
  - `OverlayPanel`: 로그, 플레이어 인포, 채팅(`ChatPanel`), 홀짝 버튼, 머니 이펙트.
- 다이얼로그: 찬스/무인도/세계여행/올림픽/랜드마크 마그네틱/레벨 선택 등 턴 상황에 맞춰 표시.
- 로그: 모든 주요 이벤트는 `ControlPanel.addLog`를 통해 누적.

## 6) 네트워크 프로토콜 개요
- 메시지 타입: `MessageType`에 정의 (연결/턴액션/이벤트/채팅/하트비트).
- 직렬화: `MessageSerializer`가 JSON 문자열로 변환, 소켓 송수신.
- 하트비트: `PING`/`PONG`(5초/15초 타임아웃)으로 연결 감시, 끊길 시 파산 처리.
- 방 관리: `RoomManager`가 플레이어 목록/최대 인원 관리 및 `LobbyPanel`에 반영.

## 7) 데이터 모델
- 보드: `Board` 32칸, `Tile.Type` START/CITY/TOURIST_SPOT/ISLAND/CHANCE/WELFARE/RAILROAD/TAX/OLYMPIC/WORLD_TOUR.
- 도시/관광지: `City`(colorGroup, level, landmark 여부), `TouristSpot`(잠금 상태).
- 플레이어: `Player`(id/name/cash/pos/jailTurns/bankrupt/railroadTicket/extraChance).

## 8) 파일 길잡이
- 엔트리/모드: `Main.java`
- 게임 로직+UI: `core/GameUI.java`, `core/RuleEngine.java`
- 모델/보드: `model/Board.java`, `model/City.java`, `model/TouristSpot.java`, `model/Player.java`
- 네트워크: `network/server/*.java`, `network/client/*.java`, `network/protocol/*.java`, `network/sync/*.java`, `network/ui/*.java`
- 리소스: `resources/rules.json` (추후 룰 외부화 여지), `asset/` (보드/아이콘 이미지)

## 9) 앞으로 볼 지점
- 규칙 상수는 현재 코드 하드코딩(`RuleEngine`); `resources/rules.json`과 싱크 필요.
- 네트워크 이벤트/상태 분리가 중첩되어 있어, 델타 전송 또는 액션 기반 리플레이를 고려 가능.
- UI 3턴 페이즈 딜리트/랜드마크 마그네틱 등 특수 규칙이 많으니, 신규 기능 추가 시 `pushNetworkEvent`에서 누락되지 않는지 확인.
