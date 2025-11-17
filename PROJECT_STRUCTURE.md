# BoardGame 프로젝트 구조

> Java/Swing 기반 모두의마블 스타일 보드게임.
> 길게 눌러 조절하는 주사위 게이지, 반응형 UI, 네트워크 멀티플레이어, 실시간 채팅을 지원한다.

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **언어 / UI** | Java 17 · Swing (커스텀 다크 테마) |
| **게임 모드** | 로컬 (2-4명, 동일 기기) · 네트워크 멀티플레이어 (TCP/IP) |
| **보드 구조** | 9×9 외곽 32칸 (시계방향 순환) |
| **시작 자금** | 1,500,000원 |
| **월급** | START 칸 통과 시 200,000원 |
| **승리 조건** | 파산 승리 · 라인 독점 · 3색 독점 |
| **진입점** | `src/com/marblegame/Main.java` |

---

## 2. 디렉터리 구조

```
src/com/marblegame/
├── Main.java                      # 앱 진입점, 게임 모드 선택, 네트워크 연결 관리
├── core/
│   ├── GameUI.java                # 주 컨트롤러 (게임 상태, 턴 관리, 네트워크 동기화)
│   ├── RuleEngine.java            # 순수 게임 규칙 엔진 (금전 계산, 독점 판정)
│   └── Game.java                  # 콘솔 레거시 (참고용)
├── model/
│   ├── Board.java                 # 32칸 타일 초기화 및 관리
│   ├── Tile.java                  # 타일 기본 클래스
│   ├── City.java                  # 도시 타일 (레벨, 소유자, 통행료)
│   ├── TouristSpot.java           # 관광지 타일 (잠금 상태)
│   ├── Player.java                # 플레이어 상태 (현금, 위치, 플래그)
│   ├── Dice.java                  # 주사위 로직 (2D6, 더블 판정)
│   └── DiceGauge.java             # 게이지 바 모델 (구간별 확률)
├── ui/
│   ├── GameFrame.java             # 메인 윈도우 프레임
│   ├── BoardPanel.java            # 보드 렌더링 + 이동 애니메이션
│   ├── OverlayPanel.java          # 플레이어 카드, 주사위, 버튼, 채팅
│   ├── ChatPanel.java             # 실시간 채팅 UI (네트워크 전용)
│   ├── DiceAnimationPanel.java    # 주사위 굴림 애니메이션
│   ├── GaugePanel.java            # 주사위 게이지 바 UI
│   ├── CompactPlayerCard.java     # 플레이어 정보 카드
│   ├── GameModeDialog.java        # 게임 모드 선택 다이얼로그
│   └── *Dialog.java               # 각종 게임 내 다이얼로그
└── network/
    ├── NetConstants.java          # 네트워크 상수 (포트, 타임아웃)
    ├── client/
    │   ├── GameClient.java        # TCP 클라이언트 소켓 관리
    │   └── ServerListener.java    # 서버 메시지 수신 스레드
    ├── server/
    │   ├── GameServer.java        # TCP 서버 (클라이언트 수락, 브로드캐스트)
    │   ├── ClientHandler.java     # 개별 클라이언트 처리 스레드
    │   └── RoomManager.java       # 방 참가자 관리
    ├── protocol/
    │   ├── Message.java           # 네트워크 메시지 컨테이너
    │   ├── MessageType.java       # 메시지 타입 열거형 (40+ 종류)
    │   └── MessageSerializer.java # JSON 직렬화/역직렬화
    ├── sync/
    │   ├── GameStateSnapshot.java # 게임 상태 스냅샷 객체
    │   └── GameStateMapper.java   # 상태 ↔ Map 변환
    └── ui/
        ├── NetworkMenuDialog.java # 네트워크 메뉴
        ├── CreateRoomDialog.java  # 방 생성 다이얼로그
        ├── JoinRoomDialog.java    # 방 참가 다이얼로그
        └── LobbyPanel.java        # 대기실 UI
```

---

## 3. 시스템 아키텍처

### 3.1 로컬 모드 아키텍처

```
[사용자 입력] → [GameUI 컨트롤러] → [RuleEngine] → [Model 계층]
                       ↓
              [UI 패널 업데이트]
```

