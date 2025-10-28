package com.marblegame.model;

/**
 * 주사위 게이지 시스템
 * 3구간(S1, S2, S3)을 왕복하며 편향된 확률로 주사위 결과 생성
 */
public class DiceGauge {
    // 게이지 파라미터
    private static final double PERIOD = 2.0; // 주기 2초
    private static final double BIAS = 0.6; // 우대 확률 60%

    // 구간 경계
    private static final double SECTION1_END = 0.333;
    private static final double SECTION2_END = 0.666;

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
     * 현재 구간 반환 (1, 2, 3)
     */
    public int getCurrentSection() {
        if (currentPosition < SECTION1_END) {
            return 1;
        } else if (currentPosition < SECTION2_END) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * 구간별 편향 확률로 2D6 주사위 굴리기
     * @param section 구간 (1, 2, 3)
     * @return 주사위 합 (2~12)
     */
    private int rollBiased(int section) {
        if (section == 1) {
            // S1: 2~5가 60% 확률
            if (Math.random() < BIAS) {
                int sum = 2 + (int)(Math.random() * 4); // 2, 3, 4, 5 중 선택
                return sum;
            } else {
                // 나머지 40%: 6~12 균등 분배
                int sum = 6 + (int)(Math.random() * 7); // 6, 7, 8, 9, 10, 11, 12 중 선택
                return sum;
            }
        } else if (section == 2) {
            // S2: 6~9가 60% 확률
            if (Math.random() < BIAS) {
                int sum = 6 + (int)(Math.random() * 4); // 6, 7, 8, 9 중 선택
                return sum;
            } else {
                // 나머지 40%: 2~5, 10~12 균등 분배
                // 2~5: 4개, 10~12: 3개 = 총 7개
                int[] others = {2, 3, 4, 5, 10, 11, 12};
                int sum = others[(int)(Math.random() * others.length)];
                return sum;
            }
        } else {
            // S3: 10~12가 60% 확률
            if (Math.random() < BIAS) {
                int sum = 10 + (int)(Math.random() * 3); // 10, 11, 12 중 선택
                return sum;
            } else {
                // 나머지 40%: 2~9 균등 분배
                int sum = 2 + (int)(Math.random() * 8); // 2, 3, 4, 5, 6, 7, 8, 9 중 선택
                return sum;
            }
        }
    }

    /**
     * 특정 합이 나오도록 2D6 생성
     */
    private int rollDiceForSum(int sum) {
        // sum이 나올 수 있는 조합 중 랜덤 선택
        int d1 = 1 + (int)(Math.random() * 6);
        int d2 = sum - d1;

        // d2가 유효 범위가 아니면 재조정
        if (d2 < 1 || d2 > 6) {
            d1 = Math.max(1, Math.min(6, sum - 6));
            d2 = sum - d1;
        }

        return d1 + d2;
    }

    /**
     * 특정 범위의 합 생성
     */
    private int rollDiceForNonBiased(int min, int max) {
        return min + (int)(Math.random() * (max - min + 1));
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
