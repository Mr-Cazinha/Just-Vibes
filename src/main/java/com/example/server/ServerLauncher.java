package com.example.server;

public class ServerLauncher {
    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            server.start();
            System.out.println("Server is running. Press Ctrl+C to stop.");
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 