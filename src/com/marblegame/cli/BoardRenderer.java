package com.marblegame.cli;

import com.marblegame.model.*;
import java.util.List;

/**
 * 2D 텍스트 기반 보드 렌더러
 * 44칸 순환 보드를 콘솔에 출력
 */
public class BoardRenderer {
    private final Board board;
    private static final int GRID_SIZE = 12;
    private static final int CELL_WIDTH = 7;
    private static final String EMPTY_CELL = " ".repeat(CELL_WIDTH);

    public BoardRenderer(Board board) {
        this.board = board;
    }

    /**
     * 보드 출력
     * 28칸을 정사각형 형태로 배치
     */
    public void render(List<Player> players) {
        System.out.println("\n========== 게임 보드 ==========");

        String[][] grid = new String[GRID_SIZE][GRID_SIZE];

        for (int i = 0; i < board.getSize(); i++) {
            int[] coord = getTileCoordinate(i);
            grid[coord[1]][coord[0]] = padCell(formatCell(i, players));
        }

        for (int y = 0; y < GRID_SIZE; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < GRID_SIZE; x++) {
                String cell = grid[y][x];
                line.append(cell != null ? cell : EMPTY_CELL);
                if (x < GRID_SIZE - 1) {
                    line.append(' ');
                }
            }
            System.out.println(line);
        }

        System.out.println("================================\n");
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

    private int[] getTileCoordinate(int index) {
        if (index <= 11) {
            int x = 11 - index;
            return new int[]{x, 11};
        } else if (index <= 22) {
            int y = 11 - (index - 11);
            return new int[]{0, y};
        } else if (index <= 33) {
            int x = index - 22;
            return new int[]{x, 0};
        } else {
            int y = index - 33;
            return new int[]{11, y};
        }
    }

    private String padCell(String cell) {
        if (cell.length() >= CELL_WIDTH) {
            return cell;
        }
        return String.format("%-" + CELL_WIDTH + "s", cell);
    }
}
