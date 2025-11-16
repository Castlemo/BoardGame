package com.marblegame.network.protocol;

/**
 * 네트워크 메시지 타입 정의
 */
public enum MessageType {
    // 연결 관리
    CONNECT,                // 클라이언트 연결 요청
    CONNECT_ACK,           // 서버 연결 승인
    DISCONNECT,            // 연결 종료
    PLAYER_JOIN,           // 플레이어 참가
    PLAYER_LEAVE,          // 플레이어 퇴장

    // 게임 시작
    GAME_START,            // 게임 시작 요청
    GAME_READY,            // 게임 준비 완료

    // 턴 액션
    ROLL_DICE,             // 주사위 굴리기
    BUY_CITY,              // 도시 구매
    UPGRADE,               // 도시 업그레이드
    TAKEOVER,              // 도시 인수
    PASS,                  // 패스

    // 상태 동기화
    GAME_STATE_UPDATE,     // 전체 게임 상태 업데이트
    PLAYER_STATE_UPDATE,   // 플레이어 상태 업데이트
    BOARD_STATE_UPDATE,    // 보드 상태 업데이트
    TURN_START,            // 턴 시작 알림
    TURN_END,              // 턴 종료 알림

    // 이벤트
    TILE_LANDED,           // 타일 도착 이벤트
    CHANCE_EVENT,          // 찬스 이벤트
    PHASE_DELETE,          // 페이즈 딜리트 이벤트
    ISLAND_EVENT,
    WORLD_TOUR_EVENT,
    TOLL_EVENT,
    TAX_EVENT,
    MAGNETIC_EVENT,
    DOUBLE_EVENT,          // 더블 주사위 이벤트
    OLYMPIC_EVENT,         // 올림픽 이벤트
    PLAYER_MOVED,          // 플레이어 이동
    PLAYER_BANKRUPT,       // 플레이어 파산

    // 특수 액션
    JAIL_CHOICE,           // 무인도 탈출 선택
    TOURIST_SPOT_CHOICE,   // 관광지 선택
    LEVEL_SELECTION,       // 레벨 선택
    CITY_SELECTION,        // 도시 선택 (올림픽 등)

    // 하트비트
    PING,                  // 핑
    PONG,                  // 퐁

    // 에러
    ERROR                  // 에러 메시지
}
