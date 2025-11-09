package com.marblegame.model;

/**
 * 주사위 게이지 시스템
 * 4구간(S1, S2, S3, S4)을 왕복하며 편향된 확률로 주사위 결과 생성
 */
public class DiceGauge {
    // 게이지 파라미터
    private static final double PERIOD = 2.0; // 주기 2초
    private static final double BIAS = 0.6; // 우대 확률 60%

    // 구간 경계 (4등분)
    private static final double SECTION1_END = 0.25;  // 0-25%: 노란색
    private static final double SECTION2_END = 0.50;  // 25-50%: 연주황색
    private static final double SECTION3_END = 0.75;  // 50-75%: 주황색
                                                       // 75-100%: 빨간색

    // 게이지 상태
    private boolean running = false;
    private long startTime = 0;
    private double currentPosition = 0.0; // 0.0 ~ 1.0
    private boolean movingForward = true; // 왕복 방향

    /**
     * 게이지 시작
     */
    public void start() {
        running = true;
        startTime = System.currentTimeMillis();
        currentPosition = 0.0;
        movingForward = true;
    }

    /**
     * 게이지 정지 및 주사위 결과 생성
     * @return 2개의 주사위 값 합 (2~12)
     */
    public int stop() {
        if (!running) {
            return rollNormal();
        }

        running = false;
        updatePosition();

        // 현재 구간 판정
        int section = getCurrentSection();

        // 구간별 편향 확률로 주사위 생성
        return rollBiased(section);
    }

    /**
     * 게이지 위치 업데이트
     */
    public void updatePosition() {
        if (!running) return;

        long elapsed = System.currentTimeMillis() - startTime;
        double progress = (elapsed / 1000.0) / PERIOD; // 0.0 ~ 1.0 (한 주기)

        // 주기마다 반복
        progress = progress % 1.0;

        // 왕복 운동 (0->1->0)
        if (progress < 0.5) {
            currentPosition = progress * 2.0; // 0->1
            movingForward = true;
        } else {
            currentPosition = 2.0 - progress * 2.0; // 1->0
            movingForward = false;
        }
    }

    /**
     * 현재 구간 반환 (1, 2, 3, 4)
     */
    public int getCurrentSection() {
        if (currentPosition < SECTION1_END) {
            return 1;
        } else if (currentPosition < SECTION2_END) {
            return 2;
        } else if (currentPosition < SECTION3_END) {
            return 3;
        } else {
            return 4;
        }
    }

    /**
     * 구간별 편향 확률로 2D6 주사위 굴리기
     * @param section 구간 (1, 2, 3, 4)
     * @return 주사위 합 (2~12)
     */
    private int rollBiased(int section) {
        if (section == 1) {
            // S1 (0-25%, 노란색): 2~4만 100% 확률
            return 2 + (int)(Math.random() * 3); // 2,3,4
        } else if (section == 2) {
            // S2 (25-50%, 연주황색): 4~6만 100% 확률
            return 4 + (int)(Math.random() * 3); // 4,5,6
        } else if (section == 3) {
            // S3 (50-75%, 주황색): 7~10만 100% 확률
            return 7 + (int)(Math.random() * 4); // 7,8,9,10
        } else {
            // S4 (75-100%, 빨간색): 9~12만 100% 확률
            return 9 + (int)(Math.random() * 4); // 9,10,11,12
        }
    }

    /**
     * 일반 주사위 (편향 없음)
     */
    private int rollNormal() {
        return 2 + (int)(Math.random() * 11); // 2~12
    }

    /**
     * 게이지 실행 여부
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 현재 게이지 위치 (0.0 ~ 1.0)
     */
    public double getCurrentPosition() {
        if (running) {
            updatePosition();
        }
        return currentPosition;
    }
}
