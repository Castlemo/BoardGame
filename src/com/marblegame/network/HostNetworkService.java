package com.marblegame.network;

import com.marblegame.network.listener.ClientMessageListener;
import com.marblegame.network.message.MessageType;
import com.marblegame.network.message.NetworkMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 호스트 모드에서 클라이언트 연결을 수락하는 베이스 서버.
 * 현재는 연결을 받아 목록에 보관만 하며, 추후 게임 동기화용 채널로 확장된다.
 */
public class HostNetworkService {
    private static final long HEARTBEAT_INTERVAL_MS = 5000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String PROTOCOL_VERSION = "1";

    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Thread shutdownHook;
    private ClientMessageListener messageListener;
    private ClientLifecycleListener lifecycleListener;
    private ScheduledExecutorService heartbeatScheduler;

    public HostNetworkService(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        serverSocket = new ServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "HostAcceptThread");
        acceptThread.setDaemon(true);
        acceptThread.start();

        startHeartbeatBroadcast();

        shutdownHook = new Thread(this::stop, "HostNetworkServiceShutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void acceptLoop() {
        System.out.println("[Host] 서버가 포트 " + port + "에서 대기 중입니다.");
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                handler.start();
                System.out.println("[Host] 클라이언트 연결: " + socket.getRemoteSocketAddress());
            } catch (SocketException e) {
                if (running.get()) {
                    System.err.println("[Host] 소켓 예외: " + e.getMessage());
                }
            } catch (IOException e) {
                if (running.get()) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        System.out.println("[Host] 서버를 종료합니다.");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        for (ClientHandler handler : clientHandlers) {
            handler.close();
        }
        clientHandlers.clear();

        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {}
        }

        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
    }

    public List<Socket> getClientSockets() {
        return Collections.unmodifiableList(
            clientHandlers.stream()
                .map(ClientHandler::getSocket)
                .collect(Collectors.toList())
        );
    }

    public int getPort() {
        return port;
    }

    public void setMessageListener(ClientMessageListener listener) {
        this.messageListener = listener;
    }

    public void setClientLifecycleListener(ClientLifecycleListener listener) {
        this.lifecycleListener = listener;
    }

    public void broadcast(NetworkMessage message) {
        for (ClientHandler handler : clientHandlers) {
            if (handler.isHandshakeComplete()) {
                handler.send(message);
            }
        }
    }

    private void removeHandler(ClientHandler handler) {
        removeHandler(handler, null);
    }

    private void removeHandler(ClientHandler handler, String reason) {
        clientHandlers.remove(handler);
        handler.close();
        if (reason != null && lifecycleListener != null) {
            lifecycleListener.onClientDisconnected(handler.getClientId(), reason);
        }
    }

    private void notifyClientConnected(String clientId) {
        if (lifecycleListener != null) {
            lifecycleListener.onClientConnected(clientId);
        }
    }

    private void startHeartbeatBroadcast() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HostHeartbeatBroadcaster");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(
            () -> broadcast(new NetworkMessage(MessageType.HEARTBEAT, null)),
            HEARTBEAT_INTERVAL_MS,
            HEARTBEAT_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
    }

    public interface ClientLifecycleListener {
        void onClientConnected(String clientId);
        void onClientDisconnected(String clientId, String reason);
    }

    private class ClientHandler {
        private final Socket socket;
        private final PrintWriter writer;
        private final BufferedReader reader;
        private final String clientId;
        private Thread readerThread;
        private volatile long lastMessageAt = System.currentTimeMillis();
        private volatile boolean handshakeComplete = false;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.socket.setSoTimeout(READ_TIMEOUT_MS);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientId = socket.getRemoteSocketAddress().toString();
        }

        void start() {
            readerThread = new Thread(this::readLoop, "HostClientReader-" + clientId);
            readerThread.setDaemon(true);
            readerThread.start();
        }

        void readLoop() {
            String disconnectReason = "클라이언트가 연결을 종료했습니다.";
            try {
                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    String line;
                    try {
                        line = reader.readLine();
                    } catch (SocketTimeoutException timeout) {
                        if (System.currentTimeMillis() - lastMessageAt >= READ_TIMEOUT_MS) {
                            disconnectReason = "클라이언트 응답 시간 초과로 연결이 종료되었습니다.";
                            System.err.println("[Host] 클라이언트 타임아웃: " + clientId);
                            break;
                        }
                        continue;
                    }
                    if (line == null) {
                        disconnectReason = "클라이언트가 연결을 종료했습니다.";
                        break;
                    }
                    try {
                        NetworkMessage message = NetworkMessage.deserialize(line);
                        lastMessageAt = System.currentTimeMillis();
                        if (!handshakeComplete) {
                            if (message.getType() == MessageType.HELLO) {
                                if (!handleHello(message)) {
                                    disconnectReason = "프로토콜 버전이 맞지 않아 연결이 종료되었습니다.";
                                    break;
                                }
                                continue;
                            } else {
                                send(new NetworkMessage(
                                    MessageType.REJECT,
                                    "프로토콜 핸드셰이크 전에는 메시지를 보낼 수 없습니다."
                                ));
                                disconnectReason = "프로토콜 핸드셰이크 실패";
                                break;
                            }
                        }
                        if (message.getType() == MessageType.HEARTBEAT) {
                            continue;
                        }
                        if (message.getType() == MessageType.HELLO) {
                            continue;
                        }
                        if (messageListener != null) {
                            messageListener.onMessage(clientId, message);
                        }
                    } catch (IllegalArgumentException ex) {
                        System.err.println(
                            "[Host] 잘못된 메시지(" + clientId + "): " + ex.getMessage() + " raw=" + line
                        );
                    }
                }
            } catch (IOException ignored) {
                disconnectReason = "네트워크 오류로 연결이 종료되었습니다.";
            } finally {
                removeHandler(this, disconnectReason);
            }
        }

        private boolean handleHello(NetworkMessage message) {
            String version = message.getPayload();
            if (version == null || version.isEmpty()) {
                send(new NetworkMessage(MessageType.REJECT, "프로토콜 버전 정보가 필요합니다."));
                return false;
            }
            if (!PROTOCOL_VERSION.equals(version)) {
                send(new NetworkMessage(
                    MessageType.REJECT,
                    "서버 버전(" + PROTOCOL_VERSION + ")과 호환되지 않습니다."
                ));
                return false;
            }
            handshakeComplete = true;
            send(new NetworkMessage(MessageType.WELCOME, PROTOCOL_VERSION));
            notifyClientConnected(clientId);
            return true;
        }

        void send(NetworkMessage message) {
            writer.println(message.serialize());
            writer.flush();
            if (writer.checkError()) {
                System.err.println("[Host] 클라이언트로 전송 실패: " + clientId);
                removeHandler(this, "클라이언트로 메시지를 전송할 수 없어 연결이 종료되었습니다.");
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }

        Socket getSocket() {
            return socket;
        }

        String getClientId() {
            return clientId;
        }

        boolean isHandshakeComplete() {
            return handshakeComplete;
        }
    }
}
