package com.marblegame.network.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * 네트워크 메시지 기본 클래스
 */
public class Message {
    private MessageType type;
    private String playerId;
    private long timestamp;
    private Map<String, Object> data;

    /**
     * 기본 생성자 (역직렬화용)
     */
    public Message() {
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }

    /**
     * 메시지 생성자
     * @param type 메시지 타입
     */
    public Message(MessageType type) {
        this();
        this.type = type;
    }

    /**
     * 메시지 생성자 (플레이어 ID 포함)
     * @param type 메시지 타입
     * @param playerId 플레이어 ID
     */
    public Message(MessageType type, String playerId) {
        this(type);
        this.playerId = playerId;
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * 데이터 추가
     * @param key 키
     * @param value 값
     * @return this (메서드 체이닝용)
     */
    public Message addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 데이터 가져오기
     * @param key 키
     * @return 값
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * 데이터 가져오기 (타입 캐스팅)
     * @param key 키
     * @param clazz 타입
     * @param <T> 제네릭 타입
     * @return 값
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        return (T) data.get(key);
    }

    /**
     * String 데이터 가져오기
     * @param key 키
     * @return String 값
     */
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Integer 데이터 가져오기
     * @param key 키
     * @return Integer 값
     */
    public Integer getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Boolean 데이터 가져오기
     * @param key 키
     * @return Boolean 값
     */
    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", playerId='" + playerId + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}
