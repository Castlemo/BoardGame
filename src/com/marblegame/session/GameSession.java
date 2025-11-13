package com.marblegame.session;

public interface GameSession {
    void start();
    default void stop() {}
}
