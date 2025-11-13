package com.marblegame.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * 클라이언트 연결 정보를 입력받는 간단한 다이얼로그.
 */
import javax.swing.JDialog;

public class ClientConnectDialog extends JDialog {
    private final JTextField hostField;
    private final JSpinner portSpinner;
    private boolean confirmed = false;

    public ClientConnectDialog(JFrame owner) {
        super(owner, "클라이언트 연결", true);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);
        setSize(320, 150);
        setLocationRelativeTo(owner);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        formPanel.add(new JLabel("호스트 주소:"));
        hostField = new JTextField("localhost");
        formPanel.add(hostField);

        formPanel.add(new JLabel("포트:"));
        portSpinner = new JSpinner(new SpinnerNumberModel(5000, 1024, 65535, 1));
        formPanel.add(portSpinner);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton connectButton = new JButton("연결");
        connectButton.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });
        JButton cancelButton = new JButton("취소");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        return (int) portSpinner.getValue();
    }
}
