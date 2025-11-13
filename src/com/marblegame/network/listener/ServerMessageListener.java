package com.marblegame.network.listener;

import com.marblegame.network.message.NetworkMessage;

public interface ServerMessageListener {
    void onMessage(NetworkMessage message);
}
