package com.marblegame.test;

import com.marblegame.model.DiceGauge;

/**
 * DiceGauge 확률 분포 검증 테스트
 * 10,000회 시뮬레이션으로 각 구간의 확률이 요구사항을 만족하는지 검증
 */
public class DiceGaugeTest {
    private static final int SIMULATION_COUNT = 10000;
    private static final double TOLERANCE = 0.03; // ±3%

    public static void main(String[] args) {
        System.out.println("=== 주사위 게이지 확률 분포 검증 ===\n");

        // 각 구간 테스트
        testSection1();
        testSection2();
        testSection3();

        System.out.println("\n=== 검증 완료 ===");
    }

    /**
     * S1 구간 테스트: 2~5가 60% 확률
     */
    private static void testSection1() {
        System.out.println("[ S1 구간 테스트 (2~5 우대 60%) ]");

        DiceGauge gauge = new DiceGauge();
        int[] distribution = new int[13]; // 2~12 범위

        // 시뮬레이션
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            gauge.start();
            // S1 구간 위치로 강제 설정 (0.1 = S1 영역)
            try {
                Thread.sleep(100); // 0.1초 대기 (S1 구간)
            } catch (InterruptedException e) {}

            int result = gauge.stop();
            distribution[result]++;
        }

        // 결과 분석
        printDistribution(distribution, 1);
        validateSection1(distribution);
    }

    /**
     * S2 구간 테스트: 6~9가 60% 확률
     */
    private static void testSection2() {
        System.out.println("\n[ S2 구간 테스트 (6~9 우대 60%) ]");

        DiceGauge gauge = new DiceGauge();
        int[] distribution = new int[13];

        // 시뮬레이션
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            gauge.start();
            // S2 구간 위치로 강제 설정 (1.0초 = S2 영역)
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            int result = gauge.stop();
            distribution[result]++;
        }

        printDistribution(distribution, 2);
        validateSection2(distribution);
    }

    /**
     * S3 구간 테스트: 10~12가 60% 확률
     */
    private static void testSection3() {
        System.out.println("\n[ S3 구간 테스트 (10~12 우대 60%) ]");

        DiceGauge gauge = new DiceGauge();
        int[] distribution = new int[13];

        // 시뮬레이션
        for (int i = 0; i < SIMULATION_COUNT; i++) {
            gauge.start();
            // S3 구간 위치로 강제 설정 (1.7초 = S3 영역)
            try {
                Thread.sleep(1700);
            } catch (InterruptedException e) {}

            int result = gauge.stop();
            distribution[result]++;
        }

        printDistribution(distribution, 3);
        validateSection3(distribution);
    }

    /**
     * 분포 출력
     */
    private static void printDistribution(int[] dist, int section) {
        System.out.println("결과 분포:");
        for (int i = 2; i <= 12; i++) {
            double percent = (dist[i] * 100.0) / SIMULATION_COUNT;
            String bar = "█".repeat((int)(percent / 2));
            System.out.printf("%2d: %5d회 (%5.2f%%) %s\n", i, dist[i], percent, bar);
        }
    }

    /**
     * S1 검증: 2~5가 60%±3%
     */
    private static void validateSection1(int[] dist) {
        int biasedCount = dist[2] + dist[3] + dist[4] + dist[5];
        double biasedPercent = biasedCount * 100.0 / SIMULATION_COUNT;

        System.out.printf("\n✓ 2~5 합산: %d회 (%.2f%%)\n", biasedCount, biasedPercent);

        if (Math.abs(biasedPercent - 60.0) <= TOLERANCE * 100) {
            System.out.println("✅ PASS: 60%±3% 범위 내");
        } else {
            System.out.println("❌ FAIL: 60%±3% 범위 초과");
        }
    }

    /**
     * S2 검증: 6~9가 60%±3%
     */
    private static void validateSection2(int[] dist) {
        int biasedCount = dist[6] + dist[7] + dist[8] + dist[9];
        double biasedPercent = biasedCount * 100.0 / SIMULATION_COUNT;

        System.out.printf("\n✓ 6~9 합산: %d회 (%.2f%%)\n", biasedCount, biasedPercent);

        if (Math.abs(biasedPercent - 60.0) <= TOLERANCE * 100) {
            System.out.println("✅ PASS: 60%±3% 범위 내");
        } else {
            System.out.println("❌ FAIL: 60%±3% 범위 초과");
        }
    }

    /**
     * S3 검증: 10~12가 60%±3%
     */
    private static void validateSection3(int[] dist) {
        int biasedCount = dist[10] + dist[11] + dist[12];
        double biasedPercent = biasedCount * 100.0 / SIMULATION_COUNT;

        System.out.printf("\n✓ 10~12 합산: %d회 (%.2f%%)\n", biasedCount, biasedPercent);

        if (Math.abs(biasedPercent - 60.0) <= TOLERANCE * 100) {
            System.out.println("✅ PASS: 60%±3% 범위 내");
        } else {
            System.out.println("❌ FAIL: 60%±3% 범위 초과");
        }
    }
}
