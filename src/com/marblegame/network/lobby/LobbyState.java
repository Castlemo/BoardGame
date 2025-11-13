package com.marblegame.network.lobby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 호스트 측 로비 상태를 관리한다.
 */
public class LobbyState {
    private final List<Slot> slots = new ArrayList<>();
    private final Map<String, Slot> clientToSlot = new HashMap<>();
    private final Set<String> connectedClients = new HashSet<>();

    public LobbyState(List<String> defaultLabels) {
        if (defaultLabels == null) {
            throw new IllegalArgumentException("default labels required");
        }
        for (int i = 0; i < defaultLabels.size(); i++) {
            Slot slot = new Slot();
            slot.index = i;
            slot.label = defaultLabels.get(i);
            slots.add(slot);
        }
    }

    public synchronized void onClientConnected(String clientId) {
        if (clientId != null) {
            connectedClients.add(clientId);
        }
    }

    public synchronized void onClientDisconnected(String clientId) {
        if (clientId == null) {
            return;
        }
        connectedClients.remove(clientId);
        releaseSlot(clientId);
    }

    public synchronized boolean assignSlot(String clientId, int slotIndex, String playerName) {
        if (clientId == null) {
            return false;
        }
        Slot target = findSlot(slotIndex);
        if (target == null) {
            return false;
        }
        if (target.assignedClientId != null && !clientId.equals(target.assignedClientId)) {
            return false;
        }
        Slot current = clientToSlot.get(clientId);
        if (current != null && current.index != slotIndex) {
            clearSlot(current);
        }
        target.assignedClientId = clientId;
        target.playerName = normalizeName(target, playerName);
        target.ready = false;
        clientToSlot.put(clientId, target);
        return true;
    }

    public synchronized boolean releaseSlot(String clientId) {
        if (clientId == null) {
            return false;
        }
        Slot slot = clientToSlot.remove(clientId);
        if (slot == null) {
            return false;
        }
        clearSlot(slot);
        return true;
    }

    public synchronized boolean releaseSlot(int slotIndex) {
        Slot slot = findSlot(slotIndex);
        if (slot == null || slot.assignedClientId == null) {
            return false;
        }
        clientToSlot.remove(slot.assignedClientId);
        clearSlot(slot);
        return true;
    }

    public synchronized Integer getSlotIndex(String clientId) {
        Slot slot = clientToSlot.get(clientId);
        return slot == null ? null : slot.index;
    }

    public synchronized boolean updateReady(String clientId, boolean ready) {
        Slot slot = clientToSlot.get(clientId);
        if (slot == null) {
            return false;
        }
        if (slot.ready == ready) {
            return false;
        }
        slot.ready = ready;
        return true;
    }

    public synchronized LobbyStateView toView(boolean gameInProgress) {
        LobbyStateView view = new LobbyStateView();
        view.maxPlayers = slots.size();
        view.gameInProgress = gameInProgress;
        int spectators = connectedClients.size() - clientToSlot.size();
        view.spectatorCount = Math.max(0, spectators);
        for (Slot slot : slots) {
            LobbySlotView slotView = new LobbySlotView();
            slotView.index = slot.index;
            slotView.label = slot.label;
            slotView.occupied = slot.assignedClientId != null;
            slotView.ready = slot.ready;
            slotView.occupantName = slotView.occupied ? slot.playerName : "";
            view.slots.add(slotView);
        }
        return view;
    }

    public synchronized boolean areAllAssignedReady() {
        for (Slot slot : slots) {
            if (slot.assignedClientId != null && !slot.ready) {
                return false;
            }
        }
        return true;
    }

    public synchronized String getClientIdForSlot(int slotIndex) {
        Slot slot = findSlot(slotIndex);
        return slot == null ? null : slot.assignedClientId;
    }

    public synchronized int getMaxPlayers() {
        return slots.size();
    }

    public synchronized String getEffectivePlayerName(int slotIndex) {
        Slot slot = findSlot(slotIndex);
        if (slot == null) {
            return "";
        }
        if (slot.playerName != null && !slot.playerName.isEmpty()) {
            return slot.playerName;
        }
        return slot.label;
    }

    private void clearSlot(Slot slot) {
        slot.assignedClientId = null;
        slot.playerName = null;
        slot.ready = false;
    }

    private Slot findSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return null;
        }
        return slots.get(slotIndex);
    }

    private String normalizeName(Slot slot, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return slot.label;
        }
        String trimmed = playerName.trim();
        return trimmed.length() > 16 ? trimmed.substring(0, 16) : trimmed;
    }

    private static final class Slot {
        int index;
        String label;
        String assignedClientId;
        String playerName;
        boolean ready;
    }
}
