# BoardGame 프로젝트 구조

> Java/Swing 기반 모두의마블 스타일 보드게임. 길게 눌러 조절하는 주사위 게이지, 반응형 UI, 다양한 타일 효과를 제공한다.  
> 자주 바뀌는 핵심만 담아 빠르게 코드베이스를 파악할 수 있도록 구성했다.

---

## 1. 개요 스냅샷

| 항목 | 내용 |
|------|------|
| 언어 / UI | Java 17 · Swing (커스텀 다크 테마) |
| 지원 인원 | 2–4명 (동일 기기) |
| 보드 | 9×9 외곽 32칸 |
| 시작 자금 | 1,500,000원 |
| 월급 | START 통과 시 200,000원 |
| 승리 조건 | 파산 승리 · 라인 독점 · 3색 독점 |
| 진입점 | `src/com/marblegame/Main.java` (UI) / `core/Game.java` (CLI 레거시) |

---

## 2. 디렉터리 구조

```
src/com/marblegame/
├── Main.java                # GameUI 부트스트랩
├── core/
│   ├── GameUI.java          # 주 컨트롤러 + 상태머신
│   ├── RuleEngine.java      # 금전/타일 규칙
│   └── Game.java            # 콘솔 레거시
├── model/
│   ├── Board.java           # 타일 목록
│   ├── Tile / City / TouristSpot / Player
│   ├── Dice.java            # 순수 2D6 도우미
│   └── DiceGauge.java       # 게이지 모델
└── ui/
    ├── GameFrame.java       # 메인 프레임
    ├── BoardPanel.java      # 보드 렌더 + 애니메이션
    ├── OverlayPanel.java    # 플레이어/홀짝/머니 델타
    ├── ActionPanel.java     # 버튼 + 게이지 + 주사위
    ├── DiceAnimationPanel.java / GaugePanel.java
    └── dialogs/*            # 각종 다이얼로그
```

> `cli/BoardRenderer.java`, `ui/InfoPanel.java`, `ui/ControlPanel.java`는 참고용 레거시 코드다.

---

## 3. 런타임 아키텍처

```
[사용자 입력]
   │ (버튼, 주사위 게이지)
   ▼
[GameUI 컨트롤러] ──호출──> [RuleEngine]
   │                              │
   │                      모델 읽기/쓰기
   ▼                              ▼
[UI 패널 & 다이얼로그] <──데이터── [Board / Players / Tiles]
   │
   └─ 애니메이션 (보드 이동, 주사위, 자산 변화)
```

* `GameUI`가 전체 상태를 소유하며 모든 UI 컴포넌트를 지휘한다.  
* `RuleEngine`은 순수 규칙/계산만 담당해 CLI에서도 재사용 가능하다.

---

## 4. 핵심 모듈

### 4.1 모델 계층
- **Board**: 32개 타일 로드, 삭제된 도시를 이동 시 건너뜀.
- **타일 계층**: `City`(레벨 0–4, 올림픽 버프, 삭제 플래그), `TouristSpot`(잠금, 고정 통행료), 타입/색상 enum.
- **Player**: 현금, 위치, 무인도 턴, `hasRailroadTicket`, `hasExtraChance` 등 플래그.
- **DiceGauge**: 2초 주기 게이지 위치, 섹션별 편향 합계를 관리.

### 4.2 RuleEngine
- 구매/업그레이드/인수/통행료/세금/월급/찬스 등 금전 처리.
- 컬러/라인/트리플 독점 및 듀얼 마그네틱 코어 계산.
- 관광지 잠금/해제 로직을 헬퍼로 일원화.
- 보드 참조 외에는 상태가 없어 테스트가 쉽다.

### 4.3 GameUI
- 턴 라이프사이클, 타일 도착, 이동 애니메이션, 다이얼로그, 더블/추가 굴림을 총괄.
- 상태: `WAITING_FOR_ROLL`, `WAITING_FOR_ACTION`, `WAITING_FOR_JAIL_CHOICE`, `WAITING_FOR_DOUBLE_ROLL`, `ANIMATING_MOVEMENT` 등.
- `clearDoubleState`, `logDoubleSuppression`으로 주사위 로직을 일관되게 유지.

