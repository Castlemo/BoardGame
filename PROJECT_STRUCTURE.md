# BoardGame 프로젝트 - 전체 구조 문서

> 이 문서는 모노폴리 스타일 보드게임 프로젝트의 전체 구조, 코드 흐름, 최근 수정사항을 정리한 참고 문서입니다.
> 새로운 세션에서 컨텍스트를 빠르게 파악하고 토큰을 효율적으로 관리하기 위해 작성되었습니다.

---

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [디렉토리 구조](#2-디렉토리-구조)
3. [패키지 구조](#3-패키지-구조)
4. [게임 플로우 & 상태 관리](#4-게임-플로우--상태-관리)
5. [UI 아키텍처](#5-ui-아키텍처)
6. [최근 추가 기능](#6-최근-추가-기능)
7. [설정 & 상수](#7-설정--상수)
8. [디자인 패턴 & 규칙](#8-디자인-패턴--규칙)
9. [핵심 기술 상세](#9-핵심-기술-상세)
10. [실행 & 빌드](#10-실행--빌드)
11. [향후 개선 아이디어](#11-향후-개선-아이디어)
12. [Git 커밋 히스토리](#12-git-커밋-히스토리)

---

## 1. 프로젝트 개요

### 1.1 기본 정보

**게임 타입:** 모노폴리 스타일 부동산 보드게임
**언어:** Java (Swing GUI)
**테마:** 다크 모드 전문 UI
**플레이어:** 2-4명 지원

### 1.2 게임 스펙

```
보드 크기:     32 타일 (9x9 그리드 배치)
시작 자금:     1,500,000원
급여:          200,000원 (START 통과 시)
타일 종류:     8가지 (도시, 관광지, 무인도, 찬스, 복지, 전국철도, 올림픽, 세계여행, 세금)
도시 레벨:     0-4 (미소유, 주택, 아파트, 빌딩, 랜드마크)
색상 그룹:     8개 (LIME, GREEN, CYAN, BLUE, LIGHT_PURPLE, PURPLE, BROWN, RED)
```

### 1.3 주요 기능

- ✅ 부동산 매입/업그레이드/인수 시스템
- ✅ 프레스 앤 홀드 주사위 게이지 시스템
- ✅ 홀수/짝수 주사위 모드 선택
- ✅ 3가지 승리 조건 (파산, 라인 독점, 트리플 색상 독점)
- ✅ 올림픽 부스트 (2배 통행료)
- ✅ 랜드마크 시스템 (인수 불가)
- ✅ 관광지 타일 (업그레이드 불가, 고정 통행료)
- ✅ 전국철도 티켓 (원하는 칸으로 이동)
- ✅ 반응형 UI (창 크기 조절에 따른 자동 스케일링)

---

## 2. 디렉토리 구조

```
/Users/yeonseongmo/IdeaProjects/BoardGame/
├── .git/                           # Git 저장소
├── .idea/                          # IntelliJ IDEA 설정
├── .claude/                        # Claude AI 설정
├── src/                            # 소스 코드
│   └── com/marblegame/
│       ├── Main.java               # 진입점 (main 메서드)
│       ├── model/                  # 데이터 모델
│       │   ├── Tile.java           # 타일 베이스 클래스
│       │   ├── City.java           # 도시 타일
│       │   ├── TouristSpot.java    # 관광지 타일
│       │   ├── Player.java         # 플레이어 모델
│       │   ├── Board.java          # 보드 구성
│       │   ├── Dice.java           # 주사위 (2D6)
│       │   └── DiceGauge.java      # 게이지 모델
│       ├── core/                   # 게임 로직 & 컨트롤러
│       │   ├── GameUI.java         # UI 게임 컨트롤러 (메인)
│       │   ├── RuleEngine.java     # 게임 규칙 엔진
│       │   └── Game.java           # 콘솔 버전 (레거시)
│       ├── ui/                     # Swing UI 컴포넌트
│       │   ├── GameFrame.java      # 메인 윈도우
│       │   ├── BoardPanel.java     # 보드 렌더링
│       │   ├── OverlayPanel.java   # 중앙 오버레이
│       │   ├── DiceAnimationPanel.java  # 주사위 애니메이션
│       │   ├── GaugePanel.java     # 연료 게이지 시각화
│       │   ├── InfoPanel.java      # 플레이어 정보 (DEPRECATED)
│       │   └── ControlPanel.java   # 게임 로그 (DEPRECATED)
│       └── cli/                    # 콘솔 렌더러 (레거시)
│           └── BoardRenderer.java
├── out/                            # 컴파일된 클래스 파일
├── bin/                            # 추가 바이너리
├── README.md                       # 사용자 문서
├── PROJECT_STRUCTURE.md            # 이 문서
├── BoardGame.iml                   # IntelliJ 모듈 파일
└── run.sh                          # 실행 스크립트
```

---

## 3. 패키지 구조

### 3.1 Model 패키지 (`com.marblegame.model`)

#### 3.1.1 Tile.java - 타일 베이스 클래스

```java
public class Tile {
    // 타일 타입
    public enum Type {
        START,          // 출발
        CITY,           // 도시 (매입/업그레이드 가능)
        ISLAND,         // 무인도 (2턴 쉼)
        CHANCE,         // 찬스 (10만원 획득)
        TOURIST_SPOT,   // 관광지 (매입만 가능, 고정 통행료)
        WELFARE,        // 복지 (아무 일 없음)
        RAILROAD,       // 전국철도 (티켓 획득)
        OLYMPIC,        // 올림픽 (소유 도시 선택 → 2배 통행료)
        WORLD_TOUR,     // 세계여행 (티켓 획득)
        TAX             // 세금 (보유금 10% 납부)
    }

    int id;
    String name;
    Type type;
    String colorGroup;  // 도시/관광지만 사용
}
```

#### 3.1.2 City.java - 도시 타일

```java
public class City extends Tile {
    int price;              // 매입 가격
    int baseToll;           // 기본 통행료
    int level;              // 0-4 (미소유, 주택, 아파트, 빌딩, 랜드마크)
    Player owner;           // 소유주
    boolean hasOlympicBoost; // 올림픽 부스트 여부 (×2)

    // 레벨별 건물 이모지
    String[] BUILDING_EMOJIS = {"", "🏠", "🏢", "🏬", "🏛️"};

    // 업그레이드 비용
    int getUpgradeCost() {
        return level < 3 ? (int)(price * 0.30) : (int)(price * 0.40); // L4는 40%
    }

    // 인수 가격
    int getTakeoverPrice() {
        return (int)(price * (1.0 + level * 0.5)); // base + (level × 50%)
    }

    // 랜드마크(L4)는 인수 불가
    boolean canBeTakenOver() {
        return level < 4;
    }
}
```

**도시 가격 분포:**
```
LIME:         150,000원 (방콕, 베이징)
GREEN:        180,000-200,000원 (타이페이, 두바이, 카이로)
CYAN:         220,000원 (도쿄, 시드니)
BLUE:         240,000-260,000원 (퀘벡, 상파울로)
LIGHT_PURPLE: 280,000-300,000원 (프라하, 베를린)
PURPLE:       320,000-340,000원 (모스크바, 제네바, 로마)
BROWN:        380,000원 (런던, 파리)
RED:          400,000원 (뉴욕, 서울)
```

#### 3.1.3 TouristSpot.java - 관광지 타일

```java
public class TouristSpot extends Tile {
    int price;      // 매입 가격 (200,000원)
    int toll;       // 고정 통행료 (price × 2.0)
    Player owner;   // 소유주

    // 업그레이드 불가, 인수 가능
}
```

**관광지 목록:**
- 독도, 발리, 하와이, 푸켓, 타히티 (각 200,000원)

#### 3.1.4 Player.java - 플레이어 모델

```java
public class Player {
    String name;            // 플레이어 이름 (PlayerA, PlayerB, ...)
    int cash;               // 보유 현금
    int pos;                // 현재 위치 (0-31)
    int jailTurns;          // 무인도 남은 턴 수 (0이면 자유)
    boolean bankrupt;       // 파산 여부
    boolean hasRailroadTicket; // 전국철도 티켓 보유 여부

    void move(int steps) { pos = (pos + steps) % 32; }
    boolean canAfford(int amount) { return cash >= amount; }
    void pay(int amount) { cash -= amount; }
    void earn(int amount) { cash += amount; }
    boolean isInJail() { return jailTurns > 0; }
}
```

#### 3.1.5 Board.java - 보드 구성

```java
public class Board {
    List<Tile> tiles = new ArrayList<>(); // 32개 타일

    // 타일 배치 (반시계 방향)
    // 하단 (우→좌): 0(START), 1-7, 8(무인도) = 9칸
    // 좌측 (하→상): 9-15, 16(올림픽) = 8칸
    // 상단 (좌→우): 17-23, 24(세계여행) = 8칸
    // 우측 (상→하): 25-31 = 7칸 (마지막은 0으로 돌아옴)
}
```

**보드 레이아웃:**
```
올림픽(16)  17  18  19  20  21  22  23  세계여행(24)
    15                                      25
    14                                      26
    13                                      27
    12                                      28
    11                                      29
    10                                      30
     9                                      31
무인도(8)   7   6   5   4   3   2   1   START(0)
```

#### 3.1.6 DiceGauge.java - 게이지 모델

```java
public class DiceGauge {
    double currentPosition; // 0.0 ~ 1.0
    long startTime;
    int currentSection;     // 1, 2, 3

    // 게이지 동작
    void start() { startTime = System.currentTimeMillis(); }
    int stop() {
        currentSection = getCurrentSection();
        return generateResult(); // 2-12 반환
    }

    // 3구간 시스템 (60% 확률로 구간 범위 편향)
    // S1 (0-33%):   2-5 편향
    // S2 (33-66%):  6-9 편향
    // S3 (66-100%): 10-12 편향
}
```

---

### 3.2 Core 패키지 (`com.marblegame.core`)

#### 3.2.1 GameUI.java - 메인 게임 컨트롤러

**역할:** UI 버전 게임의 핵심 컨트롤러, 모든 게임 로직 조율

**상태 관리:**
```java
private enum GameState {
    WAITING_FOR_ROLL,              // 주사위 굴림 대기
    WAITING_FOR_ACTION,            // 행동 선택 대기
    WAITING_FOR_JAIL_CHOICE,       // 무인도 탈출/대기 선택
    WAITING_FOR_RAILROAD_SELECTION,// 전국철도 목적지 선택
    GAME_OVER                      // 게임 종료
}
```

**주사위 모드:**
```java
private enum DiceMode {
    NORMAL, // 일반 (2-12)
    ODD,    // 홀수만 (3, 5, 7, 9, 11) - 결과값이 홀수
    EVEN    // 짝수만 (2, 4, 6, 8, 10, 12) - 결과값이 짝수
}
```

**주요 메서드:**
```java
// 게임 초기화
public GameUI(int numPlayers, int initialCash)

// 이벤트 설정
private void setupEventHandlers()
private void setupDiceButtonPressAndHold() // 프레스 앤 홀드

// 턴 관리
private void startTurn()
private void endTurn()
private void nextPlayer()

// 주사위 & 이동
private void rollDiceWithGauge()  // 게이지 기반 주사위
private void movePlayer(int steps)

// 타일 이벤트
private void handleTileLanding()  // 타일 착지 처리
private void purchaseCity()       // 도시 매입
private void upgradeCity()        // 도시 업그레이드
private void takeoverCity()       // 도시 인수
private void escapeWithBail()     // 보석금으로 탈출

// 승리 조건
private void checkVictory()
private boolean checkBankruptcyVictory()
private boolean checkLineMonopolyVictory()
private boolean checkTripleColorMonopolyVictory()

// UI 업데이트
private void updateDisplay()
private void log(String message)
private void updateOddEvenButtons()
```

**홀짝 필터 로직 (rollDiceWithGauge):**
```java
// 게이지에서 결과값(2-12) 받기
int result = frame.getActionPanel().getDiceGauge().stop();

// 홀짝 모드에 따라 결과값 조정
if (diceMode == DiceMode.ODD && result % 2 == 0) {
    // 짝수 → 홀수로 변경
    if (result > 2) result -= 1;  // 4→3, 6→5, 8→7, 10→9, 12→11
    else result += 1;              // 2→3
} else if (diceMode == DiceMode.EVEN && result % 2 == 1) {
    // 홀수 → 짝수로 변경
    if (result < 12) result += 1; // 3→4, 5→6, 7→8, 9→10, 11→12
    else result -= 1;
}

// 결과값을 2개 주사위로 분할
```

#### 3.2.2 RuleEngine.java - 게임 규칙 엔진

**역할:** 게임 규칙 계산 및 검증

**상수:**
```java
private static final int SALARY = 200000;          // START 통과 보너스
private static final int BAIL_COST = 200000;       // 보석금
private static final int ISLAND_MAX_TURNS = 2;     // 무인도 최대 턴
private static final double[] tollMultiplierByLevel = {0.0, 1.5, 2.2, 3.0, 4.0};
private static final double COLOR_MONOPOLY_MULTIPLIER = 1.5;
```

**주요 메서드:**
```java
// 통행료 계산
public int calculateToll(City city)  // 레벨 + 독점 보너스 + 올림픽 고려

// 관광지 통행료
public int calculateTouristSpotToll(TouristSpot spot)  // price × 2.0

// 세금 계산
public int calculateTax(Player player)  // cash × 0.1

// 독점 확인
public boolean hasColorMonopoly(Player player, String colorGroup)
public boolean canUpgrade(City city, Player player)
public boolean canTakeover(City city, Player player)

// 파산 처리
public void handleBankruptcy(Player player, Board board)  // 모든 소유물 초기화
```

---

### 3.3 UI 패키지 (`com.marblegame.ui`)

#### 3.3.1 GameFrame.java - 메인 윈도우

**구조:**
```java
public class GameFrame extends JFrame {
    private BoardPanel boardPanel;           // 보드 렌더링
    private OverlayPanel overlayPanel;       // 중앙 오버레이
    private InfoPanel infoPanel;             // DEPRECATED (하위 호환용)
    private ControlPanel controlPanel;       // DEPRECATED (하위 호환용)
}
```

**레이아웃:**
```
JFrame (BorderLayout)
  └── CENTER: JLayeredPane (900×900)
        ├── DEFAULT_LAYER: BoardPanel
        └── PALETTE_LAYER: OverlayPanel
```

**특징:**
- 리사이즈 가능
- macOS Dock 고려 (80px 마진)
- 화면 중앙 배치
- ComponentListener로 리사이즈 시 스케일 동기화

#### 3.3.2 BoardPanel.java - 보드 렌더링

**역할:** 32타일 보드와 플레이어 위치 렌더링

**구조:**
```java
public class BoardPanel extends JPanel {
    private Board board;
    private List<Player> players;
    private double scaleFactor = 1.0;    // 스케일 팩터
    private int translateX = 0;          // X 오프셋
    private int translateY = 0;          // Y 오프셋
}
```

**렌더링 순서:**
```java
@Override
protected void paintComponent(Graphics g) {
    updateTransform();                  // 스케일 계산
    g2d.translate(translateX, translateY);
    g2d.scale(scaleFactor, scaleFactor);

    drawBoard(g2d);    // 타일 그리기
    drawPlayers(g2d);  // 플레이어 그리기
}
```

**타일 렌더링:**
- 색상 그룹별 배경색
- 소유주 표시 (색상 원 + 이니셜)
- 건물 레벨 이모지 (🏠🏢🏬🏛️)
- 랜드마크 골드 테두리
- 올림픽 부스트 표시 (×2)

**플레이어 렌더링:**
- 막대기 인간 아이콘
- 플레이어별 색상
- 그림자 효과

**플레이어 이동 애니메이션:**
- `GameUI`에서 Timer(16ms) 기반으로 이동 상태를 관리
- 타일당 12개의 보간 스텝과 6프레임 정지 구간으로 “한 칸씩 점프” 느낌 연출
- `sin` 이징을 사용해 수평 이동을 부드럽게, 수직으로는 최대 16px까지 살짝 띄워 입체감 부여
- 이동 중에는 행동 버튼/보드 클릭이 비활성화되며, 도착 후 타일 이벤트를 처리

**스케일 계산:**
```java
private void updateTransform() {
    scaleFactor = Math.min(
        (double)getWidth() / BASE_BOARD_SIZE,
        (double)getHeight() / BASE_BOARD_SIZE
    );

    int scaledWidth = (int)(BASE_BOARD_SIZE * scaleFactor);
    int scaledHeight = (int)(BASE_BOARD_SIZE * scaleFactor);

    translateX = (getWidth() - scaledWidth) / 2;
    translateY = (getHeight() - scaledHeight) / 2;
}
```

#### 3.3.3 OverlayPanel.java - 중앙 오버레이

**역할:** 모든 중앙 UI 컴포넌트를 관리하는 투명 오버레이

**컴포넌트 구조:**
```
OverlayPanel (투명)
  ├── CompactPlayerCard (좌상) - Player 1
  ├── CompactPlayerCard (좌하) - Player 2
  ├── turnLabel (중앙 상단)
  ├── dicePanel (중앙)
  ├── oddEvenPanel (중앙) - 홀수/짝수 버튼
  ├── gaugePanel (중앙)
  └── actionButtonPanel (중앙 하단, 가변 높이)
        ├── taxInfoLabel (세금 텍스트)
        ├── rollDiceButton
        ├── purchasePriceLabel
        ├── purchaseButton
        ├── upgradePriceLabel
        ├── upgradeButton
        ├── takeoverPriceLabel
        ├── takeoverButton
        ├── skipButton
        └── escapeButton
```

**레이아웃 알고리즘:**
```java
private void repositionComponents() {
    // 1. 보드 스케일에 맞춰 내부 영역 계산
    int innerLeft = offsetX + scaledTileSize;
    int innerTop = offsetY + scaledTileSize;
    int innerBottom = offsetY + scaledBoardSize - scaledTileSize;

    // 2. 플레이어 카드 배치 (내부 좌측 상하단)
    playerCards.get(0).setBounds(innerLeft + margin, innerTop + margin, ...);
    playerCards.get(1).setBounds(innerLeft + margin, innerBottom - height - margin, ...);

    // 3. 중앙 컴포넌트 수직 배치 (cx, cy 기준)
    int cx = width / 2;
    int cy = height / 2;
    int buttonPanelBase = (int)(62 * scaleFactor);
    int buttonPanelHeight = Math.max(buttonPanelBase, actionButtonPanel.getPreferredSize().height);

    int totalHeight = TURN_LABEL_HEIGHT + spacing +
                      DICE_PANEL_HEIGHT + spacing +
                      ODDEVEN_PANEL_HEIGHT + spacing +
                      GAUGE_PANEL_HEIGHT + spacing +
                      buttonPanelHeight;
    int currentY = cy - totalHeight / 2;

    turnLabel.setBounds(cx - width/2, currentY, ...);
    currentY += height + spacing;

    dicePanel.setBounds(cx - width/2, currentY, ...);
    currentY += height + spacing;

    oddEvenPanel.setBounds(cx - width/2, currentY, ...);
    currentY += height + spacing;

    gaugePanel.setBounds(cx - width/2, currentY, ...);
    currentY += height + spacing;

    actionButtonPanel.setBounds(cx - width/2, currentY, BUTTON_PANEL_WIDTH, buttonPanelHeight);
}
```

**컴포넌트 크기:**
```java
TURN_LABEL:    140 × 35   (폰트 17px)
DICE_PANEL:    126 × 70
ODDEVEN_PANEL: 140 × 49   (버튼 42×42)
GAUGE_PANEL:   224 × 42
BUTTON_PANEL:  216 × 62
BUTTON_SIZE:   200 × 28   (폰트 11px)
PRICE_LABELS:  버튼 폭과 동일, 폰트 10px (스케일 반영), 세금/매입/업그레이드/인수 정보 노출
※ actionButtonPanel 높이는 가변 (라벨이 노출될 때 자동 확장)
```

**CompactPlayerCard (내부 클래스):**
```java
private class CompactPlayerCard extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        // 둥근 카드 배경
        g2.fillRoundRect(...);

        // 플레이어 색상 테두리
        g2.setColor(playerColor);
        g2.drawRoundRect(...);

        // 정보 텍스트
        g2.drawString(player.name, ...);
        g2.drawString("💰 " + cash, ...);

        if (player.isInJail()) {
            g2.drawString("🏝 " + jailTurns + "턴", ...);
        }
    }
}
```

**원형 토글 버튼 (홀짝 선택):**
```java
private JButton createCircularToggleButton(String text) {
    JButton button = new JButton(text) {
        @Override
        protected void paintComponent(Graphics g) {
            // 원 그리기
            Boolean selected = (Boolean)getClientProperty("selected");
            g2.setColor(selected ? BLUE : GRAY);
            g2.fillOval(...);

            // 테두리
            g2.setColor(WHITE);
            g2.drawOval(...);

            // 텍스트
            g2.drawString(text, ...);
        }
    };
    return button;
}
```

#### 3.3.4 DiceAnimationPanel.java - 주사위 애니메이션

```java
public class DiceAnimationPanel extends JPanel {
    private int dice1Value = 1;
    private int dice2Value = 1;
    private boolean isAnimating = false;

    public void startAnimation(int finalD1, int finalD2, Runnable onComplete) {
        // 1.4초 동안 랜덤 값으로 애니메이션
        // Cubic-out easing
        // 60ms 프레임 간격
        // 완료 시 콜백 호출
    }
}
```

#### 3.3.5 GaugePanel.java - 연료 게이지 시각화

```java
public class GaugePanel extends JPanel {
    private DiceGauge gauge;
    private Timer animationTimer; // 30fps

    @Override
    protected void paintComponent(Graphics g) {
        double position = gauge.getCurrentPosition(); // 0.0 ~ 1.0
        int filledWidth = (int)(position * width);

        // 빈 부분 (회색)
        g.setColor(EMPTY_GAUGE_COLOR);
        g.fillRoundRect(0, 0, width, height, ...);

        // 채워진 부분 (색상 변화)
        Color fillColor;
        if (position < 0.333) fillColor = YELLOW;
        else if (position < 0.666) fillColor = ORANGE;
        else fillColor = RED;

        g.setColor(fillColor);
        g.fillRoundRect(0, 0, filledWidth, height, ...);

        // 외곽 테두리
        g.drawRoundRect(0, 0, width, height, ...);
    }
}
```

---

## 4. 게임 플로우 & 상태 관리

### 4.1 초기화 순서

```
1. Main.main()
   └─> SwingUtilities.invokeLater()
       └─> new GameUI(2, 1500000)
           ├─> new Board()                    // 32타일 생성
           ├─> new RuleEngine(board)
           ├─> new Player[2]                  // 플레이어 생성
           ├─> new Dice()
           ├─> new GameFrame(board, players)  // UI 생성
           │   ├─> new BoardPanel(board, players)
           │   ├─> new OverlayPanel(players)
           │   │   ├─> new DiceAnimationPanel()
           │   │   ├─> new DiceGauge()
           │   │   ├─> new GaugePanel(diceGauge)
           │   │   ├─> createCircularToggleButton("홀수")
           │   │   ├─> createCircularToggleButton("짝수")
           │   │   └─> createStyledButton("🎲 주사위 굴리기", ...)
           │   ├─> new InfoPanel() (deprecated)
           │   └─> new ControlPanel() (deprecated)
           ├─> setupEventHandlers()           // 버튼 리스너 등록
           │   ├─> 주사위 버튼 (press & hold)
           │   ├─> 홀수/짝수 버튼
           │   ├─> 매입/업그레이드/인수/스킵/탈출 버튼
           │   └─> 타일 클릭 리스너
           └─> startTurn()                    // 첫 턴 시작
```

### 4.2 상태 머신 다이어그램

```
┌──────────────────────┐
│  WAITING_FOR_ROLL    │ ◀── startTurn()
└──────┬───────────────┘
       │ 주사위 버튼 누름 (press)
       │ → 게이지 시작
       │ 주사위 버튼 뗌 (release)
       │ → 게이지 정지 & 결과 생성
       │ → 주사위 애니메이션 (1.4초)
       ▼
  movePlayer(result)
       │
       ▼
  handleTileLanding()
       │
       ├─> START: 급여 → endTurn()
       ├─> CITY:
       │   ├─ 미소유 → WAITING_FOR_ACTION (매입 버튼)
       │   ├─ 내 소유 → WAITING_FOR_ACTION (업그레이드 버튼)
       │   └─ 타인 소유 → 통행료 지불 & WAITING_FOR_ACTION (인수 버튼)
       ├─> TOURIST_SPOT:
       │   ├─ 미소유 → WAITING_FOR_ACTION (매입 버튼)
       │   └─ 타인 소유 → 통행료 지불 → endTurn()
       ├─> ISLAND: jailTurns=2 → endTurn()
       ├─> CHANCE: 100k 획득 → endTurn()
       ├─> RAILROAD/WORLD_TOUR: hasRailroadTicket=true → endTurn()
       ├─> TAX: 10% 납부 → endTurn()
       └─> OLYMPIC: WAITING_FOR_ACTION (도시 선택 → 2배 부스트)
       │
       ▼
┌──────────────────────┐
│  WAITING_FOR_ACTION  │
└──────┬───────────────┘
       │ 플레이어 행동 선택
       │ (매입/업그레이드/인수/스킵)
       ▼
    endTurn()
       │
       ▼
  checkVictory()
       │
       ├─ 승리 → GAME_OVER (다이얼로그 → 재시작/종료)
       └─ 계속 → nextPlayer() → startTurn()

┌─────────────────────────┐
│ WAITING_FOR_JAIL_CHOICE │ (무인도 상태)
└──────┬──────────────────┘
       │ 플레이어 선택
       ├─ 탈출 (보석금) → 턴 진행
       └─ 대기 → jailTurns-- → endTurn()

┌──────────────────────────────┐
│ WAITING_FOR_RAILROAD_SELECTION│ (전국철도 티켓)
└──────┬───────────────────────┘
       │ 타일 클릭 활성화
       │ 플레이어가 타일 선택
       ▼
  movePlayer(선택한 타일)
       │
       ▼
  handleTileLanding()
```

### 4.3 턴 진행 순서

```java
startTurn() {
    1. 현재 플레이어 정보 로그 출력
    2. 무인도 확인
       - jailTurns > 0 → WAITING_FOR_JAIL_CHOICE
         - 탈출 버튼 표시
         - 스킵 버튼 표시
       - jailTurns == 0 → 계속
    3. 전국철도 티켓 확인
       - hasRailroadTicket == true → WAITING_FOR_RAILROAD_SELECTION
         - 타일 클릭 활성화
         - 로그: "원하는 칸을 클릭하세요"
       - hasRailroadTicket == false → 계속
    4. 주사위 버튼 활성화
    5. state = WAITING_FOR_ROLL
}

endTurn() {
    1. 버튼 모두 비활성화
    2. 다음 플레이어로 전환 대기
}

nextPlayer() {
    currentPlayerIndex = (currentPlayerIndex + 1) % players.length;

    // 파산한 플레이어 건너뛰기
    while (players[currentPlayerIndex].bankrupt) {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
    }

    turnCount++;
    startTurn();
}
```

### 4.4 타일별 이벤트 처리

```java
handleTileLanding() {
    Tile tile = board.getTile(player.pos);

    switch (tile.type) {
        case START:
            log("출발점 도착!");
            endTurn();
            break;

        case CITY:
            City city = (City) tile;
            if (city.owner == null) {
                // 미소유 도시
                showPurchaseButton();
                showSkipButton();
            } else if (city.owner == player) {
                // 내 도시
                if (city.level < 4) showUpgradeButton();
                showSkipButton();
            } else {
                // 타인 도시
                int toll = ruleEngine.calculateToll(city);
                player.pay(toll);
                city.owner.earn(toll);
                log("통행료 " + toll + "원 지불");

                if (city.canBeTakenOver()) showTakeoverButton();
                showSkipButton();
            }
            state = WAITING_FOR_ACTION;
            break;

        case ISLAND:
            player.jailTurns = 2;
            log("무인도에 갇혔습니다!");
            endTurn();
            break;

        case CHANCE:
            player.earn(100000);
            log("찬스! 100,000원 획득!");
            endTurn();
            break;

        case RAILROAD:
        case WORLD_TOUR:
            player.hasRailroadTicket = true;
            log("전국철도 티켓 획득!");
            endTurn();
            break;

        case TAX:
            int tax = ruleEngine.calculateTax(player);
            player.pay(tax);
            log("세금 " + tax + "원 납부");
            endTurn();
            break;

        case OLYMPIC:
            // 소유한 도시 목록 표시 → 선택 → 2배 부스트 적용
            showOlympicCitySelection();
            state = WAITING_FOR_ACTION;
            break;

        case WELFARE:
            log("복지 칸 - 휴식");
            endTurn();
            break;
    }
}
```

---

## 5. UI 아키텍처

### 5.1 JLayeredPane 구조

```
GameFrame (1200×900)
  └── CENTER: JLayeredPane (900×900)
        │
        ├── DEFAULT_LAYER (Z=0)
        │     └── BoardPanel (0, 0, 900, 900)
        │           - Tile rendering
        │           - Player icons
        │           - Opaque
        │
        └── PALETTE_LAYER (Z=100)
              └── OverlayPanel (0, 0, 900, 900)
                    - Turn label
                    - Dice animation
                    - Odd/Even buttons
                    - Fuel gauge
                    - Action buttons
                    - Player cards
                    - Transparent (opaque=false)
```

### 5.2 오버레이 포지셔닝 시스템

**핵심 개념:**
- 보드는 9×9 그리드 (각 타일 80px)
- 외곽 타일 1줄을 제외한 내부가 "inner board"
- 플레이어 카드는 inner board 내부 좌측 상하단에 배치
- 중앙 컴포넌트들은 화면 중심에 수직 배치

**Inner Board 계산:**
```java
// 보드 스케일 팩터 (BoardPanel과 동일)
float scale = Math.min((float)width / 720, (float)height / 720);
int scaledTileSize = (int)(80 * scale);
int scaledBoardSize = (int)(720 * scale);

// 보드 중앙 정렬 오프셋
int offsetX = (width - scaledBoardSize) / 2;
int offsetY = (height - scaledBoardSize) / 2;

// Inner board 경계
int innerLeft   = offsetX + scaledTileSize;     // 첫 타일 이후
int innerTop    = offsetY + scaledTileSize;
int innerRight  = offsetX + scaledBoardSize - scaledTileSize; // 마지막 타일 전
int innerBottom = offsetY + scaledBoardSize - scaledTileSize;

// Inner board 크기
int innerWidth  = innerRight - innerLeft;   // 7×80 = 560px (scaled)
int innerHeight = innerBottom - innerTop;
```

**플레이어 카드 배치:**
```java
// Player 1: Inner board 좌측 상단
playerCards.get(0).setBounds(
    innerLeft + margin,
    innerTop + margin,
    scaledCardWidth,
    scaledCardHeight
);

// Player 2: Inner board 좌측 하단
playerCards.get(1).setBounds(
    innerLeft + margin,
    innerBottom - scaledCardHeight - margin,
    scaledCardWidth,
    scaledCardHeight
);
```

**중앙 컴포넌트 수직 스택:**
```java
int cx = width / 2;
int cy = height / 2;

// 전체 높이 계산
int totalHeight =
    TURN_LABEL_HEIGHT + spacing +
    DICE_PANEL_HEIGHT + spacing +
    ODDEVEN_PANEL_HEIGHT + spacing +
    GAUGE_PANEL_HEIGHT + spacing +
    BUTTON_PANEL_HEIGHT;

// 시작 Y 좌표 (중앙 정렬)
int startY = cy - totalHeight / 2;
int currentY = startY;

// 각 컴포넌트 배치
turnLabel.setBounds(cx - width/2, currentY, width, height);
currentY += height + spacing;

dicePanel.setBounds(cx - width/2, currentY, width, height);
currentY += height + spacing;

oddEvenPanel.setBounds(cx - width/2, currentY, width, height);
currentY += height + spacing;

gaugePanel.setBounds(cx - width/2, currentY, width, height);
currentY += height + spacing;

actionButtonPanel.setBounds(cx - width/2, currentY, width, height);
```

### 5.3 스케일링 시스템

**목표:** 창 크기가 변경되어도 보드와 오버레이가 동일한 비율로 스케일링

**BoardPanel 스케일링:**
```java
// 1. 창 크기에 맞는 스케일 팩터 계산
private void updateTransform() {
    double availableWidth = getWidth();
    double availableHeight = getHeight();
    double boardPixels = 720.0;  // BASE_BOARD_SIZE

    scaleFactor = Math.min(
        availableWidth / boardPixels,
        availableHeight / boardPixels
    );

    // 중앙 정렬 오프셋 계산
    double scaledSize = boardPixels * scaleFactor;
    translateX = (int)((availableWidth - scaledSize) / 2);
    translateY = (int)((availableHeight - scaledSize) / 2);
}

// 2. paintComponent에서 transform 적용
@Override
protected void paintComponent(Graphics g) {
    updateTransform();
    g2d.translate(translateX, translateY);
    g2d.scale(scaleFactor, scaleFactor);

    // 이제 모든 그리기는 720×720 기준으로 수행
    drawBoard(g2d);
    drawPlayers(g2d);
}

// 3. Getter로 scaleFactor 노출
public double getScaleFactor() {
    return scaleFactor;
}
```

**OverlayPanel 스케일링:**
```java
// 1. BoardPanel의 scaleFactor 받기
private double scaleFactor = 1.0;

public void setScaleFactor(double scaleFactor) {
    this.scaleFactor = scaleFactor;
    updateButtonSizes();    // 버튼 크기 업데이트
    repositionComponents(); // 컴포넌트 재배치
}

// 2. 모든 크기에 scaleFactor 적용
private void repositionComponents() {
    // 컴포넌트 크기 (30% 축소 후 버튼 10% 증가)
    int TURN_LABEL_WIDTH   = (int)(140 * scaleFactor);
    int TURN_LABEL_HEIGHT  = (int)(35 * scaleFactor);
    int DICE_PANEL_WIDTH   = (int)(126 * scaleFactor);
    int DICE_PANEL_HEIGHT  = (int)(70 * scaleFactor);
    int ODDEVEN_PANEL_WIDTH = (int)(140 * scaleFactor);
    int ODDEVEN_PANEL_HEIGHT= (int)(49 * scaleFactor);
    int GAUGE_PANEL_WIDTH  = (int)(224 * scaleFactor);
    int GAUGE_PANEL_HEIGHT = (int)(42 * scaleFactor);
    int BUTTON_PANEL_WIDTH = (int)(216 * scaleFactor);
    int BUTTON_PANEL_HEIGHT= (int)(62 * scaleFactor);

    // 폰트 크기도 스케일
    int turnFontSize = (int)(17 * scaleFactor);
    int buttonFontSize = (int)(11 * scaleFactor);

    // 플레이어 카드 크기
    int cardWidth = (int)(160 * scaleFactor);
    int cardHeight = (int)(70 * scaleFactor);

    // ... 배치 로직
}

// 3. 버튼 크기 업데이트
private void updateButtonSizes() {
    int buttonWidth = (int)(200 * scaleFactor);
    int buttonHeight = (int)(28 * scaleFactor);
    int fontSize = Math.max(9, (int)(11 * scaleFactor));

    for (JButton button : buttons) {
        button.setFont(new Font("Malgun Gothic", Font.BOLD, fontSize));
        button.setPreferredSize(new Dimension(buttonWidth, buttonHeight));
    }
}
```

**GameFrame 연동:**
```java
// LayeredPane 리사이즈 리스너
layeredPane.addComponentListener(new ComponentAdapter() {
    @Override
    public void componentResized(ComponentEvent e) {
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();

        boardPanel.setBounds(0, 0, w, h);
        overlayPanel.setBounds(0, 0, w, h);

        // BoardPanel의 scaleFactor를 OverlayPanel에 전달
        SwingUtilities.invokeLater(() -> {
            overlayPanel.setScaleFactor(boardPanel.getScaleFactor());
        });
    }
});
```

**스케일링 흐름:**
```
창 크기 변경
  ↓
LayeredPane resize 이벤트
  ↓
boardPanel.setBounds(0, 0, w, h)
overlayPanel.setBounds(0, 0, w, h)
  ↓
BoardPanel.paintComponent()
  → updateTransform() → scaleFactor 계산
  ↓
overlayPanel.setScaleFactor(boardPanel.getScaleFactor())
  ↓
OverlayPanel.repositionComponents()
  → 모든 크기 × scaleFactor
  → 모든 컴포넌트 재배치
```

---

## 6. 최근 추가 기능

### 6.1 주사위 게이지 시스템 (DiceGauge + GaugePanel)

**배경:** 단순 주사위 클릭 대신 타이밍 기반 게임플레이 추가

**구현:**
1. **DiceGauge 모델:**
   - 2초 주기로 0.0 → 1.0 → 0.0 왕복 (sinusoidal)
   - 3개 구간: S1(0-33%), S2(33-66%), S3(66-100%)
   - 각 구간은 특정 범위에 60% 편향:
     - S1 → 2-5 편향
     - S2 → 6-9 편향
     - S3 → 10-12 편향

2. **GaugePanel 시각화:**
   - 연료 게이지 스타일 (가로 막대)
   - 3색 변화: 노란색(0-33%) → 주황색(33-67%) → 빨간색(67-100%)
   - 30fps 애니메이션
   - 빈 부분: 어두운 회색

3. **Press & Hold 메커니즘:**
   - mousePressed: 게이지 시작, 로그 출력
   - mouseReleased: 게이지 정지, 결과 생성, 주사위 애니메이션

**코드 위치:**
- `model/DiceGauge.java` - 게이지 모델
- `ui/GaugePanel.java` - 시각화
- `core/GameUI.setupDiceButtonPressAndHold()` - 이벤트 처리

### 6.2 홀수/짝수 주사위 모드

**배경:** 전략적 선택지 추가 (특정 패리티로만 이동)

**UI:**
- 2개 원형 토글 버튼: "홀수", "짝수"
- 위치: 주사위 패널과 게이지 패널 사이
- 선택 시: 파란색, 미선택 시: 회색
- 재클릭 시: 일반 모드로 복귀

**로직 (GameUI.rollDiceWithGauge):**
```java
int result = diceGauge.stop(); // 2-12

// 홀짝 필터 적용
if (diceMode == DiceMode.ODD && result % 2 == 0) {
    // 짝수 결과를 홀수로 변경
    result = (result > 2) ? result - 1 : result + 1;
} else if (diceMode == DiceMode.EVEN && result % 2 == 1) {
    // 홀수 결과를 짝수로 변경
    result = (result < 12) ? result + 1 : result - 1;
}

// 이제 result는 원하는 패리티
// 결과: 홀수 모드 → 3,5,7,9,11 / 짝수 모드 → 2,4,6,8,10,12
```

**코드 위치:**
- `core/GameUI.java` - DiceMode enum, 필터 로직
- `ui/OverlayPanel.createCircularToggleButton()` - 원형 버튼
- `core/GameUI.setupEventHandlers()` - 버튼 리스너

### 6.3 액션 패널 비용 표시 강화

**배경:** 로그 패널 제거 이후 버튼을 누르기 전까지 비용을 확인하기 어려웠음

**UI 변경 사항:**
- `actionButtonPanel` 상단에 4개의 텍스트 라벨 추가
  - `taxInfoLabel`: 국세청 진입 시 즉시 세금 금액 안내
  - `purchasePriceLabel`: 매입 버튼 노출 시 가격 표시
  - `upgradePriceLabel`: 업그레이드 가능 시 단계별 비용 표시
  - `takeoverPriceLabel`: 인수 선택지 제공 시 인수 금액 안내
- 모든 라벨은 버튼과 동일한 폭/스케일을 가지며, 표시 여부에 따라 자동으로 숨김
- 패널 높이를 `preferredSize` 기반으로 재계산하여 패스 버튼이 잘리지 않음

**로직 연동:**
- `GameUI.handleCityTile`, `handleTouristSpotTile`, `handleTaxTile` 등에서 상황별 금액 전달
- 턴 시작/타일 이동 시 `clearPriceLabels()` 호출로 잔여 정보 제거
- 버튼 상태 변화마다 `refreshPriceLabelVisibility()`가 수행되어 레이아웃을 즉시 갱신

### 6.4 플레이어 점프 이동 애니메이션

**배경:** “즉시 이동” 방식에서 한 칸씩 이동하는 연출로 몰입감 향상

**구현:** 
1. `GameUI`에 이동 전용 상태(`ANIMATING_MOVEMENT`)와 Timer(16ms) 추가
2. 칸당 12개의 보간 스텝 + 6프레임 휴지기로 자연스러운 착/이륙 타이밍 구현
3. `sin` 이징을 적용해 수평 이동은 부드럽게, 수직으로는 최대 16px까지 살짝 들어 올려 점프 느낌 연출
4. 이동 중에는 행동 버튼과 타일 선택을 비활성화하고, 각 칸 통과 시 급여/플레이어 카드 정보를 즉시 갱신
5. 최종 도착 후 `handleTileLanding()`을 호출하여 기존 타일 이벤트 흐름 유지

**커스터마이징 포인트:**
- `MOVEMENT_SUB_STEPS`, `MOVEMENT_HOLD_STEPS`, `MOVEMENT_HOP_HEIGHT` 상수로 속도·점프 높이 조절 가능
- Timer 해제/재시작 로직을 분리하여 다른 애니메이션과 충돌하지 않음

### 6.5 플레이어 카드 오버레이 이동

**변경 전:** 플레이어 정보가 좌측 사이드바(InfoPanel, WEST)에 표시
**변경 후:** 플레이어 정보가 보드 내부(inner board) 좌측 상하단에 표시

**동기:**
- 더 깔끔한 레이아웃
- 보드 영역 최대 활용
- 중앙 집중형 UI

**구현:**
1. **CompactPlayerCard 생성 (OverlayPanel 내부 클래스):**
   - 크기: 160×70 (스케일 적용)
   - 정보: 이름, 보유금액, 무인도 턴(조건부)
   - 스타일: 플레이어 색상 테두리, 다크 배경

2. **배치 위치:**
   - Player 1: innerLeft + margin, innerTop + margin
   - Player 2: innerLeft + margin, innerBottom - height - margin

3. **Deprecated:**
   - InfoPanel.java: 더 이상 레이아웃에 추가되지 않음 (하위 호환용으로 생성만)

**코드 위치:**
- `ui/OverlayPanel.CompactPlayerCard` - 플레이어 카드 클래스
- `ui/OverlayPanel.repositionComponents()` - 배치 로직
- `ui/GameFrame.initComponents()` - InfoPanel 제거

### 6.6 연료 게이지 스타일 변경

**변경 전:** 3개 구간 배경(초록/파랑/노랑) + 빨간 선 인디케이터
**변경 후:** 채워지는 막대 + 3색 그라데이션

**이유:** 더 직관적인 시각적 피드백

**구현 (GaugePanel.drawFuelGauge):**
```java
double position = gauge.getCurrentPosition(); // 0.0 ~ 1.0
int filledWidth = (int)(position * width);

// 빈 부분 (회색)
g2.setColor(EMPTY_GAUGE_COLOR);
g2.fillRoundRect(x, y, width, height, corner, corner);

// 채워진 부분 (색상 결정)
Color fillColor;
if (position < 0.333)      fillColor = YELLOW_COLOR;
else if (position < 0.666) fillColor = ORANGE_COLOR;
else                       fillColor = RED_COLOR;

g2.setColor(fillColor);
g2.fillRoundRect(x, y, filledWidth, height, corner, corner);

// 테두리
g2.setColor(BORDER_COLOR);
g2.drawRoundRect(x, y, width, height, corner, corner);
```

### 6.7 랜드마크 시스템 (도시 레벨 4)

**특징:**
- 레벨 4 = 랜드마크 (이모지: 🏛️)
- 업그레이드 비용: 기본가의 40% (L1-3은 30%)
- **인수 불가** (`canBeTakenOver()` returns false)
- 시각적 구분: 골드 테두리

**로직 (City.java):**
```java
public boolean canBeTakenOver() {
    return level < 4; // 랜드마크(L4)는 인수 불가
}

public int getUpgradeCost() {
    if (level < 3) return (int)(price * 0.30);
    else return (int)(price * 0.40); // L3→L4는 40%
}
```

**렌더링 (BoardPanel.drawTile):**
```java
if (city.level == 4) {
    // 골드 테두리
    g2.setColor(new Color(255, 215, 0)); // Gold
    g2.setStroke(new BasicStroke(4f));
    g2.drawRect(x, y, size, size);
}
```

### 6.8 올림픽 부스트 (×2 통행료)

**동작:**
1. 플레이어가 올림픽 타일(16번)에 착지
2. 소유한 도시 목록 표시
3. 플레이어가 도시 선택
4. 해당 도시에 `hasOlympicBoost = true` 설정
5. 다음 통행료 지불 시 2배 적용
6. 지불 후 `hasOlympicBoost = false` (일회용)

**시각화 (BoardPanel):**
```java
if (city.hasOlympicBoost) {
    g2.setColor(Color.RED);
    g2.setFont(new Font("Arial", Font.BOLD, 16));
    g2.drawString("×2", x + size - 25, y + 20);
}
```

**통행료 계산 (RuleEngine):**
```java
public int calculateToll(City city) {
    int baseToll = city.baseToll;
    double multiplier = tollMultiplierByLevel[city.level];
    int toll = (int)(baseToll * multiplier);

    // 색상 독점 보너스
    if (hasColorMonopoly(city.owner, city.colorGroup)) {
        toll = (int)(toll * COLOR_MONOPOLY_MULTIPLIER);
    }

    // 올림픽 부스트
    if (city.hasOlympicBoost) {
        toll *= 2;
    }

    return toll;
}
```

### 6.9 관광지 타일 (TouristSpot)

**특징:**
- 새로운 타일 타입
- 매입 가능, 업그레이드 불가
- 고정 통행료: 기본가의 200%
- 시각적: 핑크 그라데이션 배경

**타일 목록:**
```java
tiles.add(new TouristSpot(11, "독도", 200000));
tiles.add(new TouristSpot(13, "발리", 200000));
tiles.add(new TouristSpot(20, "하와이", 200000));
tiles.add(new TouristSpot(22, "푸켓", 200000));
tiles.add(new TouristSpot(28, "타히티", 200000));
```

**렌더링 (BoardPanel):**
```java
case TOURIST_SPOT:
    // 핑크 그라데이션
    GradientPaint gradient = new GradientPaint(
        x, y, new Color(255, 182, 193),
        x + size, y + size, new Color(255, 105, 180)
    );
    g2.setPaint(gradient);
    g2.fillRect(x, y, size, size);
    break;
```

### 6.10 승리 조건 3가지

**구현 위치:** `core/GameUI.checkVictory()`

#### 1. 파산 승리 (Bankruptcy Victory)
```java
private boolean checkBankruptcyVictory() {
    int alivePlayers = 0;
    for (Player p : players) {
        if (!p.bankrupt) alivePlayers++;
    }
    return alivePlayers == 1;
}
```

#### 2. 라인 독점 승리 (Line Monopoly Victory)
```java
private boolean checkLineMonopolyVictory() {
    // 보드의 4개 라인 (하단, 좌측, 상단, 우측)
    int[][] lines = {
        {1,2,3,4,5,6,7},          // 하단
        {9,10,11,12,13,14,15},    // 좌측
        {17,18,19,20,21,22,23},   // 상단
        {25,26,27,28,29,30,31}    // 우측
    };

    for (int[] line : lines) {
        boolean allOwned = true;
        for (int tileId : line) {
            Tile tile = board.getTile(tileId);
            if (tile instanceof City) {
                City city = (City) tile;
                if (city.owner != player) {
                    allOwned = false;
                    break;
                }
            } else if (tile instanceof TouristSpot) {
                TouristSpot spot = (TouristSpot) tile;
                if (spot.owner != player) {
                    allOwned = false;
                    break;
                }
            } else {
                allOwned = false;
                break;
            }
        }
        if (allOwned) return true;
    }
    return false;
}
```

#### 3. 트리플 색상 독점 승리 (Triple Color Monopoly Victory)
```java
private boolean checkTripleColorMonopolyVictory() {
    Set<String> monopolizedColors = new HashSet<>();

    String[] colorGroups = {"LIME", "GREEN", "CYAN", "BLUE",
                           "LIGHT_PURPLE", "PURPLE", "BROWN", "RED"};

    for (String color : colorGroups) {
        if (ruleEngine.hasColorMonopoly(player, color)) {
            monopolizedColors.add(color);
        }
    }

    return monopolizedColors.size() >= 3;
}
```

**승리 처리:**
```java
if (victory) {
    state = GameState.GAME_OVER;

    String victoryMessage = String.format(
        "%s 승리! (%s)\n최종 자산: %,d원",
        player.name,
        victoryType,
        player.cash
    );

    int choice = JOptionPane.showOptionDialog(
        frame,
        victoryMessage,
        "게임 종료",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.INFORMATION_MESSAGE,
        null,
        new String[]{"재시작", "종료"},
        "재시작"
    );

    if (choice == 0) restartGame();
    else System.exit(0);
}
```

### 6.11 UI 크기 조정

**배경:** 원본 UI가 너무 커서 보드를 가림

**변경:**
1. 모든 오버레이 컴포넌트를 원본의 70% 크기로 축소
2. 행동 버튼만 77% 크기로 (70% × 1.1)

**크기 변화표:**

| 항목 | 원본 | 30% 축소 | 최종 (버튼 10% 증가) |
|-----|------|----------|----------------------|
| 턴 라벨 | 200×50 | 140×35 | 140×35 |
| 턴 폰트 | 24px | 17px | 17px |
| 주사위 패널 | 180×100 | 126×70 | 126×70 |
| 홀짝 패널 | 140×70 | 98×49 → **140×49** | 140×49 (너비 증가) |
| 홀짝 버튼 | 60×60 | 42×42 | 42×42 |
| 게이지 | 320×60 | 224×42 | 224×42 |
| 버튼 패널 | 280×80 | 196×56 | **216×62** |
| 버튼 크기 | 260×35 | 182×25 | **200×28** |
| 버튼 폰트 | 14px | 10px | **11px** |
| 플레이어 카드 | 200×120 | 140×84 | **160×70** |

**코드 위치:**
- `ui/OverlayPanel.repositionComponents()` - 모든 크기 계산
- `ui/OverlayPanel.updateButtonSizes()` - 버튼 크기 업데이트

---

## 7. 설정 & 상수

### 7.1 게임 규칙 (RuleEngine.java)

```java
// 금액
private static final int SALARY = 200000;          // START 통과 급여
private static final int BAIL_COST = 200000;       // 무인도 보석금
private static final int CHANCE_REWARD = 100000;   // 찬스 보상

// 무인도
private static final int ISLAND_MAX_TURNS = 2;     // 최대 대기 턴

// 통행료 배수 (레벨별)
private static final double[] tollMultiplierByLevel = {
    0.0,   // L0 (미소유) - 통행료 없음
    1.5,   // L1 (주택)   - baseToll × 1.5
    2.2,   // L2 (아파트) - baseToll × 2.2
    3.0,   // L3 (빌딩)   - baseToll × 3.0
    4.0    // L4 (랜드마크) - baseToll × 4.0
};

// 색상 독점 보너스
private static final double COLOR_MONOPOLY_MULTIPLIER = 1.5; // 통행료 1.5배

// 세금
private static final double TAX_RATE = 0.10;       // 보유금의 10%

// 업그레이드 비용 (City.java)
int upgradeCost = (level < 3) ? (int)(price * 0.30) : (int)(price * 0.40);
// L1-3: 30%, L4(랜드마크): 40%

// 인수 비용 (City.java)
int takeoverPrice = (int)(price * (1.0 + level * 0.5));
// L1: 1.5×base, L2: 2.0×base, L3: 2.5×base, L4: 인수불가

// 관광지 통행료 (TouristSpot.java)
int toll = price * 2; // 기본가의 200%
```

### 7.2 보드 구성 (Board.java)

```java
// 그리드
private static final int BOARD_SIZE = 9;           // 9×9 그리드
private static final int BASE_TILE_SIZE = 80;      // 타일 기본 크기 80px
private static final int TOTAL_TILES = 32;         // 총 타일 수

// 색상 그룹 (가격 낮은 순)
// LIME < GREEN < CYAN < BLUE < LIGHT_PURPLE < PURPLE < BROWN < RED

// 도시 가격 범위
// LIME:         150,000원
// GREEN:        180,000 ~ 200,000원
// CYAN:         220,000원
// BLUE:         240,000 ~ 260,000원
// LIGHT_PURPLE: 280,000 ~ 300,000원
// PURPLE:       320,000 ~ 340,000원
// BROWN:        380,000원
// RED:          400,000원

// 관광지
private static final int TOURIST_SPOT_PRICE = 200000; // 모든 관광지 동일
```

### 7.3 UI 색상 (다크 테마)

```java
// 배경
Color BACKGROUND_DARK     = new Color(32, 33, 36);      // #202124
Color CARD_BACKGROUND     = new Color(52, 73, 94);      // #34495E
Color BOARD_BG            = new Color(44, 62, 80);      // #2C3E50

// 텍스트
Color TEXT_PRIMARY        = new Color(232, 234, 237);   // #E8EAED
Color TEXT_SECONDARY      = new Color(189, 195, 199);   // #BDC3C7
Color ACCENT_COLOR        = new Color(138, 180, 248);   // #8AB4F8

// 플레이어 색상 (PLAYER_COLORS)
Color PLAYER_0 = new Color(231, 76, 60);    // #E74C3C (Red)
Color PLAYER_1 = new Color(52, 152, 219);   // #3498DB (Blue)
Color PLAYER_2 = new Color(46, 204, 113);   // #2ECC71 (Green)
Color PLAYER_3 = new Color(230, 126, 34);   // #E67E22 (Orange)

// 버튼 색상
Color BUTTON_ROLL      = new Color(41, 128, 185);   // #2980B9 (Blue)
Color BUTTON_PURCHASE  = new Color(39, 174, 96);    // #27AE60 (Green)
Color BUTTON_UPGRADE   = new Color(243, 156, 18);   // #F39C12 (Orange)
Color BUTTON_TAKEOVER  = new Color(142, 68, 173);   // #8E44AD (Purple)
Color BUTTON_SKIP      = new Color(127, 140, 141);  // #7F8C8D (Gray)
Color BUTTON_ESCAPE    = new Color(192, 57, 43);    // #C0392B (Red)

// 게이지 색상 (연료 게이지)
Color YELLOW_COLOR       = new Color(255, 235, 59);   // #FFEB3B (0-33%)
Color ORANGE_COLOR       = new Color(255, 152, 0);    // #FF9800 (33-67%)
Color RED_COLOR          = new Color(244, 67, 54);    // #F44336 (67-100%)
Color EMPTY_GAUGE_COLOR  = new Color(60, 60, 60);     // #3C3C3C (빈 부분)

// 색상 그룹 (보드 타일)
"LIME"         → new Color(50, 205, 50)       // 라임
"GREEN"        → new Color(34, 139, 34)       // 초록
"CYAN"         → new Color(0, 206, 209)       // 시안
"BLUE"         → new Color(30, 144, 255)      // 파랑
"LIGHT_PURPLE" → new Color(186, 85, 211)      // 연보라
"PURPLE"       → new Color(138, 43, 226)      // 보라
"BROWN"        → new Color(139, 69, 19)       // 갈색
"RED"          → new Color(220, 20, 60)       // 빨강
```

### 7.4 애니메이션 타이밍

```java
// 주사위 애니메이션 (DiceAnimationPanel)
private static final int ANIMATION_DURATION = 1400;  // 1.4초
private static final int FRAME_INTERVAL = 60;        // 60ms (약 16fps)
// Easing: Cubic-out

// 게이지 애니메이션 (DiceGauge)
private static final long GAUGE_PERIOD = 2000;       // 2초 주기 (왕복)
// 0.0 → 1.0 → 0.0 (sinusoidal oscillation)

// 게이지 렌더링 (GaugePanel)
private Timer animationTimer = new Timer(33, ...);   // 30fps
```

---

## 8. 디자인 패턴 & 규칙

### 8.1 아키텍처 패턴

**MVC-like 구조:**
```
Model (model/)
  - Tile, City, TouristSpot, Player, Board, Dice, DiceGauge
  - Pure data classes with minimal logic
  - Getters/Setters only

Controller (core/)
  - GameUI: Main game controller
  - RuleEngine: Game rules calculator
  - Orchestrates between Model and View

View (ui/)
  - GameFrame, BoardPanel, OverlayPanel, etc.
  - Swing components
  - Event listeners
  - Rendering logic
```

**이벤트 기반:**
- Swing ActionListener for button clicks
- MouseListener for press-and-hold, tile clicks
- ComponentListener for resize events
- Timer for animations

**상태 머신:**
- GameState enum for game flow control
- State transitions based on player actions
- Centralized state management in GameUI

### 8.2 상속 구조

```
Tile (base)
  ├── City (purchasable, upgradeable, 4 levels)
  └── TouristSpot (purchasable, fixed toll, no upgrade)

JPanel (Swing)
  ├── GameFrame (JFrame, main window)
  ├── BoardPanel (tile & player rendering)
  ├── OverlayPanel (transparent overlay, absolute positioning)
  │   └── CompactPlayerCard (inner class, player info cards)
  ├── DiceAnimationPanel (dice animation)
  ├── GaugePanel (fuel gauge visualization)
  ├── InfoPanel (deprecated, player sidebar)
  └── ControlPanel (deprecated, log area)
```

### 8.3 네이밍 컨벤션

**클래스:** PascalCase
```java
GameUI, BoardPanel, OverlayPanel, DiceGauge
```

**메서드:** camelCase
```java
rollDiceWithGauge(), updateDisplay(), handleTileLanding()
```

**상수:** UPPER_SNAKE_CASE
```java
BASE_TILE_SIZE, SALARY, COLOR_MONOPOLY_MULTIPLIER
```

**변수:** camelCase
```java
currentPlayerIndex, scaleFactor, hasRailroadTicket
```

**패키지:** lowercase
```java
com.marblegame.model, com.marblegame.core, com.marblegame.ui
```

### 8.4 코딩 스타일

**언어:**
- 주석: 한글 (가독성)
- 로그 메시지: 한글
- UI 텍스트: 한글
- 변수/메서드명: 영어

**이모지 사용:**
- UI 텍스트에 적극 활용
- 버튼: 🎲 🏠 ⭐ 💰 ⏭ 🔓
- 로그: 🎯 💰 🏝 🎊
- 타일: 🏠 🏢 🏬 🏛️

**금액 표시:**
- 쉼표 구분자: `String.format("%,d원", amount)`
- 예: "1,500,000원"

**파일 인코딩:**
- UTF-8 (한글 지원)

---

## 9. 핵심 기술 상세

### 9.1 스레드 안전성

**EDT (Event Dispatch Thread) 사용:**
```java
// 1. 프로그램 시작 (Main.java)
SwingUtilities.invokeLater(() -> {
    new GameUI(2, 1500000);
});

// 2. UI 업데이트 (GameUI.java)
private void updateDisplay() {
    SwingUtilities.invokeLater(() -> {
        frame.updateDisplay(turnCount);
    });
}

// 3. 게이지 동기화 (GameFrame.java)
SwingUtilities.invokeLater(() -> {
    overlayPanel.setScaleFactor(boardPanel.getScaleFactor());
});
```

**타이머 (Thread-safe):**
```java
// Swing Timer는 자동으로 EDT에서 실행
private Timer animationTimer = new Timer(33, e -> {
    repaint(); // EDT에서 안전
});
```

**동시성 문제 없음:**
- Single-threaded game logic
- No shared mutable state between threads
- All UI operations on EDT

### 9.2 성능 최적화

**더블 버퍼링:**
```java
// Swing 기본 제공
setDoubleBuffered(true); // Default
```

**Repaint 최적화:**
```java
// 상태 변경 시에만 repaint
public void updateBoard() {
    repaint(); // 필요할 때만
}

// 애니메이션: 타이머로 제어
animationTimer.start(); // 필요할 때만 시작
animationTimer.stop();  // 끝나면 정지
```

**Transform 캐싱:**
```java
// updateTransform()는 paintComponent에서 한 번만 호출
private void updateTransform() {
    // scaleFactor, translateX, translateY 계산
    // 결과를 필드에 저장
}

@Override
protected void paintComponent(Graphics g) {
    updateTransform(); // 캐시 업데이트
    g2d.scale(scaleFactor, scaleFactor); // 캐시된 값 사용
}
```

### 9.3 메모리 관리

**경량 컴포넌트:**
```java
// 최소한의 객체 생성
// 재사용 가능한 것은 필드로 저장
private DiceGauge diceGauge; // 한 번 생성, 계속 사용
private Timer animationTimer; // 한 번 생성, 시작/정지
```

**리소스 정리:**
```java
private void restartGame() {
    frame.dispose(); // 기존 프레임 정리

    SwingUtilities.invokeLater(() -> {
        new GameUI(players.length, 1500000); // 새 게임
    });
}
```

**이벤트 리스너 관리:**
```java
// 리스너는 한 번만 등록
private void setupEventHandlers() {
    rollDiceButton.addActionListener(...); // 단일 등록
}

// 제거 불필요 (컴포넌트와 함께 GC됨)
```

---

## 10. 실행 & 빌드

### 10.1 컴파일

```bash
# 전체 프로젝트 컴파일
javac -d out -encoding UTF-8 -sourcepath src $(find src -name "*.java")

# 또는 Main만 컴파일 (의존성 자동 해결)
javac -d out -encoding UTF-8 -sourcepath src src/com/marblegame/Main.java
```

**Note:** UTF-8 인코딩 필수 (한글 지원)

### 10.2 실행

```bash
# 스크립트 사용
./run.sh

# 직접 실행
java -cp out com.marblegame.Main

# 또는 jar로 패키징 후 실행
jar cfe BoardGame.jar com.marblegame.Main -C out .
java -jar BoardGame.jar
```

### 10.3 IntelliJ IDEA 설정

**Run Configuration:**
- Main class: `com.marblegame.Main`
- VM options: `-Dfile.encoding=UTF-8`
- Working directory: `$PROJECT_DIR$`

### 10.4 설정 변경

**플레이어 수 변경:**
```java
// Main.java
SwingUtilities.invokeLater(() -> {
    new GameUI(4, 1500000); // 2 → 4명으로 변경
});
```

**초기 자금 변경:**
```java
// Main.java
new GameUI(2, 2000000); // 1,500,000 → 2,000,000
```

**보드 수정:**
```java
// Board.java - initializeBoard()
tiles.add(new City(1, "새도시", "LIME", 200000, 100000));
```

**규칙 수정:**
```java
// RuleEngine.java
private static final int SALARY = 300000; // 200,000 → 300,000
```

---

## 11. 향후 개선 아이디어

기존 코드 구조를 기반으로 추가 가능한 기능들:

### 11.1 네트워킹 (멀티플레이어)
- 클라이언트-서버 아키텍처
- GameUI를 NetworkGameUI로 확장
- 턴 동기화, 보드 상태 동기화

### 11.2 저장/로드 시스템
- 게임 상태 직렬화 (JSON/XML)
- Board, Player, GameState 저장
- 중간 저장 / 이어하기 기능

### 11.3 AI 플레이어
- 난이도별 전략:
  - Easy: 랜덤 선택
  - Medium: 기본 전략 (독점 우선)
  - Hard: 확률 기반 최적화
- Player를 AIPlayer로 확장

### 11.4 사운드 효과
- 주사위 굴리기: 딸랑딸랑
- 돈 거래: 캐시 레지스터
- 승리: 팡파르
- javax.sound.sampled 사용

### 11.5 플레이어 이동 애니메이션
- 상태: **구현됨**
- 방식: Timer(16ms) + sin 이징으로 타일별 점프 이동 (12 스텝 + 착지 딜레이)
- 효과: 주사위 결과만큼 한 칸씩 뛰며 이동, 출발지 통과 시 급여 자동 처리, 이동 중 입력 잠금

### 11.6 통계 시스템
- 게임 기록 추적
- 플레이어별 승률, 평균 자산
- 차트 시각화 (JFreeChart)

### 11.7 커스텀 보드
- JSON 기반 보드 설정
- 타일 종류, 위치, 가격 커스터마이징
- 보드 에디터 UI

### 11.8 모바일 포팅
- Android: Java → Kotlin 변환
- iOS: Swift 재구현 (같은 게임 로직)
- 터치 UI 최적화

### 11.9 더 많은 타일 타입
- 복지(WELFARE) 구현: 10만원 지급
- 우주여행: 한 바퀴 건너뛰기
- 로또: 확률 기반 대박

### 11.10 난이도 조절
- 초기 자금 조정
- 통행료 배수 조정
- AI 난이도 선택

---

## 12. Git 커밋 히스토리

최근 주요 커밋 (역순):

```
aeb250f (HEAD -> main) 2차 수정
  - 홀수/짝수 주사위 모드 추가
  - UI 크기 30% 축소 + 버튼 10% 증가
  - 연료 게이지 스타일 변경
  - 플레이어 카드 오버레이 이동

2e52248 기본적인 룰 1차 수정
  - 승리 조건 3가지 구현
  - 랜드마크 시스템 추가
  - 올림픽 부스트 구현
  - 가격 밸런스 조정

ca4ff7b 기존 궁 대신 관광지로 수정
  - TouristSpot 타일 타입 추가
  - 5개 관광지 배치
  - 핑크 그라데이션 렌더링

7a345c7 보드 9x9, 타일 디자인 수정
  - 9x9 그리드 레이아웃
  - 32타일 순환 배치
  - 타일 시각화 개선

8717504 codex활용 가변 레이아웃 적용
  - 반응형 UI 구현
  - JLayeredPane 도입
  - 스케일링 시스템
```

**브랜치:** main (단일 브랜치)
**원격:** GitHub (private repository)

---

## 참고 사항

### 파일 위치 빠른 참조

| 기능 | 파일 경로 |
|-----|----------|
| 프로그램 진입점 | `src/com/marblegame/Main.java` |
| 게임 컨트롤러 | `src/com/marblegame/core/GameUI.java` |
| 게임 규칙 | `src/com/marblegame/core/RuleEngine.java` |
| 보드 구성 | `src/com/marblegame/model/Board.java` |
| 보드 렌더링 | `src/com/marblegame/ui/BoardPanel.java` |
| 중앙 오버레이 | `src/com/marblegame/ui/OverlayPanel.java` |
| 주사위 게이지 | `src/com/marblegame/model/DiceGauge.java` |
| 게이지 시각화 | `src/com/marblegame/ui/GaugePanel.java` |
| 플레이어 모델 | `src/com/marblegame/model/Player.java` |
| 도시 모델 | `src/com/marblegame/model/City.java` |

### 자주 수정되는 코드

1. **게임 규칙 조정:** `RuleEngine.java` 상수들
2. **보드 구성 변경:** `Board.java` initializeBoard()
3. **UI 크기 조정:** `OverlayPanel.java` repositionComponents()
4. **새 타일 이벤트:** `GameUI.java` handleTileLanding()
5. **승리 조건:** `GameUI.java` checkVictory()

### 디버깅 팁

**로그 출력 확인:**
```java
frame.getControlPanel().addLog("디버그 메시지");
```

**현재 상태 확인:**
```java
System.out.println("State: " + state);
System.out.println("Player: " + players[currentPlayerIndex].name);
System.out.println("Position: " + players[currentPlayerIndex].pos);
System.out.println("Cash: " + players[currentPlayerIndex].cash);
```

**ScaleFactor 확인:**
```java
System.out.println("BoardPanel scaleFactor: " + boardPanel.getScaleFactor());
System.out.println("OverlayPanel scaleFactor: " + overlayPanel.scaleFactor);
```

---

## 요약

이 프로젝트는 **Java Swing 기반 모노폴리 스타일 보드게임**으로, 다음과 같은 특징을 가집니다:

✅ **MVC 아키텍처** - model, core, ui 패키지 분리

✅ **반응형 UI** - 창 크기 변경에 따른 자동 스케일링

✅ **다크 테마** - 전문적인 UI 디자인

✅ **고급 게임 메커니즘** - 게이지, 홀짝, 랜드마크, 올림픽

✅ **3가지 승리 조건** - 다양한 전략적 플레이

✅ **확장 가능한 구조** - 새 타일/규칙 추가 용이

**핵심 파일 3개:**
1. `GameUI.java` - 게임 흐름 제어
2. `BoardPanel.java` - 보드 렌더링
3. `OverlayPanel.java` - 중앙 UI

---

## 13. 다이얼로그 UI 디자인 가이드라인

### 13.1 다이얼로그 사용 원칙

**다이얼로그를 사용해야 하는 경우:**
- ✅ 사용자의 **확인이나 선택이 필요한 경우** (매입, 레벨 선택 등)
- ✅ 중요한 **정보를 명확히 표시**해야 하는 경우
- ✅ 사용자가 **실수로 작업을 수행하지 않도록** 방지해야 하는 경우
- ✅ **비용 정보**를 보여주고 확인받아야 하는 경우

**다이얼로그를 사용하지 말아야 하는 경우:**
- ❌ 단순 알림 메시지 (로그로 충분)
- ❌ 게임 플로우를 방해하는 빈번한 팝업
- ❌ 정보만 전달하고 사용자 액션이 필요 없는 경우

### 13.2 다이얼로그 디자인 패턴

#### 📐 레이아웃 구조

```
┌─────────────────────────────────────┐
│  HEADER PANEL (헤더)                │
│  - 제목 (20px, Bold)                │
│  - 부제목/설명 (14px, Plain)         │
│  - 보유 자금 정보 (13px, Bold, 노란색)│
├─────────────────────────────────────┤
│  CENTER PANEL (선택 옵션 또는 정보)  │
│  - 레벨 선택 버튼 (도시)            │
│  - 정보 행 (관광지)                 │
│  - 최대 3-4개 옵션                  │
├─────────────────────────────────────┤
│  SOUTH PANEL (액션 버튼)            │
│  - 확인 버튼 (녹색)                 │
│  - 취소 버튼 (회색)                 │
└─────────────────────────────────────┘
```

#### 🎨 색상 팔레트

```java
// 다크 테마 기본 색상
private static final Color BACKGROUND_DARK = new Color(32, 33, 36);
private static final Color PANEL_DARK = new Color(44, 47, 51);
private static final Color TEXT_PRIMARY = new Color(232, 234, 237);
private static final Color TEXT_SECONDARY = new Color(189, 195, 199);

// 버튼 색상
private static final Color BUTTON_CONFIRM = new Color(39, 174, 96);   // 녹색 (확인)
private static final Color BUTTON_LEVEL1 = new Color(39, 174, 96);    // 녹색 (레벨 1)
private static final Color BUTTON_LEVEL2 = new Color(41, 128, 185);   // 파란색 (레벨 2)
private static final Color BUTTON_LEVEL3 = new Color(142, 68, 173);   // 보라색 (레벨 3)
private static final Color BUTTON_CANCEL = new Color(127, 140, 141);  // 회색 (취소)
private static final Color BUTTON_DISABLED = new Color(60, 63, 65);   // 어두운 회색 (비활성)

// 강조 색상
private static final Color ACCENT_YELLOW = new Color(255, 193, 7);    // 노란색 (자금)
```

#### 📝 텍스트 스타일

```java
// 제목
titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
titleLabel.setForeground(TEXT_PRIMARY);

// 부제목/설명
messageLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
messageLabel.setForeground(TEXT_SECONDARY);

// 보유 자금 (강조)
cashLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
cashLabel.setForeground(ACCENT_YELLOW);

// 버튼
button.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
```

#### 🔘 버튼 디자인

```java
private JButton createButton(String text, Color bgColor) {
    JButton button = new JButton(text);
    button.setPreferredSize(new Dimension(120, 40));
    button.setBackground(bgColor);
    button.setForeground(TEXT_PRIMARY);
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setOpaque(true);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));

    // 호버 효과
    Color hoverColor = bgColor.brighter();
    button.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            if (button.isEnabled()) {
                button.setBackground(hoverColor);
            }
        }
        public void mouseExited(java.awt.event.MouseEvent evt) {
            if (button.isEnabled()) {
                button.setBackground(bgColor);
            }
        }
    });

    return button;
}
```

#### 💡 자금 부족 처리

```java
boolean canAfford = playerCash >= price;
button.setEnabled(canAfford);

if (!canAfford) {
    button.setBackground(BUTTON_DISABLED);
    button.setForeground(new Color(150, 150, 150));
}
```

### 13.3 구현된 다이얼로그 예시

#### 1. LevelSelectionDialog.java (도시 레벨 선택)

**용도:** 도시 매입 시 레벨 1-3 중 선택

**특징:**
- 3개 레벨 버튼 (🏠 집, 🏢 아파트, 🏬 건물)
- 각 레벨의 누적 건설 비용 표시
- 자금 부족 시 버튼 자동 비활성화
- 취소 옵션 제공

**사용 위치:** `GameUI.purchaseCity()` - 도시 타일 매입 시

#### 2. TouristSpotPurchaseDialog.java (관광지 매입 확인)

**용도:** 관광지 매입 확인

**특징:**
- 매입 가격, 보유 자금, 매입 후 잔액 표시
- "관광지는 업그레이드 불가" 안내 문구
- 자금 부족 시 매입하기 버튼 비활성화
- 취소 옵션 제공

**사용 위치:** `GameUI.purchaseCity()` - 관광지 타일 매입 시

### 13.4 새 다이얼로그 추가 시 체크리스트

#### ✅ 필수 구현 사항

1. **Modal Dialog 설정**
   ```java
   super(parent, "제목", true); // true = modal
   ```

2. **다크 테마 색상 사용**
   - BACKGROUND_DARK, PANEL_DARK
   - TEXT_PRIMARY, TEXT_SECONDARY

3. **자금 정보 표시** (금액 관련 경우)
   - 보유 자금 (노란색 강조)
   - 필요 금액
   - 잔액 계산

4. **호버 효과**
   - 버튼에 마우스 올렸을 때 색상 밝게
   - 커서 HAND_CURSOR로 변경

5. **비활성화 처리**
   - 자금 부족 시 버튼 비활성화
   - 회색 배경 + 회색 텍스트

6. **취소 옵션**
   - 항상 취소 버튼 제공
   - ESC 키로 닫기 (기본)

7. **중앙 정렬**
   ```java
   setLocationRelativeTo(parent);
   ```

8. **크기 조정 비활성화**
   ```java
   setResizable(false);
   ```

#### 📋 코드 템플릿

```java
public class MyDialog extends JDialog {
    private boolean confirmed = false;

    public MyDialog(JFrame parent, String title, int playerCash) {
        super(parent, title, true);
        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_DARK);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
```

---

## 14. 최근 세션 업데이트 (2025-01-XX)

### 14.1 주사위 게이지 4단계 분할

**변경 내용:**
- 게이지 구간을 **3단계 → 4단계**로 변경
- 눈금 표시 추가 (25%, 50%, 75% 위치)

**수정 파일:**
- `DiceGauge.java`: 섹션 경계 및 확률 분포 변경
- `GaugePanel.java`: 4단계 색상 및 눈금 표시 추가

**색상 및 주사위 값 매핑:**

| 구간 | 색상 | 주사위 값 (60% 확률) |
|------|------|---------------------|
| 0-25% | 노란색 | 2, 3, 4 |
| 25-50% | 연주황색 | 5, 6, 7 |
| 50-75% | 주황색 | 8, 9, 10 |
| 75-100% | 빨간색 | 11, 12 |

**코드 변경:**

```java
// DiceGauge.java
private static final double SECTION1_END = 0.25;  // 0-25%
private static final double SECTION2_END = 0.50;  // 25-50%
private static final double SECTION3_END = 0.75;  // 50-75%
                                                   // 75-100%

// GaugePanel.java
// 눈금 표시 (25%, 50%, 75%)
g.setColor(TICK_MARK_COLOR);
g.setStroke(new BasicStroke(2f));
int[] tickPositions = {width / 4, width / 2, width * 3 / 4};
for (int tickX : tickPositions) {
    g.drawLine(x + tickX, y, x + tickX, y + height);
}
```

### 14.2 도시 즉시 레벨 선택 건설 시스템

**변경 내용:**
- 도시 매입 시 레벨 1부터 시작하는 대신 **레벨 1, 2, 3 중 선택** 가능
- 레벨 선택 다이얼로그 UI 추가
- 누적 건설 비용 계산 시스템

**신규 파일:**
- `LevelSelectionDialog.java`: 레벨 선택 다이얼로그 UI

**수정 파일:**
- `RuleEngine.java`: `purchaseCityWithLevel()`, `calculateLevelCost()` 메서드 추가
- `GameUI.java`: `purchaseCity()` 메서드 수정, `getLevelName()` 헬퍼 추가

**비용 계산 공식:**

| 레벨 | 건물 | 비용 공식 | 예시 (base = 150,000원) |
|------|------|-----------|------------------------|
| 1 | 🏠 집 | basePrice | 150,000원 |
| 2 | 🏢 아파트 | basePrice × 1.3 | 195,000원 |
| 3 | 🏬 건물 | basePrice × 1.6 | 240,000원 |

**주요 코드:**

```java
// RuleEngine.java
public int calculateLevelCost(int basePrice, int level) {
    switch (level) {
        case 1: return basePrice;
        case 2: return (int)(basePrice * 1.3);
        case 3: return (int)(basePrice * 1.6);
        default: return basePrice;
    }
}

// GameUI.java
LevelSelectionDialog dialog = new LevelSelectionDialog(
    frame, city.name, city.price, player.cash
);
dialog.setVisible(true);
int selectedLevel = dialog.getSelectedLevel();
```

### 14.3 관광지 매입 확인 다이얼로그

**변경 내용:**
- 관광지 매입 시 **확인 다이얼로그** 추가
- 매입 가격, 보유 자금, 매입 후 잔액 표시
- "관광지는 업그레이드 불가" 안내 문구

**신규 파일:**
- `TouristSpotPurchaseDialog.java`: 관광지 매입 확인 다이얼로그

**수정 파일:**
- `GameUI.java`: `purchaseCity()` 메서드에 관광지 다이얼로그 추가

**주요 코드:**

```java
// GameUI.java
TouristSpotPurchaseDialog dialog = new TouristSpotPurchaseDialog(
    frame, touristSpot.name, touristSpot.price, player.cash
);
dialog.setVisible(true);

if (!dialog.isConfirmed()) {
    log("구매를 취소했습니다.");
    endTurn();
    return;
}
```

### 14.4 가격 라벨 제거

**변경 내용:**
- 버튼 위에 표시되던 **가격 라벨 제거** (매입 비용, 업그레이드 비용, 인수 비용)
- 다이얼로그에서 모든 비용 정보 확인 가능
- 세금 라벨만 유지

**수정 파일:**
- `OverlayPanel.java`:
  - `purchasePriceLabel`, `upgradePriceLabel`, `takeoverPriceLabel` 필드 제거
  - `setPurchasePrice()`, `setUpgradePrice()`, `setTakeoverPrice()` 메서드 제거
  - `refreshPriceLabelVisibility()`, `hasText()` 메서드 제거
- `GameUI.java`:
  - `setPurchasePrice()`, `setUpgradePrice()`, `setTakeoverPrice()` 호출 제거

**기존 UI:**
```
┌─────────────────┐
│ 매입 비용: 150,000원 │  ← 제거됨
│ [🏠 매입하기]   │
└─────────────────┘
```

**새 UI:**
```
┌─────────────────┐
│ [🏠 매입하기]   │  ← 클릭 시 다이얼로그 표시
└─────────────────┘
```

### 14.5 파일 변경 요약

**신규 파일 (2개):**
1. `LevelSelectionDialog.java` - 도시 레벨 선택 다이얼로그
2. `TouristSpotPurchaseDialog.java` - 관광지 매입 확인 다이얼로그

**수정 파일 (4개):**
1. `DiceGauge.java` - 4단계 게이지 시스템
2. `GaugePanel.java` - 4단계 색상 및 눈금 표시
3. `RuleEngine.java` - 레벨별 구매 로직
4. `GameUI.java` - 다이얼로그 통합, 가격 라벨 호출 제거
5. `OverlayPanel.java` - 가격 라벨 UI 제거

**삭제된 기능:**
- 버튼 위 가격 라벨 (매입/업그레이드/인수 비용)

**추가된 기능:**
- 도시 즉시 레벨 선택 건설
- 관광지 매입 확인 다이얼로그
- 주사위 게이지 4단계 분할 + 눈금 표시

---

**마지막 업데이트:** 2025년 1월

**버전:** 3.0 (3차 수정 완료 - 다이얼로그 시스템 추가)

**문서 작성:** Claude Code 자동 생성
