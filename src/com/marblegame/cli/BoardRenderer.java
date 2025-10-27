package com.marblegame.cli;

import com.marblegame.model.*;
import java.util.List;

/**
 * 2D 텍스트 기반 보드 렌더러
 * 28칸 정사각형 보드를 콘솔에 출력
 */
public class BoardRenderer {
    private final Board board;

    public BoardRenderer(Board board) {
        this.board = board;
    }

    /**
     * 보드 출력
     * 28칸을 정사각형 형태로 배치
     */
    public void render(List<Player> players) {
        System.out.println("\n========== 게임 보드 ==========");

        // 상단 (0~7)
        renderTopRow(players);

        // 중간 (8~19) - 양쪽만
        renderMiddleRows(players);

        // 하단 (20~27)
        renderBottomRow(players);

        System.out.println("================================\n");
    }

    private void renderTopRow(List<Player> players) {
        for (int i = 0; i <= 7; i++) {
            System.out.print(formatCell(i, players) + " ");
        }
        System.out.println();
    }

    private void renderMiddleRows(List<Player> players) {
        // 왼쪽: 27, 26, ... 오른쪽: 8, 9, ...
        int left = 27;
        int right = 8;

        while (left >= 20 && right <= 19) {
            System.out.print(formatCell(left, players));

            // 중간 공백 (6칸 분량)
            System.out.print("                              ");

            System.out.println(formatCell(right, players));

            left--;
            right++;
        }
    }

    private void renderBottomRow(List<Player> players) {
        for (int i = 19; i >= 20 - 7; i--) {
            System.out.print(formatCell(i, players) + " ");
        }
        System.out.println();
    }

    /**
     * 각 칸 포맷팅
     */
    private String formatCell(int position, List<Player> players) {
        Tile tile = board.getTile(position);
        String cellId = String.format("%02d", position);
        String playerMarks = getPlayerMarks(position, players);

        return String.format("[%s%s]", cellId, playerMarks);
    }

    /**
     * 해당 위치에 있는 플레이어 표시
     */
    private String getPlayerMarks(int position, List<Player> players) {
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.pos == position && !player.bankrupt) {
                marks.append((char)('A' + i));
            }
        }
        return marks.toString();
    }

    /**
     * 보드 상세 정보 출력
     */
    public void renderDetails() {
        System.out.println("\n===== 보드 정보 =====");
        for (int i = 0; i < board.getSize(); i++) {
            Tile tile = board.getTile(i);
            System.out.println(tile);
        }
        System.out.println("======================\n");
    }
}
