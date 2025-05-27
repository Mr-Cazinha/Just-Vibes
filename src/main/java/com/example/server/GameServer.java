package com.example.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;

public class GameServer {
    private static final int PORT = 7777;
    private static final int BUFFER_SIZE = 1024;
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private final DatagramSocket socket;
    private final byte[] receiveBuffer = new byte[BUFFER_SIZE];
    private boolean running = true;
    private final Random random = new Random();

    // Store connected players: Key = IP:Port, Value = Player Data
    private final Map<String, PlayerData> players = new ConcurrentHashMap<>();

    public GameServer() throws IOException {
        socket = new DatagramSocket(PORT);
        System.out.println("Server started on port " + PORT);
    }

    private float[] getRandomSpawnPoint() {
        // Define road positions
        int[] horizontalRoads = {GAME_HEIGHT/3, 2*GAME_HEIGHT/3};
        int[] verticalRoads = {GAME_WIDTH/3, 2*GAME_WIDTH/3};
        
        float x, y;
        boolean validPosition;
        
        do {
            // Generate random position
            x = random.nextFloat() * GAME_WIDTH;
            y = random.nextFloat() * GAME_HEIGHT;
            
            validPosition = true;
            
            // Check if position is on a road
            for (int roadY : horizontalRoads) {
                if (Math.abs(y - roadY) < 50) { // Road width is 100
                    validPosition = false;
                    break;
                }
            }
            
            if (validPosition) {
                for (int roadX : verticalRoads) {
                    if (Math.abs(x - roadX) < 50) {
                        validPosition = false;
                        break;
                    }
                }
            }
            
        } while (!validPosition);
        
        return new float[]{x, y};
    }

    public void start() {
        Thread serverThread = new Thread(this::serverLoop);
        serverThread.start();
    }

    private void serverLoop() {
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
        String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();

        if (parts[0].equals("JOIN")) {
            handleJoin(clientKey, packet.getAddress(), packet.getPort());
        } else if (parts[0].equals("POS")) {
            handlePosition(clientKey, parts);
        } else if (parts[0].equals("SHOOT")) {
            handleShoot(clientKey, parts);
        }
    }

    private void handleJoin(String clientKey, InetAddress address, int port) {
        float[] spawnPoint = getRandomSpawnPoint();
        PlayerData newPlayer = new PlayerData(address, port, spawnPoint[0], spawnPoint[1]);
        players.put(clientKey, newPlayer);
        
        // Notify new player about existing players
        for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
            if (!entry.getKey().equals(clientKey)) {
                PlayerData existingPlayer = entry.getValue();
                String joinMessage = "JOIN|" + entry.getKey() + "|" + existingPlayer.x + "|" + existingPlayer.y;
                sendToClient(joinMessage, address, port);
            }
        }
        
        // Broadcast new player to all
        broadcastPlayerJoined(clientKey);
    }

    private void sendToClient(String message, InetAddress address, int port) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePosition(String clientKey, String[] parts) {
        if (parts.length >= 3) {
            float x = Float.parseFloat(parts[1]);
            float y = Float.parseFloat(parts[2]);
            PlayerData player = players.get(clientKey);
            if (player != null) {
                player.x = x;
                player.y = y;
                broadcastPlayerPosition(clientKey, x, y);
            }
        }
    }

    private void handleShoot(String clientKey, String[] parts) {
        if (parts.length >= 3) {
            float dirX = Float.parseFloat(parts[1]);
            float dirY = Float.parseFloat(parts[2]);
            broadcastShot(clientKey, dirX, dirY);
        }
    }

    private void broadcastPlayerJoined(String clientKey) {
        PlayerData player = players.get(clientKey);
        String message = "JOIN|" + clientKey + "|" + player.x + "|" + player.y;
        broadcast(message, null);
    }

    private void broadcastPlayerPosition(String clientKey, float x, float y) {
        String message = "POS|" + clientKey + "|" + x + "|" + y;
        broadcast(message, null);
    }

    private void broadcastShot(String clientKey, float dirX, float dirY) {
        String message = "SHOOT|" + clientKey + "|" + dirX + "|" + dirY;
        broadcast(message, null);
    }

    private void broadcast(String message, String excludeKey) {
        byte[] buffer = message.getBytes();
        players.forEach((key, player) -> {
            if (!key.equals(excludeKey)) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, player.address, player.port);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() {
        running = false;
        socket.close();
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PlayerData {
        InetAddress address;
        int port;
        float x, y;

        PlayerData(InetAddress address, int port, float x, float y) {
            this.address = address;
            this.port = port;
            this.x = x;
            this.y = y;
        }
    }
} 