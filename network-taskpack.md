# Network Taskpack

네트워크 계층(`src/com/marblegame/network`)을 점검한 뒤, 즉시 손봐야 하는 영역과 추후 확장 과제를 아래와 같이 정리했다.

## Immediate Fixes (Must do)

### T1. 메시지 역직렬화 예외로 전체 연결이 끊어지는 문제 *(완료)*
- **Problem**: `NetworkMessage.deserialize`가 `IllegalArgumentException`을 던지면 서버/클라이언트의 리더 스레드가 예외를 전파하면서 바로 종료되고, `finally` 블록에서 `disconnect()`만 호출되어 원인을 파악할 수 없다. 악의적인 패킷이나 버전 불일치만으로도 세션이 끊긴다.
- **Required Fix**
  1. `HostNetworkService.ClientHandler.readLoop`와 `ClientNetworkService.startReader`에서 `NetworkMessage.deserialize` 호출을 `try/catch`로 감싸고, 잘못된 패킷은 로그로 남긴 뒤 건너뛴다. (필요 시 동일 클라이언트에서 N회 이상 발생 시에만 연결 종료)
  2. 예외 메시지에 `clientId` / 원시 페이로드를 포함해 진단이 가능하도록 한다.
- **Acceptance**: 잘못된 메시지가 들어와도 연결이 즉시 끊기지 않고, 로그에 원인이 남는다.
- **Status**: ✅ `HostNetworkService.readLoop`와 `ClientNetworkService.startReader` 모두 역직렬화 예외를 개별 처리하도록 수정됨. (`HostNetworkService.java:144-158`, `ClientNetworkService.java:77-90`)
- **References**: `src/com/marblegame/network/HostNetworkService.java:144-158`, `src/com/marblegame/network/ClientNetworkService.java:77-90`, `src/com/marblegame/network/message/NetworkMessage.java:33-51`

### T2. 클라이언트 리더 스레드와 `disconnect()` 간 경쟁 조건 정리 *(완료)*
- **Problem**: `ClientNetworkService.disconnect()`는 리더 스레드가 여전히 `reader.readLine()`을 호출하는 동안 `reader`/`writer` 필드를 `null`로 만들어 `NullPointerException`을 유발할 수 있고, 스트림 자체도 닫히지 않는다. 또한 연결 종료 이벤트가 상위 UI에 전달되지 않는다.
- **Required Fix**
  1. `startReader()` 안에서 사용할 `BufferedReader`를 로컬 `final` 변수로 캡쳐하고, 루프가 끝났을 때 해당 스트림을 닫는다.
  2. `disconnect()`가 호출되면 `reader`/`writer`를 안전하게 `close()`하고 리더 스레드를 `join()` 혹은 최소한 `interrupt` 후 종료 신호를 기다린다.
  3. 연결 종료 콜백(Hook)을 `ClientNetworkService`에 추가해 `RemoteGameUI`나 대기 화면이 즉시 알림을 받을 수 있게 한다.
- **Acceptance**: 반복 접속/종료를 수행해도 `NullPointerException`이 발생하지 않고, UI가 즉시 연결 끊김을 인지한다.
- **Status**: ✅ `ClientNetworkService`가 스트림/소켓을 동기화된 블록에서 닫고, 리더 스레드를 join하며 `disconnectListener`를 통해 UI에 알림. `RemoteGameUI`는 listener를 등록하여 경고 다이얼로그/로그를 제공. (`ClientNetworkService.java:35-151`, `RemoteGameUI.java:31-285`)
- **References**: `src/com/marblegame/network/ClientNetworkService.java:35-151`, `src/com/marblegame/core/RemoteGameUI.java:31-285`

### T3. 전송 실패 감지 및 좀비 클라이언트 정리 *(완료)*
- **Problem**: `HostNetworkService.ClientHandler.send`와 `ClientNetworkService.send`는 `PrintWriter.println` 후 오류 여부를 확인하지 않는다. `PrintWriter`는 예외를 삼키므로 TCP 연결이 끊긴 이후에도 서버는 해당 클라이언트를 `clientHandlers`에 유지하고 매 스냅샷마다 쓰기를 시도한다.
- **Required Fix**
  1. 전송 직후 `writer.checkError()`를 확인하고, true일 경우 해당 클라이언트를 제거/`disconnect()` 한다.
  2. 반대 방향(`ClientNetworkService.send`)에서도 실패 시 호출자에게 예외/상태를 알려 UI가 재연결 등을 시도할 수 있게 한다.
