package com.marblegame.ui;

import java.awt.*;
import javax.swing.*;

/**
 * UI 테마 상수 정의
 * 모든 UI 컴포넌트에서 일관된 스타일을 유지하기 위한 중앙 집중식 상수 클래스
 */
public final class UIConstants {

    // 생성자를 private으로 하여 인스턴스화 방지
    private UIConstants() {
        throw new AssertionError("UIConstants는 인스턴스화할 수 없습니다.");
    }

    // ========== 기본 배경 색상 ==========
    /** 메인 다크 배경 (가장 어두운) */
    public static final Color BACKGROUND_DARK = new Color(32, 33, 36);

    /** 패널/카드 배경 (약간 밝은 다크) */
    public static final Color PANEL_DARK = new Color(44, 47, 51);

    /** 입력 필드 배경 */
    public static final Color INPUT_BG = new Color(33, 47, 61);

    // ========== 텍스트 색상 ==========
    /** 기본 텍스트 (밝은 흰색) */
    public static final Color TEXT_PRIMARY = new Color(232, 234, 237);

    /** 보조 텍스트 (회색) */
    public static final Color TEXT_SECONDARY = new Color(189, 195, 199);

    /** 비활성화 텍스트 */
    public static final Color TEXT_DISABLED = new Color(150, 150, 150);

    // ========== 액센트/강조 색상 ==========
    /** 메인 액센트 (파란색) */
    public static final Color ACCENT_COLOR = new Color(138, 180, 248);

    /** 하이라이트 보라색 */
    public static final Color HIGHLIGHT_PURPLE = new Color(155, 89, 182);

    // ========== 버튼 색상 ==========
    /** 확인/긍정 버튼 (녹색) */
    public static final Color BUTTON_CONFIRM = new Color(39, 174, 96);

    /** 취소/부정 버튼 (회색) */
    public static final Color BUTTON_CANCEL = new Color(127, 140, 141);

    /** 비활성화 버튼 */
    public static final Color BUTTON_DISABLED = new Color(60, 63, 65);

    /** 주사위 버튼 (파란색) */
    public static final Color BUTTON_ROLL = new Color(41, 128, 185);

    /** 매입 버튼 (녹색) */
    public static final Color BUTTON_PURCHASE = new Color(39, 174, 96);

    /** 업그레이드 버튼 (주황색) */
    public static final Color BUTTON_UPGRADE = new Color(243, 156, 18);

    /** 인수 버튼 (보라색) */
    public static final Color BUTTON_TAKEOVER = new Color(142, 68, 173);

    /** 패스 버튼 (회색) */
    public static final Color BUTTON_SKIP = new Color(127, 140, 141);

    /** 탈출 버튼 (빨간색) */
    public static final Color BUTTON_ESCAPE = new Color(192, 57, 43);

    /** 경고 버튼 (빨간색) */
    public static final Color BUTTON_WARNING = new Color(231, 76, 60);

    // ========== 상태 색상 ==========
    /** 성공/긍정 (녹색) */
    public static final Color STATUS_SUCCESS = new Color(46, 204, 113);

    /** 경고/주의 (주황색) */
    public static final Color STATUS_WARNING = new Color(243, 156, 18);

    /** 오류/위험 (빨간색) */
    public static final Color STATUS_ERROR = new Color(231, 76, 60);

    /** 정보 (파란색) */
    public static final Color STATUS_INFO = new Color(52, 152, 219);

    // ========== 테두리 색상 ==========
    /** 기본 테두리 */
    public static final Color BORDER_DEFAULT = new Color(52, 73, 94);

    /** 어두운 테두리 */
    public static final Color BORDER_DARK = new Color(30, 39, 46);

    // ========== 폰트 ==========
    /** 기본 폰트 이름 */
    public static final String FONT_NAME = "Malgun Gothic";

    /** 제목 폰트 (크고 굵게) */
    public static final Font FONT_TITLE = new Font(FONT_NAME, Font.BOLD, 24);

    /** 부제목 폰트 */
    public static final Font FONT_SUBTITLE = new Font(FONT_NAME, Font.BOLD, 20);

    /** 헤더 폰트 */
    public static final Font FONT_HEADER = new Font(FONT_NAME, Font.BOLD, 18);

    /** 본문 폰트 */
    public static final Font FONT_BODY = new Font(FONT_NAME, Font.PLAIN, 14);

    /** 본문 굵은 폰트 */
    public static final Font FONT_BODY_BOLD = new Font(FONT_NAME, Font.BOLD, 14);

    /** 작은 폰트 */
    public static final Font FONT_SMALL = new Font(FONT_NAME, Font.PLAIN, 12);

    /** 작은 굵은 폰트 */
    public static final Font FONT_SMALL_BOLD = new Font(FONT_NAME, Font.BOLD, 12);

    /** 힌트/캡션 폰트 */
    public static final Font FONT_CAPTION = new Font(FONT_NAME, Font.PLAIN, 11);

    /** 이탤릭 힌트 폰트 */
    public static final Font FONT_HINT = new Font(FONT_NAME, Font.ITALIC, 11);

    // ========== 버튼 크기 ==========
    /** 기본 버튼 크기 */
    public static final Dimension BUTTON_SIZE_DEFAULT = new Dimension(120, 40);

    /** 작은 버튼 크기 */
    public static final Dimension BUTTON_SIZE_SMALL = new Dimension(100, 35);

    /** 큰 버튼 크기 */
    public static final Dimension BUTTON_SIZE_LARGE = new Dimension(150, 45);

    // ========== 여백 ==========
    /** 기본 패딩 */
    public static final int PADDING_DEFAULT = 15;

    /** 작은 패딩 */
    public static final int PADDING_SMALL = 10;

    /** 큰 패딩 */
    public static final int PADDING_LARGE = 20;

    // ========== 유틸리티 메서드 ==========

    /**
     * 스타일이 적용된 버튼 생성
     */
    public static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setPreferredSize(BUTTON_SIZE_DEFAULT);
        button.setBackground(bgColor);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 호버 효과
        Color hoverColor = bgColor.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });

        return button;
    }

    /**
     * 비활성화 가능한 스타일 버튼 생성
     */
    public static JButton createStyledButton(String text, Color bgColor, boolean enabled) {
        JButton button = createStyledButton(text, bgColor);
        button.setEnabled(enabled);
        if (!enabled) {
            button.setBackground(BUTTON_DISABLED);
            button.setForeground(TEXT_DISABLED);
        }
        return button;
    }

    /**
     * 기본 다크 테마 패널 생성
     */
    public static JPanel createDarkPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_DARK);
        return panel;
    }

    /**
     * 테두리가 있는 다크 테마 패널 생성
     */
    public static JPanel createDarkPanelWithBorder() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL_DARK);
        panel.setBorder(javax.swing.BorderFactory.createLineBorder(BORDER_DARK, 1));
        return panel;
    }
}
