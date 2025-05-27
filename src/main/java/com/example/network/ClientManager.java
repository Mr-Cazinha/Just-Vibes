package com.example.network;

import com.badlogic.gdx.math.Vector2;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientManager {
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, ClientInfo> clients = new ConcurrentHashMap<>();
    
    public static class ClientInfo {
        public final int id;
        public final Vector2 position;
        public final Vector2 direction;
        public boolean isDead;
        public int health;
        
        public ClientInfo(int id, float x, float y) {
            this.id = id;
            this.position = new Vector2(x, y);
            this.direction = new Vector2(1, 0);
            this.isDead = false;
            this.health = 100;
        }
    }
    
    public int createNewClient(float x, float y) {
        int id = nextId.getAndIncrement();
        clients.put(id, new ClientInfo(id, x, y));
        return id;
    }
    
    public ClientInfo getClient(int id) {
        return clients.get(id);
    }
    
    public void removeClient(int id) {
        clients.remove(id);
    }
    
    public ConcurrentHashMap<Integer, ClientInfo> getAllClients() {
        return clients;
    }
    
    public void updateClientPosition(int id, float x, float y) {
        ClientInfo client = clients.get(id);
        if (client != null) {
            client.position.set(x, y);
        }
    }
    
    public void updateClientDirection(int id, float dirX, float dirY) {
        ClientInfo client = clients.get(id);
        if (client != null) {
            client.direction.set(dirX, dirY).nor();
        }
    }
    
    public void setClientDead(int id, boolean isDead) {
        ClientInfo client = clients.get(id);
        if (client != null) {
            client.isDead = isDead;
        }
    }
    
    public void updateClientHealth(int id, int health) {
        ClientInfo client = clients.get(id);
        if (client != null) {
            client.health = health;
        }
    }
} 