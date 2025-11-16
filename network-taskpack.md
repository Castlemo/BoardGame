# LAN 네트워크 멀티플레이 구현 태스크팩

> 모두의 마블 2.0에 LAN 통신 기반 멀티플레이 기능 추가
> 방 생성(호스트) 및 참가(클라이언트) 방식의 턴제 게임 네트워크 동기화

---

## 1. 네트워크 아키텍처 설계

### 1.1 설계 결정
- [x] 네트워크 모델 선택: Server-Client 방식 (호스트 = 서버 + 플레이어)
- [x] 통신 프로토콜: TCP 소켓 기반 (신뢰성)
- [x] 데이터 직렬화: JSON 형식 (가독성, 디버깅 용이)
- [x] 포트 설정: 기본 포트 9999 (설정 가능)

### 1.2 통신 프로토콜 정의
- [x] 메시지 타입 정의 (enum)
  - 연결: `CONNECT`, `DISCONNECT`, `PLAYER_JOIN`, `PLAYER_LEAVE`
  - 게임 시작: `GAME_START`, `GAME_READY`
  - 턴 액션: `ROLL_DICE`, `BUY_CITY`, `UPGRADE`, `TAKEOVER`, `PASS`
  - 상태 동기화: `GAME_STATE_UPDATE`, `PLAYER_STATE_UPDATE`, `BOARD_STATE_UPDATE`
  - 이벤트: `TILE_LANDED`, `CHANCE_EVENT`, `PHASE_DELETE`
- [x] 메시지 포맷 설계 (JSON 스키마)
```json
{
  "type": "MESSAGE_TYPE",
  "playerId": "player-uuid",
  "timestamp": 1234567890,
  "data": { ... }
}
```

---

## 2. 네트워크 패키지 구조 설계

### 2.1 디렉터리 구조
```
src/com/marblegame/
└── network/
    ├── NetConstants.java        # 상수 (포트, 타임아웃 등)
    ├── protocol/
    │   ├── Message.java         # 메시지 기본 클래스
    │   ├── MessageType.java     # 메시지 타입 enum
    │   └── MessageSerializer.java # JSON 직렬화
    ├── server/
    │   ├── GameServer.java      # 서버 메인
    │   ├── ClientHandler.java   # 클라이언트 핸들러
    │   └── RoomManager.java     # 방 관리
    ├── client/
    │   ├── GameClient.java      # 클라이언트 메인
    │   └── ServerListener.java  # 서버 메시지 수신
    ├── sync/                         # ✅ 새로 추가됨
    │   ├── GameStateSnapshot.java   # 게임 상태 스냅샷
    │   └── GameStateMapper.java     # 상태 변환 유틸리티
    └── ui/
        ├── NetworkMenuDialog.java    # 네트워크 메뉴
        ├── CreateRoomDialog.java     # 방 생성
        ├── JoinRoomDialog.java       # 방 참가
        └── LobbyPanel.java           # 대기실
```

---

## 3. 서버 구현 (호스트)

### 3.1 GameServer 구현
- [x] ServerSocket 생성 및 클라이언트 연결 대기
- [x] 멀티스레드 클라이언트 처리 (각 클라이언트당 1 스레드)
- [x] 최대 플레이어 수 제한 (2-4명)
- [x] 서버 종료 시 리소스 정리

### 3.2 ClientHandler 구현
- [x] 클라이언트별 소켓 통신 관리
- [x] 메시지 수신 및 파싱
- [x] 메시지 타입별 핸들러 분기
- [x] 연결 끊김 처리

### 3.3 RoomManager 구현
- [x] 플레이어 목록 관리 (호스트 + 참가자)
- [x] 플레이어 추가/제거
- [x] 게임 시작 조건 확인 (최소 2명)
- [x] 모든 클라이언트에게 브로드캐스트

---

## 4. 클라이언트 구현

### 4.1 GameClient 구현
- [x] 서버 IP/포트로 소켓 연결
- [x] 연결 성공/실패 처리
- [x] 메시지 송신 메서드
- [x] 연결 종료 처리

