package com.example.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GameClient {
    private static final int SERVER_PORT = 7777;
    private static final int BUFFER_SIZE = 1024;
    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final byte[] receiveBuffer = new byte[BUFFER_SIZE];
    private boolean running = true;
    private String clientId;

    private final Map<String, Consumer<String[]>> messageHandlers = new ConcurrentHashMap<>();

    public GameClient(String serverHost) throws IOException {
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName(serverHost);
        startReceiving();
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
                    e.printStackTrace();
                }
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        String message = new String(packet.getData(), 0, packet.getLength());
        String[] parts = message.split("\\|");
        String messageType = parts[0];

        Consumer<String[]> handler = messageHandlers.get(messageType);
        if (handler != null) {
            handler.accept(parts);
        }
    }

    public void sendJoin() throws IOException {
        send("JOIN");
    }

    public void sendPosition(float x, float y) throws IOException {
        send("POS|" + x + "|" + y);
    }

    public void sendShoot(float dirX, float dirY) throws IOException {
        send("SHOOT|" + dirX + "|" + dirY);
    }

    public void sendDamage(String shooterId, int damage) throws IOException {
        send("DAMAGE|" + shooterId + "|" + damage);
    }

    public void sendRespawn() throws IOException {
        send("RESPAWN");
    }

    private void send(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
        socket.send(packet);
    }

    public void registerHandler(String messageType, Consumer<String[]> handler) {
        messageHandlers.put(messageType, handler);
    }

    public void stop() {
        running = false;
        socket.close();
    }
} 