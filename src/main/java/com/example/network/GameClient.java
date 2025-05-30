package com.example.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.json.JSONObject;
import org.json.JSONException;

public class GameClient {
    private static final int SERVER_PORT = 7777;
    private static final int BUFFER_SIZE = 1024;
    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final byte[] receiveBuffer = new byte[BUFFER_SIZE];
    private boolean running = true;
    private Runnable onServerShutdown;

    private final Map<String, Consumer<String[]>> messageHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JSONObject>> jsonMessageHandlers = new ConcurrentHashMap<>();

    public GameClient(String serverHost) throws IOException {
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName(serverHost);
        setupDefaultHandlers();
        startReceiving();
    }

    private void setupDefaultHandlers() {
        registerHandler("SHUTDOWN", parts -> {
            if (running) {
                if (onServerShutdown != null) {
                    onServerShutdown.run();
                }
                stop();
            }
        });
    }

    public void setOnServerShutdown(Runnable callback) {
        this.onServerShutdown = callback;
    }

    public void registerSyncHandler(Consumer<String[]> handler) {
        registerHandler("SYNC", handler);
    }

    public void registerPosHandler(Consumer<String[]> handler) {
        registerHandler("POS", handler);
    }

    private void startReceiving() {
        Thread receiveThread = new Thread(this::receiveLoop);
        receiveThread.start();
    }

    private void receiveLoop() {
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(packet);
                handlePacket(packet);
            } catch (IOException e) {
                if (running) {
                    // Silent fail
                }
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        String message = new String(packet.getData(), 0, packet.getLength());
        System.out.println("[Client] Received message: " + message);
        
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");
            System.out.println("[Client] Handling JSON message of type: " + type);
            Consumer<JSONObject> handler = jsonMessageHandlers.get(type);
            if (handler != null) {
                handler.accept(json);
                return;
            }
        } catch (JSONException e) {
            String[] parts = message.split("\\|");
            String messageType = parts[0];
            System.out.println("[Client] Handling message of type: " + messageType);

            Consumer<String[]> handler = messageHandlers.get(messageType);
            if (handler != null) {
                handler.accept(parts);
            }
        }
    }

    public void sendJoin() throws IOException {
        send("JOIN");
    }

    public void sendPosition(String playerId, float x, float y) throws IOException {
        send("POS|" + playerId + "|" + x + "|" + y);
    }

    public void sendShoot(float x, float y, float dirX, float dirY) throws IOException {
        send("SHOOT|" + dirX + "|" + dirY);
    }

    public void sendDeath() throws IOException {
        send("DEATH");
    }

    public void sendDamage(String shooterId, int damage) throws IOException {
        send("DAMAGE|" + shooterId + "|" + damage);
    }

    public void sendRespawn() throws IOException {
        send("RESPAWN");
    }

    public void sendDisconnect() throws IOException {
        send("DISCONNECT");
    }

    private void send(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
        socket.send(packet);
    }

    public void registerHandler(String messageType, Consumer<String[]> handler) {
        messageHandlers.put(messageType, handler);
    }

    public void registerJsonHandler(String messageType, Consumer<JSONObject> handler) {
        jsonMessageHandlers.put(messageType, handler);
    }

    public void stop() {
        try {
            if (running) {
                sendDisconnect();
            }
        } catch (IOException e) {
            // Silent fail
        }
        running = false;
        socket.close();
    }
} 