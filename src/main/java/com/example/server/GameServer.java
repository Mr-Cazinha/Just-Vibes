package com.example.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;
import java.util.Random;
import org.json.JSONObject;

public class GameServer {
    private static final int PORT = 7777;
    private static final int BUFFER_SIZE = 1024;
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    @SuppressWarnings("unused")
    private static final float MIN_RESPAWN_TIME = 20.0f;
    @SuppressWarnings("unused")
    private static final float MAX_RESPAWN_TIME = 30.0f;
    private final DatagramSocket socket;
    private final byte[] receiveBuffer = new byte[BUFFER_SIZE];
    private boolean running = true;
    private final Random random = new Random();
    private final RespawnManager respawnManager;
    private final ScoreManager scoreManager;

    // Store connected players: Key = PlayerID, Value = Player Data
    private final Map<String, PlayerData> players = new ConcurrentHashMap<>();
    // Map to store client address to player ID mapping
    private final Map<String, String> clientToPlayerId = new ConcurrentHashMap<>();

    public GameServer() throws IOException {
        socket = new DatagramSocket(PORT);
        System.out.println("Server started on port " + PORT);
        
        // Initialize RespawnManager with callback
        respawnManager = new RespawnManager(respawnData -> {
            PlayerData player = players.get(respawnData.playerId);
            if (player != null) {
                float[] spawnPoint = getRandomSpawnPoint();
                player.x = spawnPoint[0];
                player.y = spawnPoint[1];
                broadcastRespawn(respawnData.playerId, spawnPoint[0], spawnPoint[1]);
            }
        });
        this.scoreManager = new ScoreManager();
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
        respawnManager.start();
    }

    private void serverLoop() {
        @SuppressWarnings("unused")
        long lastUpdateTime = System.nanoTime();
        
        while (running) {
            try {
                // Handle incoming packets
                while (socket.getReceiveBufferSize() > 0) {
                    DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(packet);
                    handlePacket(packet);
                }
                
                // Small sleep to prevent CPU overuse
                Thread.sleep(16); // Roughly 60 updates per second
            } catch (IOException | InterruptedException e) {
                if (running) {
                    // Silent fail
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
        } else if (parts[0].equals("DEATH")) {
            handleDeath(clientKey);
        } else if (parts[0].equals("DISCONNECT")) {
            handleDisconnect(clientKey);
        }
    }

    private String generatePlayerId(String clientKey) {
        // Generate a random number between 1000 and 9999
        int randomNum = 1000 + random.nextInt(9000);
        return clientKey + "_" + randomNum;
    }

    private void handleJoin(String clientKey, InetAddress address, int port) {
        float[] spawnPoint = getRandomSpawnPoint();
        String playerId = generatePlayerId(clientKey);
        
        PlayerData newPlayer = new PlayerData(address, port, spawnPoint[0], spawnPoint[1]);
        players.put(playerId, newPlayer);
        clientToPlayerId.put(clientKey, playerId);
        
        // Send the new player their ID and spawn position
        String initialMessage = "JOIN|" + playerId + "|" + spawnPoint[0] + "|" + spawnPoint[1];
        sendToClient(initialMessage, address, port);
        
        // Notify new player about existing players
        for (Map.Entry<String, PlayerData> entry : players.entrySet()) {
            String existingPlayerId = entry.getKey();
            if (!existingPlayerId.equals(playerId)) {
                PlayerData existingPlayer = entry.getValue();
                String joinMessage = "JOIN|" + existingPlayerId + "|" + existingPlayer.x + "|" + existingPlayer.y;
                sendToClient(joinMessage, address, port);
            }
        }
        
        // Broadcast new player to all others
        broadcastPlayerJoined(playerId);
        scoreManager.addPlayer(playerId);
        broadcastScores();
    }

    private void sendToClient(String message, InetAddress address, int port) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            // Silent fail
        }
    }