### 4.2 ServerListener 구현
- [x] 서버 메시지 수신 (별도 스레드)
- [x] 메시지 파싱 및 이벤트 전달
- [x] 연결 끊김 감지
- [x] UI 업데이트 (SwingUtilities.invokeLater 사용)

---

## 5. 프로토콜 구현

### 5.1 Message 클래스
- [x] 메시지 필드: type, playerId, timestamp, data
- [x] Getter/Setter
- [x] toString() 구현 (디버깅용)

### 5.2 MessageSerializer
- [x] JSON 직렬화 (Message → String)
- [x] JSON 역직렬화 (String → Message)
- [x] 에러 처리 (잘못된 JSON)
- [x] List 직렬화/역직렬화 지원 (✅ 새로 추가됨)
- [x] 중첩 객체 지원 (Map, List 재귀 처리)

### 5.3 메시지 타입별 데이터 구조
- [x] `PLAYER_JOIN`: { name, color }
- [x] `GAME_STATE_UPDATE`: { currentPlayer, turnNumber, board, players }
- [x] `ROLL_DICE`: { diceSum, dice1, dice2, isDouble }
- [x] `BUY_CITY`: { tileIndex }
- [x] 기타 필요한 메시지 데이터 구조 정의
- [x] 이벤트 메시지 타입 추가: `ISLAND_EVENT`, `WORLD_TOUR_EVENT`, `TOLL_EVENT`, `TAX_EVENT`, `MAGNETIC_EVENT`

### 5.4 상태 동기화 패키지 (✅ 새로 구현됨)
- [x] GameStateSnapshot 클래스 (게임 전체 상태 캡처)
  - DiceState, PlayerState, CityState, TouristSpotState, EventState
- [x] GameStateMapper 클래스 (상태 변환 유틸리티)
  - capture(): 모델 → 스냅샷
  - apply(): 스냅샷 → 모델
  - toMap()/fromMap(): 직렬화용 변환

---

## 6. UI 구현

### 6.1 NetworkMenuDialog
- [x] 메인 메뉴에서 호출 가능한 다이얼로그
- [x] 버튼: "방 만들기", "방 참가하기", "취소"
- [x] 다크 테마 스타일 적용

### 6.2 CreateRoomDialog
- [x] 플레이어 이름 입력
- [x] 포트 번호 설정 (기본 9999)
- [x] 최대 플레이어 수 선택 (2-4명)
- [x] "생성" 버튼: GameServer 시작 + LobbyPanel 이동
- [x] 로컬 IP 주소 표시 (참가자에게 공유용)

### 6.3 JoinRoomDialog
- [x] 플레이어 이름 입력
- [x] 서버 IP 주소 입력
- [x] 포트 번호 입력 (기본 9999)
- [x] "참가" 버튼: GameClient 연결 시도
- [x] 연결 실패 시 에러 다이얼로그

### 6.4 LobbyPanel
- [x] 현재 참가자 목록 표시
- [x] 호스트만 "게임 시작" 버튼 활성화
- [x] 플레이어 상태 실시간 업데이트
- [ ] 채팅 기능 (선택 사항)
- [x] "나가기" 버튼: 연결 종료 후 메인 메뉴

---

## 7. 게임 로직 네트워크 통합

### 7.1 GameUI 수정
- [x] 네트워크 모드 플래그 추가 (`isNetworkMode`, `isHost`)
- [x] 로컬 플레이어와 원격 플레이어 구분
- [x] 원격 플레이어 턴일 때 UI 비활성화
- [x] 서버로부터 상태 업데이트 수신 시 UI 반영

### 7.2 턴 동기화
- [x] 호스트: 턴 진행 후 모든 클라이언트에 상태 브로드캐스트
- [x] 클라이언트: 자신의 턴에만 액션 전송, 타인 턴은 대기
- [x] 주사위 굴리기 결과 동기화
- [x] 타일 도착 이벤트 동기화

### 7.3 액션 처리
- [x] 클라이언트 액션 → 서버 전송 → 서버 검증 → 브로드캐스트
- [x] 구매/업그레이드/인수/패스 액션 동기화
- [x] 찬스 카드 결과 동기화
- [x] Phase Delete 동기화

