package com.marblegame.network.listener;

import com.marblegame.network.message.NetworkMessage;

public interface ClientMessageListener {
    void onMessage(String clientId, NetworkMessage message);
}