- **GameUI**: 게임의 모든 상태를 소유하고 턴 진행을 총괄
- **RuleEngine**: 순수 계산만 담당 (외부 상태 없음, 테스트 용이)
- **Model**: 데이터만 보관 (Board, Player, Tile 등)

### 3.2 네트워크 모드 아키텍처

```
[클라이언트 A] ←──TCP──→ [서버] ←──TCP──→ [클라이언트 B]
      ↓                      ↓                    ↓
  [GameUI]              [브로드캐스트]         [GameUI]
      ↓                      ↓                    ↓
 [UI 표시]            [게임 상태 동기화]       [UI 표시]
```

**호스트-클라이언트 구조**:
1. 호스트가 서버를 생성하고 자신도 클라이언트로 접속
2. 호스트의 GameUI가 게임 로직을 실행하고 상태를 브로드캐스트
3. 클라이언트는 상태를 수신하여 UI만 업데이트
4. 클라이언트의 행동(주사위, 구매 등)은 서버를 통해 호스트에게 전달

---

## 4. 핵심 게임 로직

### 4.1 주사위 시스템

**게이지 기반 확률 조절**:
- 플레이어가 버튼을 길게 누르면 게이지가 0→100→0으로 순환
- 놓는 시점의 게이지 위치에 따라 주사위 합계의 확률이 변경

| 구간 | 게이지 위치 | 나올 수 있는 합계 |
|------|-------------|-------------------|
| S1 | 0–25% | 2, 3, 4 (낮은 숫자) |
| S2 | 25–50% | 4, 5, 6 (중간 낮음) |
| S3 | 50–75% | 7, 8, 9, 10 (중간 높음) |
| S4 | 75–100% | 9, 10, 11, 12 (높은 숫자) |

**더블 억제 알고리즘**:
```java
// 연속 더블을 방지하여 게임 밸런스 유지
if (연속더블횟수 == 1) 억제확률 = 60%;  // 2연속 더블 시도 시
if (연속더블횟수 == 2) 억제확률 = 20%;  // 3연속 더블 시도 시
if (연속더블횟수 >= 3) 억제확률 = 0%;   // 억제하지 않음

// 억제 시 다른 주사위 조합으로 대체 (합계는 유지)
예: 더블(4,4)=8 → 비더블(3,5)=8
```

**홀수/짝수 모드**:
- 플레이어가 선택 시 해당 숫자만 나오도록 합계 조정
- 짝수 모드에서 더블 억제 시에도 짝수 합계 보장

### 4.2 도시 소유권 & 통행료

**레벨 시스템**:
```
레벨 0: 빈 땅
레벨 1: 별장 (기본 통행료)
레벨 2: 빌딩 (통행료 x1.5)
레벨 3: 호텔 (통행료 x2)
레벨 4: 랜드마크 (통행료 x3, 인수 불가)
```

**통행료 계산**:
```java
기본통행료 = 도시가격 * 0.3
실제통행료 = 기본통행료 * 레벨보너스 * 독점보너스 * 올림픽보너스
```

**인수 시스템**:
- 다른 플레이어 소유 도시에 도착 시 인수 가능
- 인수 비용 = 현재 도시 총 가치 * 2
- 랜드마크(레벨 4)는 인수 불가

### 4.3 독점 판정 알고리즘

**컬러 독점**: 같은 색상의 모든 도시를 소유
```java
// 색상별 도시 수 카운트
Map<CityColor, Integer> colorCount;
if (소유한도시수 == 해당색상총도시수) → 컬러독점 달성
```

**라인 독점**: 보드의 한 변(8칸)의 모든 도시를 소유
```java
// 보드의 4개 변 검사
상단: 타일 0-7
우측: 타일 8-15
하단: 타일 16-23
좌측: 타일 24-31
```

**3색 독점**: 서로 다른 3가지 색상을 모두 독점

### 4.4 승리 조건 로직

```java
void checkWinConditions() {
    // 1. 파산 승리: 1명만 남음
    int 생존자수 = countActivePlayers();
    if (생존자수 == 1) → 파산승리;

    // 2. 독점 승리
    for (Player p : players) {
        if (hasLineMonopoly(p) || hasTripleColorMonopoly(p)) {
            → 독점승리;
        }
    }
}
```

### 4.5 특수 타일 효과

