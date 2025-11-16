package com.marblegame.network.client;

import com.marblegame.network.protocol.Message;
import com.marblegame.network.protocol.MessageSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * 서버 메시지 수신 리스너
 * 별도 스레드에서 서버로부터 메시지를 수신
 */
public class ServerListener implements Runnable {
    private final BufferedReader in;
    private final GameClient client;
    private volatile boolean running;

    /**
     * ServerListener 생성자
     * @param in 입력 스트림
     * @param client 게임 클라이언트
     */
    public ServerListener(BufferedReader in, GameClient client) {
        this.in = in;
        this.client = client;
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("서버 리스너 시작");

        try {
            String line;
            while (running) {
                try {
                    line = in.readLine();
                    if (line == null) {
                        // 서버가 연결을 종료함
                        break;
                    }
                    handleMessage(line);
                } catch (SocketTimeoutException e) {
                    // 타임아웃은 정상 - 계속 대기
                    continue;
                } catch (SocketException e) {
                    // 소켓 종료
                    if (running) {
                        System.out.println("서버 연결 종료됨");
                    }
                    break;
                } catch (IOException e) {
                    // 기타 IO 오류
                    if (running) {
                        System.err.println("서버 통신 오류: " + e.getMessage());
                    }
                    break;
                } catch (Exception e) {
                    System.err.println("메시지 처리 중 오류: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            if (running) {
                System.err.println("서버 리스너 오류: " + e.getMessage());
            }
        } finally {
            if (running) {
                running = false;
                client.onDisconnected();
            }
        }

        System.out.println("서버 리스너 종료");
    }

    /**
     * 메시지 처리
     * @param line 수신된 메시지 (JSON 문자열)
     */
    private void handleMessage(String line) {
        try {
            Message message = MessageSerializer.deserialize(line);

            // GameClient에 메시지 전달
            client.onMessageReceived(message);

        } catch (IllegalArgumentException e) {
            System.err.println("잘못된 메시지 형식: " + e.getMessage());
        }
    }

    /**
     * 리스너 중지
     */
    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
