package com.marblegame.network;

import com.marblegame.network.listener.ServerMessageListener;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 클라이언트 모드에서 호스트에 접속하는 베이스 클라이언트.
 * 현재는 연결만 수립하며, 추후 메시지 송수신 기능을 추가한다.
 */
public class ClientNetworkService {
    private static final long HEARTBEAT_INTERVAL_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String PROTOCOL_VERSION = "1";

    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Thread readerThread;
    private final CopyOnWriteArrayList<ServerMessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private Runnable disconnectListener;
    private final AtomicBoolean disconnectNotified = new AtomicBoolean(true);
    private final AtomicLong lastServerMessageAt = new AtomicLong();
    private ScheduledExecutorService heartbeatExecutor;
    private volatile String disconnectReason = "호스트와의 연결이 종료되었습니다.";
    private volatile boolean handshakeComplete = false;
    private CountDownLatch handshakeLatch;

    public ClientNetworkService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        if (socket != null && socket.isConnected()) {
            return;
        }
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        disconnectNotified.set(false);
        lastServerMessageAt.set(System.currentTimeMillis());
        disconnectReason = "호스트와의 연결이 종료되었습니다.";
        handshakeLatch = new java.util.concurrent.CountDownLatch(1);
        startReader();
        sendHello();
        try {
            if (!handshakeLatch.await(READ_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                disconnectInternal("호스트와 프로토콜 핸드셰이크에 실패했습니다.");
                throw new IOException("프로토콜 핸드셰이크 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            disconnectInternal("프로토콜 핸드셰이크 중 인터럽트");
            throw new IOException("프로토콜 핸드셰이크 실패", e);
        }
        handshakeComplete = true;
        startHeartbeat();
        System.out.println("[Client] 호스트(" + host + ":" + port + ")에 연결되었습니다.");
    }

    public void disconnect() {
        disconnectInternal(null);
    }

    private void disconnectInternal(String reason) {
        updateDisconnectReason(reason);
        handshakeComplete = false;
        if (handshakeLatch != null) {
            handshakeLatch.countDown();
        }
        PrintWriter writerToClose;
        BufferedReader readerToClose;
        Socket socketToClose;
        Thread threadToJoin;

        synchronized (this) {
            boolean hasResources = socket != null || reader != null || writer != null || readerThread != null;
            if (!hasResources) {
                notifyDisconnectListener();
                return;
            }
            writerToClose = writer;
            readerToClose = reader;
            socketToClose = socket;
            threadToJoin = readerThread;
            writer = null;
            reader = null;
            socket = null;
            readerThread = null;
        }

        closeQuietly(writerToClose);
        closeQuietly(readerToClose);
        closeQuietly(socketToClose);
        stopHeartbeat();

        if (threadToJoin != null && threadToJoin != Thread.currentThread()) {
            threadToJoin.interrupt();
            try {
                threadToJoin.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        notifyDisconnectListener();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean send(NetworkMessage message) {
        PrintWriter currentWriter = writer;
        if (currentWriter == null) {
            return false;
        }
        currentWriter.println(message.serialize());
        currentWriter.flush();
        if (currentWriter.checkError()) {
            System.err.println("[Client] 메시지 전송 실패, 연결을 종료합니다.");
            disconnectInternal("호스트와 통신할 수 없어 연결이 종료되었습니다.");
            return false;
        }
        return true;
    }

    public void setMessageListener(ServerMessageListener listener) {
        messageListeners.clear();
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    public void addMessageListener(ServerMessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    public void removeMessageListener(ServerMessageListener listener) {
        if (listener != null) {
            messageListeners.remove(listener);
        }
    }

    public void setDisconnectListener(Runnable listener) {
        this.disconnectListener = listener;
    }

    public String getDisconnectReason() {
        return disconnectReason;
    }

    private void sendHello() {
        if (!send(new NetworkMessage(MessageType.HELLO, PROTOCOL_VERSION))) {
            disconnectInternal("호스트와 핸드셰이크 메시지를 교환하지 못했습니다.");
        }
    }

    private void startReader() {
        final BufferedReader readerRef = reader;
        readerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String line;
                    try {
                        line = readerRef.readLine();
                    } catch (SocketTimeoutException timeout) {
                        if (System.currentTimeMillis() - lastServerMessageAt.get() >= READ_TIMEOUT_MS) {
                            System.err.println("[Client] 서버로부터 응답이 없어 연결을 종료합니다.");
                            updateDisconnectReason("호스트 응답이 없어 연결이 종료되었습니다.");
                            break;
                        }
                        continue;
                    }
                    if (line == null) {
                        updateDisconnectReason("호스트가 연결을 종료했습니다.");
                        break;
                    }
                    try {
                        NetworkMessage message = NetworkMessage.deserialize(line);
                        lastServerMessageAt.set(System.currentTimeMillis());
                        if (handleControlMessage(message)) {
                            continue;
                        }
                        dispatchMessage(message);
                    } catch (IllegalArgumentException ex) {
                        System.err.println("[Client] 잘못된 서버 메시지: " + ex.getMessage() + " raw=" + line);
                    }
                }
            } catch (IOException ignored) {
                updateDisconnectReason("네트워크 오류로 연결이 종료되었습니다.");
            } finally {
                disconnect();
            }
        }, "ClientServerReader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void dispatchMessage(NetworkMessage message) {
        for (ServerMessageListener listener : messageListeners) {
            try {
                listener.onMessage(message);
            } catch (Exception ex) {
                System.err.println("[Client] 메시지 리스너 예외: " + ex.getMessage());
            }
        }
    }

    private void notifyDisconnectListener() {
        if (disconnectListener == null) {
            return;
        }
        if (disconnectNotified.compareAndSet(false, true)) {
            disconnectListener.run();
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ClientHeartbeatSender");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (!send(new NetworkMessage(MessageType.HEARTBEAT, null))) {
                stopHeartbeat();
            }
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
    }

    private void updateDisconnectReason(String reason) {
        if (reason != null && !reason.isEmpty()) {
            disconnectReason = reason;
        }
    }

    private boolean handleControlMessage(NetworkMessage message) {
        MessageType type = message.getType();
        if (type == MessageType.HEARTBEAT) {
            return true;
        }
        if (type == MessageType.WELCOME) {
            if (!PROTOCOL_VERSION.equals(message.getPayload())) {
                disconnectInternal("호스트와 프로토콜 버전이 호환되지 않습니다.");
                return true;
            }
            handshakeComplete = true;
            if (handshakeLatch != null) {
                handshakeLatch.countDown();
            }
            return true;
        }
        if (type == MessageType.REJECT) {
            String reason = message.getPayload();
            if (reason == null || reason.isEmpty()) {
                reason = "호스트가 연결을 거부했습니다.";
            }
            disconnectInternal(reason);
            return true;
        }
        if (!handshakeComplete) {
            disconnectInternal("프로토콜 핸드셰이크 전에 수신된 메시지로 인해 연결이 종료되었습니다.");
            return true;
        }
        return false;
    }
}
