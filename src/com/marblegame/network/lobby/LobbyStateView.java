package com.marblegame.network.lobby;

import java.util.ArrayList;
import java.util.List;

public class LobbyStateView {
    public int maxPlayers;
    public int spectatorCount;
    public boolean gameInProgress;
    public final List<LobbySlotView> slots = new ArrayList<>();
}