### 4.4 UI 컴포넌트
- **BoardPanel**: 보드/플레이어 렌더 + 점프 경로 제공.
- **OverlayPanel**: 플레이어 리스트, 현금, 홀짝 버튼, 자산 변동 애니메이션(START 월급 포함).
- **ActionPanel**: 주사위 버튼/게이지/행동 버튼, `DiceAnimationPanel` 보관.
- **Dialogs**: 레벨 선택, 관광지 구매, 찬스, 통행료, 더블 안내 등.

---

## 5. 턴 진행 흐름

1. **startTurn**: 파산자 스킵, 관광지 잠금 해제, 3턴마다 Phase Delete, 무인도·철도 여부에 따라 상태 결정.
2. **roll**: 게이지 길게 눌러 편향 합계를 얻고 주사위 조합/애니메이션으로 전달.
3. **move**: 점프 애니메이션 실행, 0번 칸 통과 시 월급 + OverlayPanel 애니메이션.
4. **land**: `handleTileLanding()`이 타일 타입별로 다이얼로그/턴 종료 등을 처리.
5. **actions**: 상황에 따라 구매/업그레이드/인수 버튼 활성화, 관광지는 Extra Chance를 부여하기도 함.
6. **endTurn**: Extra Chance → 더블 순으로 처리, 없으면 다음 플레이어로 전환 후 승리 조건 확인.

---

## 6. 주사위 & 확률 시스템

### 6.1 게이지 구간

| 구간 | 색상 | 등장 합계 |
|------|------|-----------|
| S1 (0–25%)  | 노란색 | 2, 3, 4 |
| S2 (25–50%) | 연주황 | 4, 5, 6 |
| S3 (50–75%) | 주황 | 7, 8, 9, 10 |
| S4 (75–100%)| 빨간색 | 9, 10, 11, 12 |

- `SECTION_SUM_POOLS` 배열만 고치면 편향 조정 가능.
- 게이지 미사용 시 `rollNormal()`이 실제 2개의 d6을 굴려 현실적인 분포 유지.

### 6.2 더블 로직

- `SUM_TO_DICE_COMBINATIONS`로 합계→주사위 조합을 단일 테이블에서 관리.
- 더블 억제 확률: 1회차 60%, 2회차 20%, 3회차 0%. `logDoubleSuppression()`이 `A → B` 합계를 자동 로그한다.
- START/무인도/올림픽/세계여행/파산 등은 `clearDoubleState()`로 즉시 더블 상태를 초기화.
- 홀짝 모드가 합계를 먼저 조정하며, 억제 시에도 짝수 모드는 6,6→6,4처럼 짝수 합계를 보장한다.

---

## 7. 특수 타일 & 효과

| 타일 | 효과 |
|------|------|
| START | 월급 지급, 더블 무효화 |
| CITY | 구매, 레벨 1–4 업그레이드, 통행료, 인수(랜드마크 제외) |
| TOURIST | 고정 가격/통행료, 잠금 또는 Extra Chance 선택 |
| CHANCE | `RuleEngine.processChance`로 랜덤 보상 |
| TAX | 현금의 10% 납부 (다이얼로그 확인) |
| ISLAND | 2턴 구금, 보석금 또는 더블 탈출 |
| RAILROAD / WORLD TOUR | 다음 턴 원하는 칸 이동 티켓 |
| OLYMPIC | 소유 도시 1곳 통행료 2배(1회) |
| WELFARE | 현재 효과 없음 |
| 삭제된 도시 | 이동 시 자동 스킵 (Phase Delete) |

**듀얼 마그네틱 코어**: 자신의 랜드마크에 도착하면 삭제되지 않은 양옆 4칸 내 플레이어를 끌어와 통행료 징수.

---

## 8. 빌드 & 실행

```bash
# 프로젝트 루트에서
./run.sh
# 또는
javac -d out $(find src -name "*.java")
java -cp out com.marblegame.Main
```

JDK 외 추가 의존성 없음.

---

### 빠른 참조 다이어그램 (턴 루프)

```
[턴 시작]
   ↓ (무인도/티켓 체크)
[주사위 혹은 타일 선택]
   ↓
[게이지 길게 누르기] → [편향 합계] → [주사위 조합 + 억제]
   ↓
[이동 애니메이션] → [타일 처리/다이얼로그]
   ↓
[Extra Chance?] → [더블?] → [턴 종료/다음 플레이어]
```

새 타일이나 능력을 추가할 때 위 흐름 중 어느 단계에 연결할지 결정하면 된다.
