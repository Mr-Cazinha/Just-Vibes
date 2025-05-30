package com.example.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.example.game.GameScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

public class MainGameScreen implements Screen {
    private final MyGame game;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapeRenderer;
    private final GameClient client;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final CityBackground cityBackground;
    private GameScreen gameScreen;
    private Player localPlayer;
    private String localPlayerId;
    private float shootCooldown = 0;
    private float respawnCooldown = 0;
    private static final float SHOOT_DELAY = 0.5f;
    private static final float RESPAWN_DELAY = 3.0f;
    private static final int BULLET_DAMAGE = 20;
    private boolean isConnected = false;
    private float connectionTimeout = 5.0f;
    private final Map<String, String> bulletOwners = new ConcurrentHashMap<>();
    private float playerUpdateTimer = 0;
    private static final float PLAYER_UPDATE_INTERVAL = 0.1f; // 10 times per second

    public MainGameScreen(MyGame game, String serverIp) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        this.shapeRenderer = new ShapeRenderer();
        this.cityBackground = new CityBackground(800, 600);

        try {
            this.client = new GameClient(serverIp);
            this.client.setOnServerShutdown(() -> {
                Gdx.app.postRunnable(() -> game.exit());
            });
            setupNetworkHandlers();
            this.gameScreen = new GameScreen(game, "Waiting for ID...", client);
            client.sendJoin();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize network client", e);
        }
    }

    private void setupNetworkHandlers() {
        client.registerPosHandler(parts -> {
            if (parts.length < 4) return;
            String playerId = parts[1];
            float worldX = Float.parseFloat(parts[2]);
            float worldY = Float.parseFloat(parts[3]);
            
            if (!playerId.equals(localPlayerId)) {
                Gdx.app.postRunnable(() -> {
                    Player player = players.get(playerId);
                    if (player != null && !player.isLocal()) {
                        // Store world position directly, no conversion needed
                        player.setWorldPosition(worldX, worldY);
                    }
                });
            }
        });

        client.registerHandler("JOIN", parts -> {
            if (parts.length < 4) return;
            String playerId = parts[1];
            float worldX = Float.parseFloat(parts[2]);
            float worldY = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                if (localPlayer == null && !isConnected) {
                    localPlayerId = playerId;
                    localPlayer = new Player(playerId, worldX, worldY, true);
                    players.put(playerId, localPlayer);
                    isConnected = true;
                } else if (!playerId.equals(localPlayerId)) {
                    Player player = new Player(playerId, worldX, worldY, false);
                    players.put(playerId, player);
                }
            });
        });

        client.registerSyncHandler(parts -> {
            if (parts.length < 2) {
                System.out.println("[Client] Received invalid sync message: too few parts");
                return;
            }
            
            System.out.println("[Client] Received sync message with " + (parts.length - 1) + " player updates");
            System.out.println("[Client] Local player ID: " + localPlayerId);
            System.out.println("[Client] Current players before sync: " + String.join(", ", players.keySet()));
            
            Set<String> currentPlayers = new HashSet<>(players.keySet());
            
            for (int i = 1; i < parts.length; i++) {
                String[] playerData = parts[i].split(",");
                if (playerData.length < 4) {
                    System.out.println("[Client] Invalid player data at index " + i + ": " + String.join(",", playerData));
                    continue;
                }
                
                String playerId = playerData[0];
                float worldX = Float.parseFloat(playerData[1]);
                float worldY = Float.parseFloat(playerData[2]);
                boolean isDead = playerData[3].equals("1");
                
                System.out.println("[Client] Processing player in sync: " + playerId + " at (" + worldX + ", " + worldY + ") dead: " + isDead);
                
                currentPlayers.remove(playerId);
                
                Gdx.app.postRunnable(() -> {
                    Player player = players.get(playerId);
                    if (player == null) {
                        // Create new player if they don't exist
                        boolean isLocal = playerId.equals(localPlayerId);
                        player = new Player(playerId, worldX, worldY, isLocal);
                        players.put(playerId, player);
                        System.out.println("[Client] Created new player from sync: " + playerId);
                    } else {
                        // Update position for all players, including local player
                        player.setWorldPosition(worldX, worldY);
                        System.out.println("[Client] Updated " + (player.isLocal() ? "local" : "remote") + 
                                         " player from sync: " + playerId);
                    }
                    player.setDead(isDead);
                });
            }
            
            System.out.println("[Client] Players to remove: " + String.join(", ", currentPlayers));
            currentPlayers.forEach(playerId -> {
                if (!playerId.equals(localPlayerId)) {
                    System.out.println("[Client] Removing player not in sync: " + playerId);
                    Gdx.app.postRunnable(() -> players.remove(playerId));
                }
            });
            
            System.out.println("[Client] Current players after sync: " + String.join(", ", players.keySet()));
        });

        client.registerJsonHandler("FULL_STATE", jsonMessage -> {
            Gdx.app.postRunnable(() -> {
                try {
                    // Handle player states
                    JSONObject playersState = jsonMessage.getJSONObject("players");
                    playersState.keys().forEachRemaining(playerId -> {
                        if (!playerId.equals(localPlayerId)) {
                            JSONObject playerState = playersState.getJSONObject(playerId);
                            float x = (float)playerState.getDouble("x");
                            float y = (float)playerState.getDouble("y");
                            
                            Player player = players.get(playerId);
                            if (player == null) {
                                player = new Player(playerId, x, y, false);
                                players.put(playerId, player);
                            } else {
                                player.setWorldPosition(x, y);
                            }
                        }
                    });
                    
                    // Handle bullet states
                    bullets.clear(); // Clear existing bullets
                    JSONObject bulletsState = jsonMessage.getJSONObject("bullets");
                    bulletsState.keys().forEachRemaining(bulletId -> {
                        JSONObject bulletState = bulletsState.getJSONObject(bulletId);
                        String ownerId = bulletState.getString("ownerId");
                        Player owner = players.get(ownerId);
                        if (owner != null) {
                            Vector2 ownerPos = owner.getPosition();
                            Vector2 ownerDir = owner.getDirection();
                            Bullet bullet = new Bullet(ownerId, ownerPos.x, ownerPos.y, ownerDir.x, ownerDir.y);
                            bullets.add(bullet);
                            bulletOwners.put(bulletId, ownerId);
                        }
                    });
                } catch (Exception e) {
                    Gdx.app.error("MainGameScreen", "Error processing full state update", e);
                }
            });
        });

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
                        player = new Player(playerId, x, y, false);
                        players.put(playerId, player);
                    }
                    updatePlayerState(player, x, y, dirX, dirY, health, isDead);
                }
            });
        });

        client.registerHandler("SHOOT", parts -> {
            if (parts.length < 7) return;
            String playerId = parts[1];
            float worldX = Float.parseFloat(parts[2]);
            float worldY = Float.parseFloat(parts[3]);
            float dirX = Float.parseFloat(parts[4]);
            float dirY = Float.parseFloat(parts[5]);
            String bulletId = parts[6];
            
            if (!playerId.equals(localPlayerId)) {
                Gdx.app.postRunnable(() -> {
                    Player shooter = players.get(playerId);
                    if (shooter != null) {
                        shooter.setWorldPosition(worldX, worldY);
                        shooter.setDirection(dirX, dirY);
                        // Create bullet in world coordinates
                        Bullet bullet = new Bullet(playerId, worldX, worldY, dirX, dirY);
                        bullets.add(bullet);
                        bulletOwners.put(bulletId, playerId);
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
            float worldX = Float.parseFloat(parts[2]);
            float worldY = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                Player player = players.get(playerId);
                if (player != null) {
                    Vector2 screenPos = getScreenCoordinates(new Vector2(worldX, worldY));
                    player.respawn(screenPos.x, screenPos.y);
                }
            });
        });

        client.registerHandler("DISCONNECT", parts -> {
            if (parts.length < 2) return;
            String playerId = parts[1];
            
            Gdx.app.postRunnable(() -> {
                if (!playerId.equals(localPlayerId)) {
                    players.remove(playerId);
                }
            });
        });
    }

    private void updatePlayerState(Player player, float x, float y, float dirX, float dirY, int health, boolean isDead) {
        if (!player.isLocal()) {
            player.setWorldPosition(x, y);
            player.setDirection(dirX, dirY);
            player.setHealth(health);
            player.setDead(isDead);
        }
    }

    @Override
    public void render(float delta) {
        if (!isConnected) {
            connectionTimeout -= delta;
            if (connectionTimeout <= 0) {
                return;
            }
        }

        updateGameState(delta);
        
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        renderGameElements();
        gameScreen.render(delta);
    }

    private void updateGameState(float delta) {
        if (!isConnected) return;

        handleInput(delta);
        updateBullets(delta);
        updatePlayers(delta);
        
        // Periodic check for missing players
        playerUpdateTimer += delta;
        if (playerUpdateTimer >= PLAYER_UPDATE_INTERVAL) {
            playerUpdateTimer = 0;
            checkMissingPlayers();
        }
    }

    private void handleInput(float delta) {
        if (localPlayer == null || localPlayer.isDead()) return;

        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Keys.D)) moveX += 1;
        if (Gdx.input.isKeyPressed(Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Keys.S)) moveY -= 1;

        if (moveX != 0 || moveY != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= length;
            moveY /= length;
            
            // Update player in world coordinates
            Vector2 worldPos = localPlayer.getPosition();
            worldPos.x += moveX * Player.SPEED * delta;
            worldPos.y += moveY * Player.SPEED * delta;
            
            localPlayer.setWorldPosition(worldPos.x, worldPos.y);
        }

        // Shooting
        boolean shouldShoot = false;
        float dirX = 0, dirY = 0;
        
        // Mouse shooting
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && shootCooldown <= 0) {
            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            // Calculate direction from player to mouse cursor in world coordinates
            Vector2 playerPos = localPlayer.getPosition();
            Vector2 mouseWorldPos = new Vector2(
                mouseX + camera.position.x - camera.viewportWidth/2,
                mouseY + camera.position.y - camera.viewportHeight/2
            );
            
            dirX = mouseWorldPos.x - playerPos.x;
            dirY = mouseWorldPos.y - playerPos.y;
            float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            dirX /= length;
            dirY /= length;
            shouldShoot = true;
        }
        
        // Spacebar shooting
        if (Gdx.input.isKeyJustPressed(Keys.SPACE) && shootCooldown <= 0) {
            Vector2 playerDir = localPlayer.getDirection();
            dirX = playerDir.x;
            dirY = playerDir.y;
            shouldShoot = true;
        }
        
        if (shouldShoot) {
            Vector2 playerPos = localPlayer.getPosition();
            
            localPlayer.setDirection(dirX, dirY);
            
            Bullet bullet = new Bullet(localPlayerId, playerPos.x, playerPos.y, dirX, dirY);
            bullets.add(bullet);
            
            try {
                client.sendShoot(playerPos.x, playerPos.y, dirX, dirY);
            } catch (IOException e) {
                Gdx.app.error("MainGameScreen", "Failed to send shoot message", e);
            }
            
            shootCooldown = SHOOT_DELAY;
        }
        
        shootCooldown -= delta;
    }

    private Vector2 getWorldCoordinates(Vector2 screenPos) {
        return new Vector2(
            screenPos.x + camera.position.x - camera.viewportWidth/2,
            screenPos.y + camera.position.y - camera.viewportHeight/2
        );
    }

    private Vector2 getScreenCoordinates(Vector2 worldPos) {
        return new Vector2(
            worldPos.x - (camera.position.x - camera.viewportWidth/2),
            worldPos.y - (camera.position.y - camera.viewportHeight/2)
        );
    }

    private void renderGameElements() {
        // Update screen positions for all players
        for (Player player : players.values()) {
            if (player != null) {
                player.updateScreenPosition(camera);
            }
        }

        shapeRenderer.begin(ShapeType.Filled);
        cityBackground.render(shapeRenderer);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeType.Filled);
        for (Player player : players.values()) {
            if (player != null) {
                player.render(shapeRenderer);
            }
        }
        shapeRenderer.end();

        // Only render bullets if there are any
        if (!bullets.isEmpty()) {
            shapeRenderer.begin(ShapeType.Filled);
            for (Bullet bullet : bullets) {
                if (bullet != null && bullet.active) {
                    bullet.render(shapeRenderer);
                }
            }
            shapeRenderer.end();
        }
    }

    private void updateBullets(float delta) {
        if (bullets.isEmpty()) return;
        
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            if (bullet == null) {
                bulletIterator.remove();
                continue;
            }
            
            bullet.update(delta);

            // Check for bullet collisions with players
            for (Player player : players.values()) {
                if (!player.isDead() && bullet.checkCollision(player)) {
                    // Apply damage to the hit player
                    player.setHealth(player.getHealth() - BULLET_DAMAGE);
                    
                    // Send damage message to server
                    String bulletId = bullet.getOwnerId() + "_" + System.currentTimeMillis();
                    try {
                        client.sendDamage(bulletId, BULLET_DAMAGE);
                        
                        // If player died from this hit, send death message
                        if (player.getHealth() <= 0) {
                            if (player.isLocal()) {
                                client.sendDeath();
                                respawnCooldown = RESPAWN_DELAY;
                            }
                            player.setDead(true);
                        }
                    } catch (IOException e) {
                        Gdx.app.error("MainGameScreen", "Failed to send damage/death", e);
                    }
                    
                    // Remove the bullet
                    bulletIterator.remove();
                    break;
                }
            }

            // Remove bullets that are out of bounds
            if (bullet.isOutOfBounds(800, 600)) {
                bulletIterator.remove();
            }
        }
    }

    private void checkCollisions() {
        // Remove this method as collision handling is now in updateBullets
    }

    private void updatePlayers(float delta) {
        if (localPlayer != null && !localPlayer.isDead()) {
            playerUpdateTimer += delta;
            if (playerUpdateTimer >= PLAYER_UPDATE_INTERVAL) {
                playerUpdateTimer = 0;
                try {
                    Vector2 worldPos = localPlayer.getPosition();
                    client.sendPosition(localPlayer.getId(), worldPos.x, worldPos.y);
                } catch (IOException e) {
                    Gdx.app.error("MainGameScreen", "Failed to send position update", e);
                }
            }
        }

        for (Player player : players.values()) {
            if (player != null && !player.isLocal()) {
                player.update(delta, 0, 0);
            }
        }
        
        updateRespawnTimer(delta);
    }

    private void updateRespawnTimer(float delta) {
        if (localPlayer != null && localPlayer.isDead()) {
            respawnCooldown -= delta;
            if (respawnCooldown <= 0) {
                try {
                    client.sendRespawn();
                    respawnCooldown = RESPAWN_DELAY;
                } catch (IOException e) {
                    Gdx.app.error("MainGameScreen", "Failed to send respawn", e);
                }
            }
        }
    }

    private void checkMissingPlayers() {
        try {
            // Request current player list from server
            Vector2 pos = localPlayer.getPosition();
            client.sendPosition(localPlayer.getId(), pos.x, pos.y);
        } catch (IOException e) {
            Gdx.app.error("MainGameScreen", "Failed to request player update", e);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = 800;
        camera.viewportHeight = 600;
        
        if (localPlayer != null) {
            Vector2 worldPos = localPlayer.getPosition();
            localPlayer.setWorldPosition(worldPos.x, worldPos.y);
        }
        
        camera.update();
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.stop();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        gameScreen.dispose();
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