### 7.4 상태 동기화
- [x] 플레이어 상태: 위치, 현금, 소유 도시, 무인도 턴
- [x] 보드 상태: 도시 레벨, 소유자, 삭제된 타일
- [x] 턴 상태: 현재 플레이어, 턴 번호, 더블 여부
- [x] 동기화 충돌 방지 (서버를 신뢰할 수 있는 소스로 지정)

---

## 8. 에러 처리 및 안정성

### 8.1 연결 관리
- [ ] 연결 타임아웃 설정 (하트비트 도입 후 재조정 필요)
- [ ] 하트비트 메커니즘 (주기적 핑-퐁)
- [ ] 재연결 시도 옵션
- [x] 클라이언트 갑작스런 종료 처리

### 8.2 게임 중 연결 끊김
- [x] 플레이어 연결 끊김 감지
- [ ] 호스트: 해당 플레이어 "파산" 처리 또는 일시 정지
- [x] 클라이언트: 서버 연결 끊김 시 메인 메뉴로 복귀
- [x] 모든 플레이어에게 알림

### 8.3 예외 처리
- [x] 소켓 예외 (IOException, SocketException)
- [x] JSON 파싱 예외
- [x] 잘못된 메시지 타입 처리
- [ ] 로그 시스템 추가 (네트워크 디버깅용)

---

## 9. 테스트

### 9.1 단위 테스트
- [ ] MessageSerializer 직렬화/역직렬화 테스트
- [ ] 메시지 타입별 데이터 검증
- [ ] RoomManager 플레이어 관리 테스트

### 9.2 통합 테스트
- [ ] 로컬 서버-클라이언트 연결 테스트
- [ ] 2명, 3명, 4명 동시 접속 테스트
- [ ] 게임 전체 플로우 테스트 (시작 ~ 종료)
- [ ] 턴 동기화 정확성 검증

### 9.3 네트워크 테스트
- [ ] LAN 환경 실제 테스트 (다른 컴퓨터)
- [ ] 연결 끊김 시나리오 테스트
- [ ] 지연 시뮬레이션 테스트
- [ ] 동시 액션 충돌 테스트

---

## 10. 최적화 및 개선

### 10.1 성능 최적화
- [ ] 불필요한 상태 동기화 최소화 (델타만 전송)
- [ ] 대역폭 모니터링
- [ ] 메시지 큐잉 및 배치 처리

### 10.2 UX 개선
- [ ] 네트워크 지연 표시 (핑)
- [ ] 로딩 인디케이터
- [ ] 에러 메시지 사용자 친화적 개선
- [ ] 게임 중 플레이어 상태 실시간 표시

### 10.3 추가 기능 (선택)
- [ ] 관전자 모드
- [ ] 게임 일시 정지/재개
- [ ] 저장/불러오기 (네트워크 게임)
- [ ] 리플레이 기능

---

## 구현 우선순위

### Phase 1: 기본 인프라 (필수) ✅ 완료
1. ✅ 네트워크 패키지 구조 생성
2. ✅ 프로토콜 및 메시지 클래스 구현
3. ✅ GameServer 기본 구현 (연결 수락)
4. ✅ GameClient 기본 구현 (연결 시도)

### Phase 2: UI 및 로비 (필수) ✅ 완료
5. ✅ NetworkMenuDialog, CreateRoomDialog, JoinRoomDialog
6. ✅ LobbyPanel (플레이어 목록, 게임 시작)
7. ✅ 호스트/클라이언트 연결 흐름 완성

### Phase 3: 게임 동기화 (핵심) ✅ 완료
8. ✅ 기본 턴 동기화 (주사위, 이동)
9. ✅ 액션 동기화 (구매, 업그레이드, 패스)
10. ✅ 전체 게임 상태 동기화
11. ✅ 이벤트 동기화 (찬스, 무인도, 월드투어, 통행료 등)

### Phase 4: 안정화 (필수)
11. 에러 처리 및 연결 끊김 대응
12. 테스트 및 버그 수정
13. 로그 시스템 추가