- **Acceptance**: 네트워크 케이블을 뽑는 등 비정상 종료 상황에서도 죽은 소켓이 즉시 제거되고, 스냅샷 브로드캐스트 루프에 남지 않는다.
- **Status**: ✅ `ClientNetworkService.send`가 boolean을 반환하며 오류 시 로그 후 `disconnect()` 호출. RemoteGameUI/ClientGameSession은 반환값을 확인해 경고/정리. 서버 측 `ClientHandler.send`는 `checkError()` 발생 시 즉시 핸들러 제거. (`ClientNetworkService.java:94-120`, `RemoteGameUI.java:238-266`, `ClientGameSession.java:38-48`, `HostNetworkService.java:165-170`)
- **References**: `src/com/marblegame/network/HostNetworkService.java:165-170`, `src/com/marblegame/network/ClientNetworkService.java:94-120`, `src/com/marblegame/core/RemoteGameUI.java:238-266`, `src/com/marblegame/session/ClientGameSession.java:38-48`

## Future Enhancements (Nice to have)

### F1. Heartbeat & Timeout *(완료)*
- **Problem**: 리더 루프가 무기한 블록되어 좀비 연결을 감지할 방법이 없고, 호스트/클라이언트 모두 상대가 죽었는지 판단할 수 없었다.
- **Resolution**: 호스트는 주기적으로 `HEARTBEAT` 메시지를 브로드캐스트하고 각 클라이언트 핸들러에 `Socket#setSoTimeout`과 마지막 수신 시각을 두어 응답이 없으면 종료한다. 클라이언트는 동일하게 서버 메시지 수신 시각을 추적하며, 자체 스케줄러로 주기적인 heartbeat를 송신한다. 두 측 모두 `MessageType.HEARTBEAT`를 무시하고, 타임아웃(15s) 이상 응답이 없으면 연결을 닫는다.
- **References**: `src/com/marblegame/network/HostNetworkService.java:1-180`, `src/com/marblegame/network/ClientNetworkService.java:1-170`, `src/com/marblegame/network/message/MessageType.java:1-6`, `src/com/marblegame/core/RemoteGameUI.java:43-67`

### F2. 스냅샷 직렬화 포맷 개선 *(완료)*
- **Problem**: `GameSnapshotSerializer`는 Java 기본 직렬화 + Base64에 의존해 무겁고, 클래스 호환성/보안 문제가 있었다.
- **Resolution**: 스냅샷을 명시적인 JSON 포맷으로 직렬화/역직렬화하도록 재작성했다. 커스텀 JSON 빌더와 경량 파서를 포함해 모든 필드를 명시적으로 제어하며, 문자열 이스케이프 및 null 값을 안전하게 처리한다.
- **References**: `src/com/marblegame/network/snapshot/GameSnapshotSerializer.java:1-330`

### F3. 프로토콜 버전/핸드셰이크 및 역압 *(완료, 역압 TBD)*
- **Problem**: 이전에는 버전 협상 없이 곧바로 메시지를 주고받아 호스트/클라이언트 빌드가 다르면 즉시 예외가 발생하고, 서버는 원인을 모른 채 연결을 닫았다.
- **Resolution**: `HELLO/WELCOME/REJECT` 메시지 타입을 추가하고, 클라이언트는 연결 직후 HELLO(프로토콜 버전)를 전송한 뒤 WELCOME을 받을 때까지 대기한다. 서버는 첫 메시지로 HELLO를 강제하고, 버전이 다르면 REJECT 이유를 전달한 뒤 연결을 종료한다. 스냅샷/하트비트 브로드캐스트는 핸드셰이크가 완료된 클라이언트에게만 전송된다. (역압은 추후 별도 과제로 남김)
- **References**: `src/com/marblegame/network/message/MessageType.java:1-10`, `src/com/marblegame/network/ClientNetworkService.java:1-320`, `src/com/marblegame/network/HostNetworkService.java:1-240`

## Next Tasks (Pending)

### N1. 호스트 다이얼로그 클라이언트 동기화
- **Objective**: 호스트에서 뜨는 게임 다이얼로그(주사위, 보물, 선택지 등)를 클라이언트에서도 동일하게 표시
- **Scope**: 호스트의 다이얼로그 표시 시점에 클라이언트로 메시지 전송하여 동일 다이얼로그 렌더링

### N2. 턴 대기 상태 메시지
- **Objective**: 다른 플레이어의 턴일 때 대기 중인 플레이어에게 "다른 플레이어가 플레이중..." 메시지 표시
- **Scope**: 다이얼로그는 표시하지 않고, UI 오버레이나 라벨로 대기 상태 알림

### N3. 로비 UI 개선
- **Objective**: 게임 시작 전 대기 로비 화면 개선
- **Scope**: 플레이어 목록, 연결 상태, 준비 버튼 등 UX 향상

### N4. 네트워크 모듈 최적화 및 리스크 제거
- **Objective**: 기존 네트워크 계층의 잠재적 버그 제거 및 성능 최적화
- **Scope**: 중복 코드 정리, 스레드 안전성 검토, 메모리 누수 방지, 예외 처리 보강