| 타일 | 동작 로직 |
|------|-----------|
| **START** | 통과 시 월급 200,000원 지급, 더블 상태 초기화 |
| **CHANCE** | 랜덤 이벤트 발생 (보상금, 벌금, 이동 등) |
| **TAX** | 현재 보유 현금의 10% 징수 |
| **ISLAND** | 2턴 구금, 보석금(50,000원) 또는 더블로 탈출 |
| **RAILROAD** | 다음 턴에 원하는 칸으로 이동 가능 |
| **WORLD TOUR** | 철도와 동일, 단 즉시 이동 |
| **OLYMPIC** | 소유 도시 1곳 선택, 1회 통행료 2배 |

**랜드마크 마그네틱**:
자신의 랜드마크에 도착 시 양옆 4칸 범위 내의 다른 플레이어를 끌어와 통행료 징수

**Phase Delete**:
3턴마다 랜덤 도시 1개가 삭제됨 (이동 시 자동 스킵)

---

## 5. 네트워크 구현

### 5.1 기술 스택

- **프로토콜**: TCP/IP (신뢰성 보장)
- **포트**: 12345 (기본값)
- **직렬화**: JSON (MessageSerializer)
- **스레딩**: ExecutorService (비동기 처리)
- **하트비트**: 15초마다 PING/PONG

### 5.2 메시지 프로토콜

```java
public class Message {
    MessageType type;       // 메시지 종류
    String senderId;        // 보낸 플레이어 ID
    Map<String, Object> data;  // 페이로드
}
```

**주요 메시지 타입**:
- **연결 관리**: CONNECT, DISCONNECT, PLAYER_JOIN, PLAYER_LEAVE
- **게임 시작**: GAME_START, GAME_READY
- **턴 액션**: ROLL_DICE, BUY_CITY, UPGRADE, TAKEOVER, PASS
- **상태 동기화**: GAME_STATE_UPDATE, PLAYER_STATE_UPDATE
- **이벤트**: CHANCE_EVENT, TOLL_EVENT, OLYMPIC_EVENT
- **채팅**: CHAT_MESSAGE, CHAT_EMOJI
- **하트비트**: PING, PONG

### 5.3 게임 상태 동기화

**스냅샷 기반 동기화**:
```java
class GameStateSnapshot {
    List<PlayerState> players;     // 모든 플레이어 상태
    List<TileState> tiles;         // 모든 타일 상태
    int currentPlayerIndex;        // 현재 턴
    int turnCount;                 // 턴 번호
    DiceState diceState;           // 주사위 상태
    EventState eventState;         // 진행 중인 이벤트
}
```

**동기화 흐름**:
1. 호스트의 GameUI가 게임 상태 변경
2. GameStateMapper가 스냅샷 생성
3. 서버가 모든 클라이언트에게 브로드캐스트
4. 클라이언트가 스냅샷을 수신하여 UI 업데이트

### 5.4 채팅 시스템

**구현 방식**:
- ChatPanel이 메시지 입력 UI 제공
- 네트워크 콜백을 통해 서버로 전송
- 서버가 모든 클라이언트에게 브로드캐스트
- 각 클라이언트의 ChatPanel이 메시지 표시

**메시지 형식**:
```java
payload = {
    "playerIndex": 0,           // 보낸 플레이어 인덱스
    "playerName": "Player A",   // 플레이어 이름
    "content": "안녕하세요!"    // 메시지 내용
}
```

**기능**:
- 텍스트 메시지 (다이얼로그 입력)
- 빠른 이모지 반응 (👍 😊 😂 👏 🎉)
- 플레이어별 색상 구분
- 타임스탬프 표시
- 자동 스크롤

---

## 6. UI 시스템

### 6.1 반응형 레이아웃

- JLayeredPane을 사용한 레이어 구조
- 윈도우 리사이즈 시 자동 스케일링
- scaleFactor를 통한 모든 컴포넌트 비례 조정

```
Layer 구조:
DEFAULT_LAYER  → BoardPanel (보드 렌더링)
PALETTE_LAYER  → OverlayPanel (UI 오버레이)
```

### 6.2 다크 테마

```java
// 기본 색상 팔레트
BACKGROUND_DARK = RGB(32, 33, 36)
PANEL_DARK = RGB(44, 47, 51)
TEXT_PRIMARY = RGB(232, 234, 237)
ACCENT_COLOR = RGB(138, 180, 248)
```

