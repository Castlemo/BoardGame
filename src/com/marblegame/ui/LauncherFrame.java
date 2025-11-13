package com.marblegame.ui;

import com.marblegame.session.ClientGameSession;
import com.marblegame.session.HostGameSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.NumberFormatter;

/**
 * 게임 실행 전에 호스트/클라이언트 모드를 고르는 런처 프레임.
 * 1단계에서는 호스트 모드만 실제 GameUI를 띄우고,
 * 클라이언트 버튼은 추후 네트워크 구현 시 교체한다.
 */
public class LauncherFrame extends JFrame {

    private final JSpinner playerSpinner;
    private final JSpinner portSpinner;
    private final JFormattedTextField cashField;

    public LauncherFrame() {
        setTitle("BoardGame Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setPreferredSize(new Dimension(420, 260));

        // 기본 UI 구성
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(content);

        JLabel title = new JLabel("멀티플레이 모드 선택");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        content.add(title, BorderLayout.NORTH);

        // 입력 영역
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.add(new JLabel("플레이어 수 (2-4명):"));
        playerSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 4, 1));
        formPanel.add(playerSpinner);

        formPanel.add(new JLabel("시작 자금:"));
        NumberFormatter formatter = new NumberFormatter(NumberFormat.getIntegerInstance());
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(false);
        formatter.setMinimum(100000);
        cashField = new JFormattedTextField(formatter);
        cashField.setValue(1_500_000);
        cashField.addKeyListener(new EnterKeyListener());
        formPanel.add(cashField);

        formPanel.add(new JLabel("호스트 포트:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(5000, 1024, 65535, 1));
        formPanel.add(portSpinner);

        content.add(formPanel, BorderLayout.CENTER);

        // 버튼 영역
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton hostButton = new JButton("호스트 시작");
        hostButton.setBackground(new Color(52, 152, 219));
        hostButton.setForeground(Color.WHITE);
        hostButton.addActionListener(e -> launchHost());

        JButton clientButton = new JButton("클라이언트 연결");
        clientButton.addActionListener(e -> connectAsClient());

        buttonPanel.add(hostButton);
        buttonPanel.add(clientButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void launchHost() {
        int numPlayers = (int) playerSpinner.getValue();
        try {
            cashField.commitEdit();
        } catch (ParseException ex) {
            showParseError();
            return;
        }
        int initialCash = ((Number) cashField.getValue()).intValue();
        int port = (int) portSpinner.getValue();

        HostGameSession session = new HostGameSession(numPlayers, initialCash, port);
        session.start();
        dispose();
    }

    private void connectAsClient() {
        ClientConnectDialog dialog = new ClientConnectDialog(this);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }

        ClientGameSession session = new ClientGameSession(dialog.getHost(), dialog.getPort());
        session.start();
    }

    private void showParseError() {
        JOptionPane.showMessageDialog(
            this,
            "시작 자금 입력을 확인해주세요.",
            "입력 오류",
            JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * 사용자가 시작 자금 입력란에서 바로 Enter를 눌러 호스트를 실행할 수 있도록 처리.
     */
    private class EnterKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                launchHost();
            }
        }
    }
}
