package com.marblegame.ui;

import com.marblegame.network.ClientNetworkService;
import com.marblegame.network.listener.ServerMessageListener;
import com.marblegame.network.lobby.LobbySlotView;
import com.marblegame.network.lobby.LobbyStateCodec;
import com.marblegame.network.lobby.LobbyStateView;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import com.marblegame.network.message.ReadyStatusPayload;
import com.marblegame.network.message.SlotAssignmentPayload;
import com.marblegame.network.message.SlotRequestPayload;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * 클라이언트 측 로비 UI. 슬롯 현황과 준비 상태를 표시하고 요청을 전송한다.
 */
public class ClientLobbyFrame extends JFrame {
    private final ClientNetworkService networkService;
    private final ServerMessageListener listener;
    private final JTextField nameField = new JTextField();
    private final JButton readyButton = new JButton("준비");
    private final JButton releaseButton = new JButton("슬롯 해제");
    private final JLabel spectatorLabel = new JLabel("관전 중: 0명");
    private final JLabel statusLabel = new JLabel("슬롯을 선택하세요.");
    private final JPanel slotsPanel = new JPanel();
    private final List<SlotRow> slotRows = new ArrayList<>();

    private int assignedSlot = -1;
    private boolean ready = false;

    public ClientLobbyFrame(ClientNetworkService networkService) {
        super("멀티플레이 로비");
        this.networkService = networkService;
        this.listener = this::handleServerMessage;
        this.networkService.addMessageListener(listener);
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(420, 520));
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(content);

        JLabel title = new JLabel("게임 로비", SwingConstants.CENTER);
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        content.add(title, BorderLayout.NORTH);

        slotsPanel.setLayout(new GridLayout(0, 1, 8, 8));
        JScrollPane scrollPane = new JScrollPane(slotsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        content.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("플레이어 이름"), BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);
        bottomPanel.add(namePanel, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        readyButton.addActionListener(e -> toggleReady());
        readyButton.setEnabled(false);
        releaseButton.addActionListener(e -> releaseSlot());
        releaseButton.setEnabled(false);
        actionPanel.add(readyButton);
        actionPanel.add(releaseButton);
        bottomPanel.add(actionPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        spectatorLabel.setForeground(new Color(189, 195, 199));
        statusPanel.add(spectatorLabel, BorderLayout.WEST);
        statusLabel.setForeground(new Color(236, 240, 241));
        statusPanel.add(statusLabel, BorderLayout.SOUTH);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        content.add(bottomPanel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
    }

    private void handleServerMessage(NetworkMessage message) {
        if (message.getType() == MessageType.LOBBY_STATE) {
            try {
                LobbyStateView view = LobbyStateCodec.decode(message.getPayload());
                SwingUtilities.invokeLater(() -> applyLobbyState(view));
            } catch (IllegalArgumentException ex) {
                System.err.println("[Client] 로비 상태 파싱 실패: " + ex.getMessage());
            }
            return;
        }
        if (message.getType() == MessageType.SLOT_ASSIGNMENT) {
            try {
                SlotAssignmentPayload payload = SlotAssignmentPayload.decode(message.getPayload());
                SwingUtilities.invokeLater(() -> applySlotAssignment(payload));
            } catch (IllegalArgumentException ex) {
                System.err.println("[Client] 슬롯 배정 파싱 실패: " + ex.getMessage());
            }
        }
    }

    private void applyLobbyState(LobbyStateView view) {
        spectatorLabel.setText("관전 중: " + view.spectatorCount + "명");
        ensureSlotRows(view.maxPlayers);
        for (LobbySlotView slotView : view.slots) {
            if (slotView.index >= 0 && slotView.index < slotRows.size()) {
                slotRows.get(slotView.index).render(slotView);
            }
        }
        slotsPanel.revalidate();
        slotsPanel.repaint();
    }

    private void applySlotAssignment(SlotAssignmentPayload payload) {
        switch (payload.getStatus()) {
            case ASSIGNED:
                assignedSlot = payload.getSlotIndex();
                ready = false;
                readyButton.setEnabled(true);
                readyButton.setText("준비");
                releaseButton.setEnabled(true);
                statusLabel.setText("슬롯 #" + (assignedSlot + 1) + " 배정 완료");
                break;
            case RELEASED:
                assignedSlot = -1;
                ready = false;
                readyButton.setEnabled(false);
                readyButton.setText("준비");
                releaseButton.setEnabled(false);
                statusLabel.setText("슬롯을 비웠습니다.");
                break;
            case DENIED:
                statusLabel.setText(payload.getNote().isEmpty() ? "요청이 거절되었습니다." : payload.getNote());
                break;
            default:
                break;
        }
        if (!payload.getNote().isEmpty() && payload.getStatus() != SlotAssignmentPayload.Status.DENIED) {
            statusLabel.setText(payload.getNote());
        }
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

    private void requestSlot(int slotIndex) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력해주세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SlotRequestPayload payload = new SlotRequestPayload(slotIndex, name);
        networkService.send(new NetworkMessage(MessageType.SLOT_REQUEST, SlotRequestPayload.encode(payload)));
    }

    private void toggleReady() {
        ready = !ready;
        readyButton.setText(ready ? "준비 해제" : "준비");
        ReadyStatusPayload payload = new ReadyStatusPayload(ready);
        networkService.send(new NetworkMessage(MessageType.READY_STATUS, ReadyStatusPayload.encode(payload)));
    }

    private void releaseSlot() {
        SlotRequestPayload payload = new SlotRequestPayload(-1, "");
        networkService.send(new NetworkMessage(MessageType.SLOT_REQUEST, SlotRequestPayload.encode(payload)));
    }

    @Override
    public void dispose() {
        networkService.removeMessageListener(listener);
        super.dispose();
    }

    private final class SlotRow extends JPanel {
        private final int slotIndex;
        private final JLabel titleLabel = new JLabel();
        private final JLabel occupantLabel = new JLabel();
        private final JButton actionButton = new JButton("요청");

        SlotRow(int slotIndex) {
            super(new BorderLayout(6, 6));
            this.slotIndex = slotIndex;
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(52, 73, 94), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            titleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
            occupantLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
            add(titleLabel, BorderLayout.WEST);
            add(occupantLabel, BorderLayout.CENTER);
            actionButton.addActionListener(e -> requestSlot(this.slotIndex));
            add(actionButton, BorderLayout.EAST);
        }

        void render(LobbySlotView slot) {
            titleLabel.setText(slot.label == null ? "Slot " + (slot.index + 1) : slot.label);
            if (slot.occupied) {
                String occupant = slot.occupantName == null || slot.occupantName.isEmpty()
                    ? "미확인 플레이어"
                    : slot.occupantName;
                occupantLabel.setText(occupant + (slot.ready ? " (준비)" : " (대기)"));
            } else {
                occupantLabel.setText("빈 슬롯");
            }
            if (slot.index == assignedSlot) {
                actionButton.setText("내 슬롯");
                actionButton.setEnabled(false);
            } else if (slot.occupied) {
                actionButton.setText("점유됨");
                actionButton.setEnabled(false);
            } else {
                actionButton.setText("요청");
                actionButton.setEnabled(true);
            }
        }
    }
}
