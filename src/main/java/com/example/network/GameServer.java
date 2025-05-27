package com.example.network;

import com.badlogic.gdx.math.MathUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private final DatagramSocket socket;
    private final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private final byte[] buffer = new byte[256];
    private boolean running;
    private static final float MAP_WIDTH = 800;
    private static final float MAP_HEIGHT = 600;

    private class ClientInfo {
        InetAddress address;
        int port;
        float x, y;
        boolean isDead;

        ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
            respawn();
        }

        void respawn() {
            this.x = MathUtils.random(100, MAP_WIDTH - 100);
            this.y = MathUtils.random(100, MAP_HEIGHT - 100);
            this.isDead = false;
        }
    }

    public GameServer(int port) throws IOException {
        socket = new DatagramSocket(port);
        running = true;
    }

    public void run() {
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split("\\|");

                String clientId = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                handleMessage(clientId, parts, packet.getAddress(), packet.getPort());
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleMessage(String clientId, String[] parts, InetAddress address, int port) throws IOException {
        if (!clients.containsKey(clientId)) {
            clients.put(clientId, new ClientInfo(address, port));
            broadcastNewPlayer(clientId);
            sendAllPlayers(clientId);
        }

        ClientInfo client = clients.get(clientId);
        
        switch (parts[0]) {
            case "POS":
                if (client.isDead) break;
                float x = Float.parseFloat(parts[1]);
                float y = Float.parseFloat(parts[2]);
                client.x = x;
                client.y = y;
                broadcastPosition(clientId, x, y);
                break;

            case "SHOOT":
                if (client.isDead) break;
                float shotX = Float.parseFloat(parts[1]);
                float shotY = Float.parseFloat(parts[2]);
                float dirX = Float.parseFloat(parts[3]);
                float dirY = Float.parseFloat(parts[4]);
                broadcastShot(clientId, shotX, shotY, dirX, dirY);
                break;

            case "DAMAGE":
                String shooterId = parts[1];
                int damage = Integer.parseInt(parts[2]);
                client.isDead = true;
                broadcastDeath(clientId, shooterId);
                break;

            case "RESPAWN":
                if (!client.isDead) break;
                client.respawn();
                broadcastRespawn(clientId, client.x, client.y);
                break;
        }
    }

    private void broadcastNewPlayer(String clientId) throws IOException {
        ClientInfo client = clients.get(clientId);
        String message = "NEW|" + clientId + "|" + client.x + "|" + client.y;
        broadcast(message, null);
    }

    private void broadcastPosition(String clientId, float x, float y) throws IOException {
        String message = "POS|" + clientId + "|" + x + "|" + y;
        broadcast(message, null);
    }

    private void broadcastShot(String clientId, float x, float y, float dirX, float dirY) throws IOException {
        String message = "SHOOT|" + clientId + "|" + x + "|" + y + "|" + dirX + "|" + dirY;
        broadcast(message, null);
    }

    private void broadcastDeath(String clientId, String shooterId) throws IOException {
        String message = "DEATH|" + clientId + "|" + shooterId;
        broadcast(message, null);
    }

    private void broadcastRespawn(String clientId, float x, float y) throws IOException {
        String message = "RESPAWN|" + clientId + "|" + x + "|" + y;
        broadcast(message, null);
    }

    private void sendAllPlayers(String newClientId) throws IOException {
        ClientInfo newClient = clients.get(newClientId);
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            if (!clientId.equals(newClientId)) {
                ClientInfo client = entry.getValue();
                String message = "NEW|" + clientId + "|" + client.x + "|" + client.y;
                send(message, newClient.address, newClient.port);
            }
        }
    }

    private void broadcast(String message, String excludeClientId) throws IOException {
        for (Map.Entry<String, ClientInfo> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            if (excludeClientId == null || !clientId.equals(excludeClientId)) {
                ClientInfo client = entry.getValue();
                send(message, client.address, client.port);
            }
        }
    }

    private void send(String message, InetAddress address, int port) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
} 