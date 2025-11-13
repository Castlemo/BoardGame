package com.marblegame.ui;

import com.marblegame.network.lobby.LobbySlotView;
import com.marblegame.network.lobby.LobbyStateView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 호스트가 슬롯 현황을 모니터링하고 필요 시 슬롯을 해제할 수 있는 간단한 패널.
 */
public class HostLobbyFrame extends JFrame {
    private final IntConsumer slotReleaseHandler;
    private final JPanel slotsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
    private final JLabel readyLabel = new JLabel("로비 정보를 기다리는 중...", SwingConstants.LEFT);
    private final JLabel spectatorLabel = new JLabel("관전자: 0명", SwingConstants.RIGHT);
    private final List<SlotRow> slotRows = new ArrayList<>();

    public HostLobbyFrame(IntConsumer slotReleaseHandler) {
        super("호스트 로비 모니터");
        this.slotReleaseHandler = slotReleaseHandler;
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(new Dimension(360, 480));
        setAlwaysOnTop(false);
        buildLayout();
    }

    private void buildLayout() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(content);

        JLabel title = new JLabel("클라이언트 슬롯 상태", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        content.add(title, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(slotsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        content.add(scrollPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        readyLabel.setForeground(new Color(236, 240, 241));
        spectatorLabel.setForeground(new Color(189, 195, 199));
        statusPanel.add(readyLabel, BorderLayout.CENTER);
        statusPanel.add(spectatorLabel, BorderLayout.EAST);
        content.add(statusPanel, BorderLayout.SOUTH);
    }

    public void update(LobbyStateView view) {
        if (view == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> render(view));
    }

    private void render(LobbyStateView view) {
        ensureSlotRows(view.maxPlayers);
        spectatorLabel.setText("관전자: " + view.spectatorCount + "명");
        readyLabel.setText(buildReadySummary(view));
        for (LobbySlotView slot : view.slots) {
            if (slot.index >= 0 && slot.index < slotRows.size()) {
                slotRows.get(slot.index).render(slot);
            }
        }
        slotsPanel.revalidate();
        slotsPanel.repaint();
    }

    private void ensureSlotRows(int required) {
        if (slotRows.size() == required) {
            return;
        }
        slotRows.clear();
        slotsPanel.removeAll();
        for (int i = 0; i < required; i++) {
            SlotRow row = new SlotRow(i);
            slotRows.add(row);
            slotsPanel.add(row);
        }
    }

    private String buildReadySummary(LobbyStateView view) {
        int assigned = 0;
        int readyCount = 0;
        for (LobbySlotView slot : view.slots) {
            if (slot.occupied) {
                assigned++;
                if (slot.ready) {
                    readyCount++;
                }
            }
        }
        if (assigned == 0) {
            return "할당된 슬롯 없음";
        }
        return "준비 상태: " + readyCount + "/" + assigned;
    }

    private final class SlotRow extends JPanel {
        private final int slotIndex;
        private final JLabel nameLabel = new JLabel();
        private final JLabel readyLabel = new JLabel();
        private final JButton releaseButton = new JButton("슬롯 해제");

        SlotRow(int slotIndex) {
            super(new BorderLayout(6, 6));
            this.slotIndex = slotIndex;
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(44, 62, 80)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
            ));
            nameLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
            readyLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            readyLabel.setForeground(new Color(189, 195, 199));
            add(nameLabel, BorderLayout.CENTER);
            add(readyLabel, BorderLayout.SOUTH);
            releaseButton.addActionListener(e -> {
                if (slotReleaseHandler != null) {
                    slotReleaseHandler.accept(this.slotIndex);
                }
            });
            add(releaseButton, BorderLayout.EAST);
        }

        void render(LobbySlotView slot) {
            String baseLabel = slot.label == null ? "Slot " + (slot.index + 1) : slot.label;
            if (slot.occupied) {
                String occupant = slot.occupantName == null || slot.occupantName.isEmpty()
                    ? "미확인 플레이어"
                    : slot.occupantName;
                nameLabel.setText(baseLabel + " - " + occupant);
                readyLabel.setText(slot.ready ? "준비 완료" : "대기 중");
                readyLabel.setForeground(slot.ready ? new Color(39, 174, 96) : new Color(231, 76, 60));
                releaseButton.setEnabled(true);
            } else {
                nameLabel.setText(baseLabel + " - 빈 슬롯");
                readyLabel.setText("연결 대기");
                readyLabel.setForeground(new Color(189, 195, 199));
                releaseButton.setEnabled(false);
            }
        }
    }
}
