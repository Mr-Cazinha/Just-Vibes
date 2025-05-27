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
    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();
    private final ClientManager clientManager = new ClientManager();
    private final byte[] buffer = new byte[256];
    private boolean running;
    private static final float MAP_WIDTH = 800;
    private static final float MAP_HEIGHT = 600;

    private class ConnectionInfo {
        InetAddress address;
        int port;
        int clientId;

        ConnectionInfo(InetAddress address, int port, int clientId) {
            this.address = address;
            this.port = port;
            this.clientId = clientId;
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

                String connectionKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                handleMessage(connectionKey, parts, packet.getAddress(), packet.getPort());
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleMessage(String connectionKey, String[] parts, InetAddress address, int port) throws IOException {
        ConnectionInfo connection = connections.get(connectionKey);
        
        if (parts[0].equals("JOIN")) {
            if (connection == null) {
                float[] spawnPoint = getRandomSpawnPoint();
                int clientId = clientManager.createNewClient(spawnPoint[0], spawnPoint[1]);
                connection = new ConnectionInfo(address, port, clientId);
                connections.put(connectionKey, connection);
                
                // Send initial state to new client
                sendInitialState(clientId, spawnPoint, address, port);
                
                // Broadcast new client to all existing clients
                broadcastNewClient(clientId, spawnPoint[0], spawnPoint[1]);
            }
            return;
        }

        if (connection == null) return;
        int clientId = connection.clientId;
        ClientManager.ClientInfo client = clientManager.getClient(clientId);
        if (client == null) return;

        switch (parts[0]) {
            case "POS":
                handlePosition(clientId, parts);
                break;
            case "SHOOT":
                handleShoot(clientId, client, parts);
                break;
            case "DAMAGE":
                handleDamage(clientId, parts);
                break;
            case "RESPAWN":
                handleRespawn(clientId, client);
                break;
        }
    }

    private void sendInitialState(int clientId, float[] spawnPoint, InetAddress address, int port) throws IOException {
        // Send client their ID and spawn position
        String joinMessage = String.format("JOIN|%d|%f|%f", clientId, spawnPoint[0], spawnPoint[1]);
        send(joinMessage, address, port);
        
        // Send existing clients to new client
        for (Map.Entry<String, ConnectionInfo> entry : connections.entrySet()) {
            ClientManager.ClientInfo existingClient = clientManager.getClient(entry.getValue().clientId);
            if (existingClient != null) {
                String clientMessage = formatClientMessage(existingClient);
                send(clientMessage, address, port);
            }
        }
    }

    private String formatClientMessage(ClientManager.ClientInfo client) {
        return String.format("CLIENT|%d|%f|%f|%f|%f|%d|%b",
            client.id,
            client.position.x,
            client.position.y,
            client.direction.x,
            client.direction.y,
            client.health,
            client.isDead
        );
    }

    private void handlePosition(int clientId, String[] parts) throws IOException {
        if (parts.length < 3) return;
        float x = Float.parseFloat(parts[1]);
        float y = Float.parseFloat(parts[2]);
        clientManager.updateClientPosition(clientId, x, y);
        broadcastPosition(clientId, x, y);
    }

    private void handleShoot(int clientId, ClientManager.ClientInfo client, String[] parts) throws IOException {
        if (parts.length < 3) return;
        float dirX = Float.parseFloat(parts[1]);
        float dirY = Float.parseFloat(parts[2]);
        clientManager.updateClientDirection(clientId, dirX, dirY);
        broadcastShot(clientId, client.position.x, client.position.y, dirX, dirY);
    }

    private void handleDamage(int clientId, String[] parts) throws IOException {
        if (parts.length < 3) return;
        int shooterId = Integer.parseInt(parts[1]);
        int damage = Integer.parseInt(parts[2]);
        clientManager.setClientDead(clientId, true);
        broadcastDeath(clientId, shooterId);
    }

    private void handleRespawn(int clientId, ClientManager.ClientInfo client) throws IOException {
        if (!client.isDead) return;
        float[] spawnPoint = getRandomSpawnPoint();
        clientManager.updateClientPosition(clientId, spawnPoint[0], spawnPoint[1]);
        clientManager.setClientDead(clientId, false);
        clientManager.updateClientHealth(clientId, 100);
        broadcastRespawn(clientId, spawnPoint[0], spawnPoint[1]);
    }

    private float[] getRandomSpawnPoint() {
        return new float[]{
            MathUtils.random(100, MAP_WIDTH - 100),
            MathUtils.random(100, MAP_HEIGHT - 100)
        };
    }

    private void broadcastNewClient(int clientId, float x, float y) throws IOException {
        String message = String.format("CLIENT|%d|%f|%f|1|0|100|false", clientId, x, y);
        broadcast(message, null);
    }

    private void broadcastPosition(int clientId, float x, float y) throws IOException {
        String message = String.format("POS|%d|%f|%f", clientId, x, y);
        broadcast(message, null);
    }

    private void broadcastShot(int clientId, float x, float y, float dirX, float dirY) throws IOException {
        String message = String.format("SHOOT|%d|%f|%f|%f|%f", clientId, x, y, dirX, dirY);
        broadcast(message, null);
    }

    private void broadcastDeath(int clientId, int shooterId) throws IOException {
        String message = String.format("DEATH|%d|%d", clientId, shooterId);
        broadcast(message, null);
    }

    private void broadcastRespawn(int clientId, float x, float y) throws IOException {
        String message = String.format("RESPAWN|%d|%f|%f", clientId, x, y);
        broadcast(message, null);
    }

    private void broadcast(String message, String excludeKey) throws IOException {
        byte[] data = message.getBytes();
        for (Map.Entry<String, ConnectionInfo> entry : connections.entrySet()) {
            if (!entry.getKey().equals(excludeKey)) {
                ConnectionInfo connection = entry.getValue();
                send(message, connection.address, connection.port);
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