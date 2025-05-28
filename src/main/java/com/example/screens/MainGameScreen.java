package com.example.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.example.MyGame;
import com.example.entities.Player;
import com.example.entities.Bullet;
import com.example.network.GameClient;
import com.example.map.CityBackground;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainGameScreen implements Screen {
    private final MyGame game;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final GameClient client;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final CityBackground cityBackground;
    private Player localPlayer;
    private String localPlayerId;
    private float shootCooldown = 0;
    private float respawnCooldown = 0;
    private static final float SHOOT_DELAY = 0.5f;
    private static final float RESPAWN_DELAY = 3.0f;
    private static final int BULLET_DAMAGE = 20;
    private boolean isConnected = false;
    private float connectionTimeout = 5.0f; // 5 seconds timeout

    public MainGameScreen(MyGame game, String serverIp) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        this.shapeRenderer = new ShapeRenderer();
        this.cityBackground = new CityBackground(800, 600);

        try {
            this.client = new GameClient(serverIp);
            this.client.setOnServerShutdown(() -> {
                Gdx.app.postRunnable(() -> {
                    Gdx.app.log("MainGameScreen", "Server shutdown received, exiting game");
                    game.exit(); // Add this method to MyGame
                });
            });
            setupNetworkHandlers();
            Gdx.app.log("MainGameScreen", "Sending JOIN request to server");
            client.sendJoin();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize network client", e);
        }
    }

    private void setupNetworkHandlers() {
        // Initial connection and player setup
        client.registerHandler("JOIN", parts -> {
            if (parts.length < 4) {
                Gdx.app.error("MainGameScreen", "Invalid JOIN message format");
                return;
            }
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                // If we don't have a local player yet and we're not connected, this is our JOIN response
                if (localPlayer == null && !isConnected) {
                    Gdx.app.log("MainGameScreen", "Received initial JOIN response. Creating local player with ID: " + playerId);
                    localPlayerId = playerId;
                    localPlayer = new Player(playerId, x, y, true);
                    players.put(playerId, localPlayer);
                    isConnected = true;
                    Gdx.app.log("MainGameScreen", "Local player initialized. Position: " + x + ", " + y);
                } else if (!playerId.equals(localPlayerId)) {
                    // This is another player joining
                    Gdx.app.log("MainGameScreen", "New remote player joined with ID: " + playerId);
                    if (!players.containsKey(playerId)) {
                        Player newPlayer = new Player(playerId, x, y, false);
                        players.put(playerId, newPlayer);
                        Gdx.app.log("MainGameScreen", "Added remote player. Position: " + x + ", " + y);
                    }
                }
                Gdx.app.debug("MainGameScreen", "Total players: " + players.size() + 
                    ", Local player: " + (localPlayer != null ? localPlayer.id : "null"));
            });
        });

        // Other player updates
        client.registerHandler("CLIENT", parts -> {
            if (parts.length < 8) return;
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            float dirX = Float.parseFloat(parts[4]);
            float dirY = Float.parseFloat(parts[5]);
            int health = Integer.parseInt(parts[6]);
            boolean isDead = Boolean.parseBoolean(parts[7]);
            
            Gdx.app.postRunnable(() -> {
                if (!playerId.equals(localPlayerId)) {
                    Player player = players.get(playerId);
                    if (player == null) {
                        Gdx.app.log("MainGameScreen", "New remote player joined with ID: " + playerId);
                        player = new Player(playerId, x, y, false);
                        players.put(playerId, player);
                    }
                    updatePlayerState(player, x, y, dirX, dirY, health, isDead);
                }
            });
        });

        // Position updates
        client.registerHandler("POS", parts -> {
            if (parts.length < 4) return;
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            // Only process position updates for other players
            if (!playerId.equals(localPlayerId)) {
                Gdx.app.postRunnable(() -> {
                    Player player = players.get(playerId);
                    if (player != null && !player.isLocal()) {
                        player.updateNetworkPosition(x, y);
                    }
                });
            }
        });

        // Combat actions
        client.registerHandler("SHOOT", parts -> {
            if (parts.length < 6) return;
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            float dirX = Float.parseFloat(parts[4]);
            float dirY = Float.parseFloat(parts[5]);
            
            if (!playerId.equals(localPlayerId)) {
                Gdx.app.postRunnable(() -> {
                    Player shooter = players.get(playerId);
                    if (shooter != null) {
                        shooter.position.set(x, y);
                        shooter.direction.set(dirX, dirY).nor();
                        bullets.add(new Bullet(playerId, x, y, dirX, dirY));
                    }
                });
            }
        });

        client.registerHandler("DEATH", parts -> {
            if (parts.length < 3) return;
            String playerId = parts[1];
            
            Gdx.app.postRunnable(() -> {
                Player player = players.get(playerId);
                if (player != null) {
                    player.setDead(true);
                }
            });
        });

        client.registerHandler("RESPAWN", parts -> {
            if (parts.length < 4) return;
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                Player player = players.get(playerId);
                if (player != null) {
                    player.respawn(x, y);
                }
            });
        });

        client.registerHandler("DISCONNECT", parts -> {
            if (parts.length < 2) return;
            String playerId = parts[1];
            
            Gdx.app.postRunnable(() -> {
                if (!playerId.equals(localPlayerId)) {
                    players.remove(playerId);
                    Gdx.app.log("MainGameScreen", "Player disconnected: " + playerId);
                }
            });
        });
    }

    private void updatePlayerState(Player player, float x, float y, float dirX, float dirY, int health, boolean isDead) {
        if (!player.isLocal()) {
            player.updateNetworkPosition(x, y);
            player.direction.set(dirX, dirY);
            player.setHealth(health);
            player.setDead(isDead);
        }
    }

    @Override
    public void render(float delta) {
        // Check connection timeout
        if (!isConnected) {
            connectionTimeout -= delta;
            if (connectionTimeout <= 0) {
                Gdx.app.error("MainGameScreen", "Connection timeout. Failed to receive JOIN response");
                return;
            }
        }

        // Update game state
        updateGameState(delta);
        
        // Clear screen and prepare for rendering
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Render game elements
        renderGameElements();
    }

    private void updateGameState(float delta) {
        // Only handle input for local player
        if (localPlayer != null && !localPlayer.isDead()) {
            float moveX = 0, moveY = 0;
            if (Gdx.input.isKeyPressed(Keys.A)) moveX -= 1;
            if (Gdx.input.isKeyPressed(Keys.D)) moveX += 1;
            if (Gdx.input.isKeyPressed(Keys.W)) moveY += 1;
            if (Gdx.input.isKeyPressed(Keys.S)) moveY -= 1;

            // Normalize diagonal movement
            if (moveX != 0 || moveY != 0) {
                float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
                moveX /= length;
                moveY /= length;
                
                // Update local player position
                localPlayer.update(delta, moveX, moveY);
                
                // Send position update
                try {
                    client.sendPosition(localPlayer.id, localPlayer.position.x, localPlayer.position.y);
                } catch (IOException e) {
                    Gdx.app.error("MainGameScreen", "Failed to send position update", e);
                }
            }

            // Handle shooting
            if (Gdx.input.isKeyPressed(Keys.SPACE) && shootCooldown <= 0) {
                shoot();
            }
        }

        // Update all non-local players (network interpolation)
        for (Player player : players.values()) {
            if (player != null && !player.isLocal()) {
                player.update(delta, 0, 0); // Only update interpolation
            }
        }
        
        updateBullets(delta);
        updateRespawnTimer(delta);
        checkCollisions();

        // Update shoot cooldown
        if (shootCooldown > 0) {
            shootCooldown -= delta;
        }
    }

    private void shoot() {
        if (localPlayer == null) return;
        
        try {
            client.sendShoot(localPlayer.position.x, localPlayer.position.y, localPlayer.direction.x, localPlayer.direction.y);
            bullets.add(new Bullet(localPlayerId,
                localPlayer.position.x,
                localPlayer.position.y,
                localPlayer.direction.x,
                localPlayer.direction.y));
            shootCooldown = SHOOT_DELAY;
        } catch (IOException e) {
            Gdx.app.error("MainGameScreen", "Failed to send shoot command", e);
        }
    }

    private void renderGameElements() {
        // Render background
        shapeRenderer.begin(ShapeType.Filled);
        cityBackground.render(shapeRenderer);
        shapeRenderer.end();

        // Render players
        shapeRenderer.begin(ShapeType.Filled);
        for (Player player : players.values()) {
            if (player != null) {
                player.render(shapeRenderer);
            }
        }
        shapeRenderer.end();

        // Render bullets
        shapeRenderer.begin(ShapeType.Filled);
        for (Bullet bullet : bullets) {
            bullet.render(shapeRenderer);
        }
        shapeRenderer.end();
    }

    private void checkCollisions() {
        for (Bullet bullet : bullets) {
            for (Player player : players.values()) {
                if (player.checkBulletCollision(bullet)) {
                    player.takeDamage(BULLET_DAMAGE);
                    bullet.active = false;
                    
                    // If this is our player and they died, notify server
                    if (player == localPlayer) {
                        if (player.getHealth() <= 0) {
                            try {
                                client.sendDeath();
                            } catch (IOException e) {
                                Gdx.app.error("MainGameScreen", "Failed to send death notification", e);
                            }
                        } else {
                            try {
                                client.sendDamage(String.valueOf(bullet.shooterId), BULLET_DAMAGE);
                            } catch (IOException e) {
                                Gdx.app.error("MainGameScreen", "Failed to send damage", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateRespawnTimer(float delta) {
        // Remove local respawn timer as it's now handled by the server
        if (localPlayer != null && localPlayer.isDead()) {
            // Display "Waiting to respawn..." message or similar UI feedback
            // This could be added in the render method
        }
    }

    private void respawnPlayer() {
        // Remove local respawn handling as it's now managed by the server
    }

    private void updateBullets(float delta) {
        bullets.removeIf(bullet -> {
            bullet.update(delta);
            return bullet.isOutOfBounds(800, 600) || !bullet.active;
        });
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.stop(); // This will now send the disconnect message before stopping
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}
} 