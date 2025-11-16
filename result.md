# Code Review

## Findings

1. **Stale Event Replay for Late Joiners (High)**  
   `GameUI.pushNetworkEvent` persisted `lastEventState` forever, so every snapshot reused the same payload and late joiners instantly replayed obsolete dialogs.

2. **Server Never Detects Dead Sockets (High)**  
   `NetConstants.SOCKET_TIMEOUT = 0` disabled read timeouts and there was no heartbeat, which meant the host never reclaimed slots when a client vanished silently.

3. **Toll/Tax Dialogs Rebuilt from Host Data (Medium)**  
   Modal dialogs consumed host-supplied `playerCashBefore` for every client, so balances didn’t match the already-mutated local model and even non-actors were blocked.

## Fixes Applied

1. **Event Replay Guard** – `GameStateSnapshot` gained an `eventSequence`, hosts stamp every snapshot, and clients raise their `lastHandledEventId` baseline whenever snapshots arrive without pending events. This prevents historical events from replaying for reconnecting players. (`GameUI.java`, `GameStateSnapshot.java`, `GameStateMapper.java`)
2. **Heartbeat & Timeouts** – Added bidirectional PING/PONG handling: clients auto-respond to server PINGs, and `ClientHandler` now sends heartbeats, expects timely PONGs, and closes dead sockets when they stop responding. (`GameClient.java`, `ClientHandler.java`)
3. **Scoped Toll/Tax Dialogs** – Only the paying player now sees the blocking dialog, and the cash numbers are derived from that client’s local model (others receive a log-only update). (`GameUI.java`)