### Phase 5: 개선 (선택)
14. 성능 최적화
15. UX 개선
16. 추가 기능

---

## 기술 스택

- **네트워크**: Java Socket API (ServerSocket, Socket)
- **직렬화**: 커스텀 JSON 구현 (외부 라이브러리 미사용) ✅ 구현 완료
- **멀티스레딩**: Java Thread, ExecutorService
- **UI**: Java Swing (기존 코드 스타일 유지)
- **상태 관리**: GameStateSnapshot/Mapper 패턴 ✅ 새로 추가

---

## 주의사항

1. **동기화 충돌**: 서버(호스트)를 신뢰할 수 있는 단일 소스로 지정
2. **스레드 안전성**: UI 업데이트는 반드시 EDT에서 실행 (SwingUtilities.invokeLater)
3. **타임아웃**: 무한 대기 방지를 위해 소켓 타임아웃 설정
4. **포트 충돌**: 포트 사용 중일 경우 다른 포트 선택 옵션 제공
5. **방화벽**: 사용자에게 방화벽 설정 안내 필요
6. **IP 주소**: 호스트는 로컬 IP (192.168.x.x) 표시, 참가자에게 공유

---

## 예상 파일 수정/추가

### 추가 ✅
- ✅ `src/com/marblegame/network/**` (전체 패키지)
- ✅ `src/com/marblegame/network/sync/**` (상태 동기화 패키지)
- ✅ `src/com/marblegame/ui/GameModeDialog.java` (게임 모드 선택)
- ~~`NetworkMain.java`: 네트워크 멀티플레이 진입점~~ → Main.java에 통합됨

### 수정 ✅
- ✅ `Main.java`: 네트워크 모드 선택 및 통합 (로컬/네트워크 모드)
- ✅ `GameUI.java`: 네트워크 모드 지원, 상태 동기화, 원격 액션 처리
- `GameFrame.java`: 네트워크 메뉴 추가
- `RuleEngine.java`: 서버 측 검증 로직 추가 (필요 시)

---

## 완료 체크리스트

- [x] 방 생성 및 서버 시작 기능
- [x] 방 참가 및 클라이언트 연결 기능
- [x] 대기실에서 플레이어 목록 확인
- [x] 호스트가 게임 시작 가능
- [x] 턴제 게임 동기화 (주사위, 이동, 액션) ✅
- [x] 모든 플레이어가 동일한 게임 상태 확인
- [x] 연결 끊김 시 적절한 처리
- [ ] LAN 환경에서 실제 테스트 완료
- [ ] 에러 처리 및 안정성 검증
- [ ] 사용자 매뉴얼/가이드 작성

## TODO
- [x] 주사위 애니메이션 및 이벤트 알림 다이얼로그를 모든 클라이언트 화면에 동기화하여 표시
- [x] Chance/Phase Delete 등 특수 이벤트를 네트워크 메시지로 전파하고 UI에 노출
- [ ] 대규모 플레이 테스트 및 하트비트/재접속 기능 구현

## 잠재적 문제점 및 개선 필요 사항

### 🟡 중요도: 중간
1. **SOCKET_TIMEOUT = 0 (무한 대기)**
   - 위치: `NetConstants.java:11`
   - 문제: 연결 끊김 감지 불가
   - 해결: Heartbeat 구현 후 적절한 값으로 변경 필요

2. **MessageSerializer 크기 검증 없음**
   - 위치: `MessageSerializer.java`
   - 문제: MAX_MESSAGE_SIZE(1MB) 설정만 있고 실제 검증 없음
   - 해결: 직렬화/역직렬화 시 크기 체크 추가

3. **연결 끊김 시 플레이어 파산 처리 미구현**
   - 위치: `8.2 게임 중 연결 끊김`
   - 문제: 게임 중 플레이어 연결 끊김 시 자동 파산 처리 없음
   - 해결: GameUI에서 연결 끊김 플레이어 자동 처리

### 🟢 중요도: 낮음
4. **채팅 기능 미구현**
   - 위치: `6.4 LobbyPanel`
   - 상태: 선택 사항으로 남겨둠