### 6.3 애니메이션

**플레이어 이동**:
- 점프 경로 계산 (BoardPanel)
- 타이머 기반 프레임 업데이트
- START 통과 시 월급 애니메이션

**주사위 굴림**:
- 랜덤 값 빠르게 변경
- 최종 값으로 수렴
- 더블 시 특수 효과

**자산 변동**:
- 금액 증감 시 +/- 텍스트 애니메이션
- 페이드 아웃 효과

---

## 7. 빌드 & 실행

### 7.1 요구사항

- JDK 17 이상
- 추가 의존성 없음 (순수 Java/Swing)

### 7.2 컴파일 및 실행

```bash
# 컴파일
javac -d out -encoding UTF-8 $(find src -name "*.java")

# 실행
java -cp out com.marblegame.Main

# 또는 스크립트 사용
./run.sh
```

### 7.3 게임 모드 선택

1. **혼자하기 (로컬)**: 같은 컴퓨터에서 2-4명이 교대로 플레이
2. **온라인 대전**: 네트워크를 통한 멀티플레이
   - 방 만들기: 서버 호스팅
   - 참가하기: 기존 서버에 접속

---

## 8. 확장 가이드

### 8.1 새 타일 타입 추가

1. `model/` 폴더에 새 타일 클래스 생성
2. `Board.java`의 `initializeBoard()`에 타일 배치
3. `GameUI.java`의 `handleTileLanding()`에 로직 추가
4. 필요시 `RuleEngine`에 규칙 메서드 추가

### 8.2 새 네트워크 메시지 추가

1. `MessageType.java`에 새 타입 추가
2. `GameUI.java`에서 메시지 전송 로직 작성
3. `Main.java`의 `handleMessage()`에 수신 처리 추가
4. 서버 브로드캐스트가 필요하면 `handleServerSideAction()` 수정

### 8.3 새 UI 컴포넌트 추가

1. `ui/` 폴더에 새 패널/다이얼로그 생성
2. `OverlayPanel` 또는 `GameFrame`에 통합
3. 필요시 `repositionComponents()`에서 위치 지정
4. scaleFactor를 고려한 반응형 레이아웃 구현

---

## 9. 턴 진행 플로우차트

```
[게임 시작]
     ↓
[턴 시작] ←──────────────────────┐
     ↓                           │
[파산자 체크] → [파산] → [스킵] ─┘
     ↓
[무인도 체크] → [구금 중] → [탈출 선택]
     ↓                           ↓
[주사위 굴리기]              [보석금/더블]
     ↓                           ↓
[게이지 입력]              [성공/실패]
     ↓                           ↓
[합계 계산 + 더블 억제]    [이동/대기]
     ↓
[이동 애니메이션]
     ↓
[START 통과] → [월급 지급]
     ↓
[타일 도착]
     ↓
[타일 종류에 따른 처리]
├── CITY → [구매/업그레이드/인수/통행료]
├── TOURIST → [구매/잠금/Extra Chance]
├── CHANCE → [랜덤 이벤트]
├── TAX → [세금 징수]
├── OLYMPIC → [도시 선택]
└── 기타
     ↓
[Extra Chance 확인]
     ↓
[더블 확인] → [더블] → [추가 턴] ─┐
     ↓                              │
[턴 종료]                           │
     ↓                              │
[승리 조건 체크]                    │
     ↓                              │
[다음 플레이어] ────────────────────┘
```

---

## 10. 디버깅 & 로깅

- 모든 네트워크 메시지는 콘솔에 로그됨
- 주사위 억제 시 원래값→변경값 형태로 기록
- 상태 변경 시 `System.out.println()` 출력
- 채팅 메시지는 타임스탬프와 함께 UI에 표시

---

## 11. 알려진 제한사항

- 게임 중 연결 끊김 시 자동 재연결 미지원
- 관전 모드 미구현
- 게임 저장/불러오기 미구현
- 최대 4명까지만 지원
- 동일 네트워크(LAN) 내에서만 멀티플레이 가능

---

이 문서는 프로젝트의 전체적인 구조와 핵심 로직을 설명합니다.
코드를 수정하기 전에 관련 섹션을 참고하여 시스템의 동작 방식을 이해하세요.
