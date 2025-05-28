package com.example.server;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class RespawnManager implements Runnable {
    private static final float MIN_RESPAWN_TIME = 20.0f;
    private static final float MAX_RESPAWN_TIME = 30.0f;
    private final ConcurrentLinkedQueue<RespawnData> respawnQueue;
    private final Consumer<RespawnData> onRespawnCallback;
    private volatile boolean running = true;
    private float respawnTimer = 0;
    private final Object timerLock = new Object();
    private Thread respawnThread;

    public static class RespawnData {
        final String playerId;
        final float respawnTime;

        RespawnData(String playerId, float respawnTime) {
            this.playerId = playerId;
            this.respawnTime = respawnTime;
        }
    }

    public RespawnManager(Consumer<RespawnData> onRespawnCallback) {
        this.respawnQueue = new ConcurrentLinkedQueue<>();
        this.onRespawnCallback = onRespawnCallback;
    }

    public void start() {
        respawnThread = new Thread(this);
        respawnThread.setName("RespawnManager");
        respawnThread.start();
    }

    public void stop() {
        running = false;
        if (respawnThread != null) {
            respawnThread.interrupt();
        }
    }

    public void addToRespawnQueue(String playerId) {
        float respawnTime = MIN_RESPAWN_TIME + (float)(Math.random() * (MAX_RESPAWN_TIME - MIN_RESPAWN_TIME));
        RespawnData respawnData = new RespawnData(playerId, respawnTime);
        respawnQueue.offer(respawnData);
        System.out.println("Added player " + playerId + " to respawn queue with time: " + respawnTime);
    }

    @Override
    public void run() {
        while (running) {
            try {
                updateRespawnQueue();
                Thread.sleep(16); // Roughly 60 updates per second
            } catch (InterruptedException e) {
                if (running) {
                    System.err.println("RespawnManager interrupted: " + e.getMessage());
                }
            }
        }
    }

    private void updateRespawnQueue() {
        RespawnData nextRespawn = respawnQueue.peek();
        if (nextRespawn != null) {
            synchronized (timerLock) {
                respawnTimer += 0.016f; // ~16ms in seconds
                System.out.println("Respawn timer: " + respawnTimer + " / " + nextRespawn.respawnTime + " for player: " + nextRespawn.playerId);

                if (respawnTimer >= nextRespawn.respawnTime) {
                    // Remove from queue
                    respawnQueue.poll();
                    System.out.println("Respawning player: " + nextRespawn.playerId);
                    
                    // Notify the server to handle the respawn
                    onRespawnCallback.accept(nextRespawn);
                    
                    // Reset timer for next respawn
                    respawnTimer = 0;
                }
            }
        }
    }
} 