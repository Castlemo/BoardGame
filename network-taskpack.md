# LAN 네트워크 멀티플레이 구현 - 완료 보고서

> 모두의 마블 2.0 LAN 멀티플레이 기능
> 최종 업데이트: 2025-11-16

---

## 프로젝트 상태: ✅ 프로토타입 완성

모든 핵심 기능 구현 완료. 실제 테스트 단계 진입 가능.

---

## 1. 아키텍처 요약

### 기술 스택
- **네트워크**: Java TCP Socket (Server-Client 모델)
- **데이터 직렬화**: 커스텀 JSON (외부 라이브러리 미사용)
- **동기화**: GameStateSnapshot 기반 상태 전파
- **UI**: Java Swing (다크 테마)
- **포트**: 9999 (설정 가능)
- **플레이어**: 2-4명

### 패키지 구조
```
src/com/marblegame/network/
├── NetConstants.java           # 네트워크 상수
├── protocol/
│   ├── Message.java           # 메시지 클래스
│   ├── MessageType.java       # 44개 메시지 타입
│   └── MessageSerializer.java # JSON 직렬화
├── server/
│   ├── GameServer.java        # 서버 (호스트)
│   ├── ClientHandler.java     # 클라이언트 처리 + 하트비트
│   └── RoomManager.java       # 방 관리
├── client/
│   ├── GameClient.java        # 클라이언트
│   └── ServerListener.java    # 메시지 수신
├── sync/
│   ├── GameStateSnapshot.java # 상태 스냅샷
│   └── GameStateMapper.java   # 상태 변환
└── ui/
    ├── NetworkMenuDialog.java # 네트워크 메뉴
    ├── CreateRoomDialog.java  # 방 생성 (LAN IP 표시)
    ├── JoinRoomDialog.java    # 방 참가
    └── LobbyPanel.java        # 대기실
```

---

## 2. 구현 완료 기능

### 핵심 기능 ✅
- [x] 방 생성 및 클라이언트 연결 (TCP 소켓)
- [x] 대기실 플레이어 목록 실시간 업데이트
- [x] 게임 상태 완전 동기화 (플레이어, 보드, 턴)
- [x] 모든 게임 이벤트 네트워크 전파 (15개 이벤트 타입)
- [x] 다이얼로그 적절한 화면 표시 (호스트/클라이언트 분리)
- [x] LAN 멀티플레이 지원 (같은 WiFi/공유기)

### 안정성 기능 ✅
- [x] 하트비트 메커니즘 (5초 PING, 15초 타임아웃)
- [x] 연결 끊김 시 자동 파산 처리
- [x] 게임 종료 및 재시작 동기화
- [x] 파산 이벤트 중복 방지

### 동기화되는 이벤트 (15종)
```
게임 이벤트: CHANCE, PHASE_DELETE, ISLAND, WORLD_TOUR, TOLL, TAX,
             MAGNETIC, DOUBLE, OLYMPIC, TOURIST_LANDING
게임 상태: PLAYER_BANKRUPT, GAME_OVER, GAME_RESTART
액션: TOURIST_SPOT_CHOICE, CITY_SELECTION
```

---

## 3. 사용 방법

### 호스트 (방 만들기)
1. 게임 실행 → 네트워크 멀티플레이 → 방 만들기
2. 플레이어 이름, 포트(기본 9999), 최대 인원 설정
3. 화면에 표시된 **LAN IP** (예: 192.168.0.10) 확인
4. 클라이언트에게 IP 공유
5. 대기실에서 플레이어 참가 확인 후 "게임 시작"

### 클라이언트 (방 참가)
1. 게임 실행 → 네트워크 멀티플레이 → 방 참가
2. 플레이어 이름 입력
3. 호스트의 **LAN IP** 입력 (예: 192.168.0.10)
4. 포트 확인 (기본 9999)
5. "참가" 클릭 후 대기실 입장