5. **로그 시스템 미구현**
   - 위치: `8.3 예외 처리`
   - 해결: 향후 디버깅 편의를 위해 로깅 프레임워크 도입 고려

### ✅ 확인 완료
- 모든 import 문 정상
- 모든 Dialog 클래스 존재
- 모든 handleRemote*() 메서드 구현 완료
- 모든 notify*Event() 메서드 구현 완료 (notifyTaxEvent 포함)
- MessageSerializer List 지원 완료
- 컴파일 오류 없음 ✅

### 🔧 수정된 버그
1. **클라이언트 다이얼로그 미표시 문제** (2025-11-16 수정)
   - 위치: `GameUI.java:1954`
   - 원인: `applyNetworkSnapshot()`에서 `handleNetworkEvent()` 호출 누락
   - 증상: 클라이언트 화면에서 이벤트 타일, 매입, 지불 등 모든 오버레이 다이얼로그가 표시되지 않음
   - 해결: `handleNetworkEvent(snapshot.getEventState())` 호출 추가
   - 결과: 클라이언트에서도 호스트와 동일한 이벤트 다이얼로그 표시됨

2. **더블 주사위 다이얼로그 네트워크 전파 누락** (2025-11-16 수정)
   - 위치: `GameUI.java:1678`, `MessageType.java`
   - 원인: `DOUBLE_EVENT` 메시지 타입 및 네트워크 알림 함수 미구현
   - 증상: 호스트에서만 더블 다이얼로그 표시, 클라이언트에서는 표시되지 않음
   - 해결:
     - `MessageType.DOUBLE_EVENT` 추가
     - `notifyDoubleEvent()` 함수 구현
     - `handleRemoteDoubleEvent()` 핸들러 구현
     - `handleNetworkEvent()`에 DOUBLE_EVENT 케이스 추가
   - 결과: 클라이언트에서도 더블 다이얼로그 표시됨

3. **오버레이 다이얼로그 중복 표시 문제** (2025-11-16 수정 - 2차)
   - 위치: `GameUI.java` - 게임 로직 내 다이얼로그 표시 부분
   - 원인: 호스트가 자신의 턴이 아닐 때도 다이얼로그를 무조건 표시
   - 증상:
     - A 플레이어(클라이언트) 턴의 더블/무인도 이벤트가 호스트 화면에도 표시
     - 호스트에서 확인 후 클라이언트에서 다시 표시
   - 해결:
     - `shouldShowLocalDialog()` 헬퍼 함수 추가 (line 306-314)
       - 로컬 게임: 항상 true
       - 호스트: 자신의 턴일 때만 true
       - 클라이언트: 항상 false (handleRemote*Event를 통해서만 표시)
     - 게임 로직 내 다이얼로그에 조건 추가:
       - CHANCE: ChanceDialog (line 682-685)
       - ISLAND: IslandDialog (line 664-667)
       - WORLD_TOUR: WorldTourDialog (line 717-720)
       - TAX: TaxPaymentDialog (line 1418-1425)
       - TOLL (도시): TollPaymentDialog (line 793-804)
       - TOLL (관광지): TollPaymentDialog (line 891-902)
       - DOUBLE: DoubleDialog (line 1696-1699)
       - OLYMPIC: OlympicDialog (line 1555-1558)
       - DoubleSuppressedDialog (line 521)
     - `handleRemote*Event()` 함수들은 `isLocalPlayersTurn()` 체크 유지
   - 유지: 전체 플레이어 공유 이벤트 (PHASE_DELETE, MAGNETIC_EVENT)는 조건 없음
   - 추가:
     - `MessageType.OLYMPIC_EVENT` 추가
     - `notifyOlympicEvent()` 함수 구현 (line 2532-2539)
     - `handleRemoteOlympicEvent()` 핸들러 구현 (line 2453-2460)
   - 결과:
     - 호스트 턴 이벤트 → 호스트만 다이얼로그 표시
     - 클라이언트 턴 이벤트 → 클라이언트만 다이얼로그 표시
     - 페이즈 딜리트 등 → 모든 플레이어에게 표시