    private void handlePosition(String clientKey, String[] parts) {
        if (parts.length < 4) return;
        String playerId = parts[1];
        float x = Float.parseFloat(parts[2]);
        float y = Float.parseFloat(parts[3]);
        
        String senderPlayerId = clientToPlayerId.get(clientKey);
        if (senderPlayerId != null && senderPlayerId.equals(playerId)) {
            PlayerData player = players.get(playerId);
            if (player != null) {
                player.x = x;
                player.y = y;
                // Broadcast to all players except the sender
                broadcastPlayerPosition(playerId, x, y, clientKey);
            }
        }
    }

    private void handleShoot(String clientKey, String[] parts) {
        if (parts.length >= 3) {
            String playerId = clientToPlayerId.get(clientKey);
            if (playerId != null) {
                PlayerData player = players.get(playerId);
                if (player != null) {
                    float dirX = Float.parseFloat(parts[1]);
                    float dirY = Float.parseFloat(parts[2]);
                    broadcastShot(playerId, player.x, player.y, dirX, dirY);
                }
            }
        }
    }

    private void handleDeath(String clientKey) {
        System.out.println("Handling death for client: " + clientKey);
        String playerId = clientToPlayerId.get(clientKey);
        if (playerId != null) {
            PlayerData player = players.get(playerId);
            if (player != null) {
                // Add player to respawn queue
                respawnManager.addToRespawnQueue(playerId);
                // Broadcast death to all clients
                broadcastDeath(playerId);
                scoreManager.addKill(playerId);
                broadcastScores();
                
                if (scoreManager.hasWinner()) {
                    broadcastGameOver(scoreManager.getWinner());
                    // Reset after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000);
                            scoreManager.reset();
                            broadcastScores();
                        } catch (InterruptedException e) {
                            // Silent fail
                        }
                    }).start();
                }
            }
        }
    }

    private void handleDisconnect(String clientKey) {
        String playerId = clientToPlayerId.get(clientKey);
        if (playerId != null) {
            // Remove from all data structures
            players.remove(playerId);
            clientToPlayerId.remove(clientKey);
            
            // Broadcast player disconnection to all other clients
            broadcastPlayerDisconnected(playerId);
            scoreManager.removePlayer(playerId);
            broadcastScores();
        }
    }

    private void broadcastPlayerJoined(String playerId) {
        PlayerData player = players.get(playerId);
        String message = "JOIN|" + playerId + "|" + player.x + "|" + player.y;
        broadcast(message, null);
    }

    private void broadcastPlayerPosition(String playerId, float x, float y, String excludeKey) {
        String message = "POS|" + playerId + "|" + x + "|" + y;
        broadcast(message, excludeKey);
    }

    private void broadcastShot(String playerId, float x, float y, float dirX, float dirY) {
        String message = "SHOOT|" + playerId + "|" + x + "|" + y + "|" + dirX + "|" + dirY;
        broadcast(message, null);
    }

    private void broadcastDeath(String playerId) {
        @SuppressWarnings("unused")
        PlayerData player = players.get(playerId);
        String message = "DEATH|" + playerId;
        broadcast(message, null);
    }

    private void broadcastPlayerDisconnected(String playerId) {
        String message = "DISCONNECT|" + playerId;
        broadcast(message, null);
    }

    private void broadcastRespawn(String playerId, float x, float y) {
        String message = "RESPAWN|" + playerId + "|" + x + "|" + y;
        broadcast(message, null);
    }

    private void broadcastScores() {
        JSONObject message = new JSONObject();
        message.put("type", "scores");
        message.put("scores", new JSONObject(scoreManager.getScores()));
        message.put("winner", scoreManager.getWinner());
        broadcast(message.toString(), null);
    }

    private void broadcastGameOver(String winnerId) {
        JSONObject message = new JSONObject();
        message.put("type", "gameOver");
        message.put("winner", winnerId);
        broadcast(message.toString(), null);
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
                    // Silent fail
                }
            }
        });
    }

    public void stop() {
        if (running) {
            System.out.println("Server shutting down...");
            // Send shutdown message to all clients
            try {
                String message = "SHUTDOWN";
                broadcast(message, null);
                // Give clients a small window to receive the shutdown message
                Thread.sleep(100);
            } catch (Exception e) {
                // Silent fail
            }
            running = false;
            respawnManager.stop();
            socket.close();
            System.out.println("Server stopped.");
        }
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.start();
        } catch (IOException e) {
            // Silent fail
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