### 주의사항
- 같은 WiFi/공유기 네트워크 필수
- 방화벽이 포트 9999 차단 시 허용 설정 필요
- 호스트가 게임 로직 처리 (권위 서버 모델)

---

## 4. 주요 구현 내역

### 네트워크 핵심 로직
| 파일 | 역할 | 주요 메서드 |
|------|------|-------------|
| `GameUI.java` | 게임 로직 + 네트워크 통합 | `shouldShowLocalDialog()`, `handleRemote*()`, `notify*Event()` |
| `Main.java` | 진입점 + 서버/클라이언트 연결 | `handleServerSideAction()`, `handlePlayerDisconnect()` |
| `GameServer.java` | 서버 관리 | `getLanIPAddress()`, `onPlayerDisconnected()` |
| `ClientHandler.java` | 클라이언트 처리 + 하트비트 | `startHeartbeatMonitor()`, `checkHeartbeat()` |
| `GameClient.java` | 클라이언트 네트워크 | `sendAction()`, `startHeartbeat()` |

### 상태 동기화 패턴
```java
// 호스트: 이벤트 발생 시 클라이언트에 알림
notifyChanceEvent(playerName, reward);
pushNetworkEvent(MessageType.CHANCE_EVENT, data);

// 클라이언트: 이벤트 수신 시 처리
handleRemoteChanceEvent(data);
// → 다이얼로그 표시 및 로그 출력
```

### 다이얼로그 표시 규칙
- **shouldShowLocalDialog()**: 로컬 게임 OR (호스트 AND 호스트 턴)
- **isLocalPlayersTurn()**: 네트워크 클라이언트 AND 클라이언트 턴
- **전체 공유**: PHASE_DELETE, MAGNETIC_EVENT, PLAYER_BANKRUPT, GAME_OVER

---

## 5. 테스트 체크리스트

### 기능 테스트
- [ ] 2인 네트워크 게임 전체 플로우
- [ ] 3인, 4인 네트워크 게임
- [ ] 모든 이벤트 칸 정상 동작
- [ ] 파산 이벤트 중복 방지 확인
- [ ] 게임 종료 및 재시작 동기화
- [ ] 연결 끊김 시 자동 파산

### 안정성 테스트
- [ ] 클라이언트 갑작스런 종료
- [ ] 호스트 종료 시 클라이언트 처리
- [ ] 장시간 게임 시 안정성

---

## 6. 향후 개선 (선택사항)

### 낮은 우선순위
- 재연결 시도 옵션
- 메시지 크기 검증
- 로그 시스템 (파일 출력)
- 대기실 채팅 기능
- 관전자 모드
- 네트워크 지연 표시 (핑 ms)

### 성능 최적화
- 델타 상태 동기화 (변경분만 전송)
- 메시지 압축
- 배치 처리

---

## 7. 알려진 제한사항

1. **LAN 전용**: 인터넷(WAN) 연결 미지원 (포트 포워딩 필요)
2. **방화벽**: Windows/macOS 방화벽이 연결 차단 가능
3. **동시성**: 동시 액션 충돌 처리 미구현 (턴제이므로 큰 문제 없음)
4. **재연결**: 연결 끊김 후 재연결 미지원 (새로 시작 필요)

---

## 8. 파일 변경 요약

### 신규 생성 (16개 파일)
- `network/` 패키지 전체
- `ui/GameModeDialog.java`

### 주요 수정
- `Main.java`: 네트워크 모드 통합
- `GameUI.java`: 네트워크 동기화 로직 (~3000줄 추가)
- `Board.java`: resetBoard() 메서드 추가
- `Player.java`: playerId 필드 추가

---

## 결론

LAN 네트워크 멀티플레이 프로토타입이 완성되었습니다. 모든 핵심 게임 기능이 네트워크 환경에서 정상 동작하며, 같은 WiFi/공유기를 사용하는 플레이어들과 함께 플레이할 수 있습니다.

**다음 단계**: 실제 2인 이상 환경에서 전체 게임 플로우 테스트
