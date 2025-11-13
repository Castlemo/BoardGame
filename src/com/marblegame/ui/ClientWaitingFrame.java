package com.marblegame.ui;

import com.marblegame.network.ClientNetworkService;
import com.marblegame.core.input.PlayerInputEvent;
import com.marblegame.core.input.PlayerInputType;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import com.marblegame.network.message.RemoteActionCodec;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

/**
 * 클라이언트가 연결된 뒤 실제 게임 UI가 준비될 때까지 보여주는 임시 화면.
 */
public class ClientWaitingFrame extends JFrame {
    private final JTextArea logArea;

    public ClientWaitingFrame(ClientNetworkService service, Runnable onDisconnect) {
        setTitle("클라이언트 대기");
        setSize(360, 160);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onDisconnect.run();
            }
        });

        JPanel content = new JPanel(new BorderLayout(10, 10));
        JLabel label = new JLabel("호스트에 연결되었습니다. 게임 시작을 기다리는 중...");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 16f));
        content.add(label, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        content.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton disconnectButton = new JButton("연결 종료");
        disconnectButton.addActionListener(e -> onDisconnect.run());
        JPanel buttonPanel = new JPanel(new BorderLayout(10, 0));
        buttonPanel.add(disconnectButton, BorderLayout.EAST);
        buttonPanel.add(buildActionPanel(service), BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(content);

        service.setMessageListener(message ->
            SwingUtilities.invokeLater(() -> {
                if (message.getType() == MessageType.LOG_ENTRY) {
                    appendLog(message.getPayload());
                }
            })
        );
    }

    public void appendLog(String log) {
        logArea.append(log + "\n");
    }

    private JPanel buildActionPanel(ClientNetworkService service) {
        JPanel panel = new JPanel(new GridLayout(2, 4, 5, 5));
        panel.add(createActionButton("굴리기", () -> {
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.GAUGE_PRESS)));
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.GAUGE_RELEASE)));
        }));
        panel.add(createActionButton("매입", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.PURCHASE_CITY)))));
        panel.add(createActionButton("업그레이드", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.UPGRADE_CITY)))));
        panel.add(createActionButton("인수", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.TAKEOVER)))));
        panel.add(createActionButton("패스", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.SKIP_TURN)))));
        panel.add(createActionButton("보석금", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.PAY_BAIL)))));
        panel.add(createActionButton("홀수", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.TOGGLE_ODD_MODE)))));
        panel.add(createActionButton("짝수", () ->
            service.send(RemoteActionCodec.encode(PlayerInputEvent.of(PlayerInputType.TOGGLE_EVEN_MODE)))));

        JPanel container = new JPanel(new BorderLayout(5, 5));
        container.add(panel, BorderLayout.CENTER);

        JSpinner tileSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 31, 1));
        JButton tileBtn = new JButton("타일 선택");
        tileBtn.addActionListener(e ->
            service.send(RemoteActionCodec.encode(
                PlayerInputEvent.withInt(PlayerInputType.TILE_SELECTED, (int) tileSpinner.getValue())
            ))
        );
        JPanel tilePanel = new JPanel(new BorderLayout(5, 5));
        tilePanel.add(tileSpinner, BorderLayout.CENTER);
        tilePanel.add(tileBtn, BorderLayout.EAST);
        container.add(tilePanel, BorderLayout.SOUTH);
        return container;
    }

    private JButton createActionButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> action.run());
        return button;
    }
}
