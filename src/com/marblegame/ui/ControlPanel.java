package com.marblegame.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 게임 로그를 표시하는 패널 (하단 배치용)
 * 수정됨: 버튼 제거 (중앙 오버레이로 이동), 로그만 표시
 */
public class ControlPanel extends JPanel {
    private JTextArea logArea;
    private JLabel logTitleLabel;

    public ControlPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(44, 62, 80));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();
    }

    private void initComponents() {
        // 로그 영역 생성
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.CENTER);
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
            new EmptyBorder(8, 8, 8, 8)
        ));

        // 제목
        logTitleLabel = new JLabel("📋 게임 로그");
        logTitleLabel.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        logTitleLabel.setForeground(new Color(236, 240, 241));
        logTitleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));

        // 로그 텍스트 영역
        logArea = new JTextArea(6, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        logArea.setBackground(new Color(52, 73, 94));
        logArea.setForeground(new Color(236, 240, 241));
        logArea.setCaretColor(Color.WHITE);
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);

        panel.add(logTitleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 로그 메시지 추가
     */
    public void addLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * 로그 초기화
     */
    public void clearLog() {
        logArea.setText("");
    }

    /**
     * 로그 영역 반환 (직접 접근용)
     */
    public JTextArea getLogArea() {
        return logArea;
    }
}
