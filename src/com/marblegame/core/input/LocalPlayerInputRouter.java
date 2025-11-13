package com.marblegame.core.input;

import com.marblegame.ui.GameFrame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * 로컬 호스트 모드에서 UI 컴포넌트 이벤트를 PlayerInputEvent로 변환하는 어댑터.
 */
public class LocalPlayerInputRouter {
    private final GameFrame frame;
    private final PlayerInputSink sink;

    public LocalPlayerInputRouter(GameFrame frame, PlayerInputSink sink) {
        this.frame = frame;
        this.sink = sink;
        attachListeners();
    }

    private void attachListeners() {
        setupDiceButton();

        frame.getActionPanel().setPurchaseListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.PURCHASE_CITY)));

        frame.getActionPanel().setUpgradeListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.UPGRADE_CITY)));

        frame.getActionPanel().setTakeoverListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.TAKEOVER)));

        frame.getActionPanel().setSkipListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.SKIP_TURN)));

        frame.getActionPanel().setEscapeListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.PAY_BAIL)));

        frame.getOverlayPanel().getOddButton().addActionListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.TOGGLE_ODD_MODE)));

        frame.getOverlayPanel().getEvenButton().addActionListener(e ->
            sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.TOGGLE_EVEN_MODE)));

        frame.getBoardPanel().setTileClickListener(tileIndex ->
            sink.handlePlayerInput(PlayerInputEvent.withInt(PlayerInputType.TILE_SELECTED, tileIndex)));
    }

    private void setupDiceButton() {
        JButton diceButton = frame.getActionPanel().getRollDiceButton();
        diceButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.GAUGE_PRESS));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sink.handlePlayerInput(PlayerInputEvent.of(PlayerInputType.GAUGE_RELEASE));
            }
        });
    }
}
