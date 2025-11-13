package com.marblegame.network.message;

public enum MessageType {
    STATE_SNAPSHOT,
    PLAYER_ACTION,
    LOG_ENTRY,
    HEARTBEAT,
    HELLO,
    WELCOME,
    REJECT,
    DIALOG_SYNC,
    LOBBY_STATE,
    SLOT_REQUEST,
    SLOT_ASSIGNMENT,
    READY_STATUS,
    DIALOG_COMMAND,
    DIALOG_RESPONSE
}
