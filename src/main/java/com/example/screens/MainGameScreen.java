package com.example.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

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
    @SuppressWarnings("unused")
    private float respawnCooldown = 0;
    private static final float SHOOT_DELAY = 0.5f;
    @SuppressWarnings("unused")
    private static final float RESPAWN_DELAY = 3.0f;
    private static final int BULLET_DAMAGE = 20;
    private boolean isConnected = false;
    private float connectionTimeout = 5.0f;

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
        client.registerHandler("JOIN", parts -> {
            if (parts.length < 4) return;
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                if (localPlayer == null && !isConnected) {
                    localPlayerId = playerId;
                    localPlayer = new Player(playerId, x, y, true);
                    players.put(playerId, localPlayer);
                    isConnected = true;
                    gameScreen.dispose();
                    gameScreen = new GameScreen(game, playerId, client);
                } else if (!playerId.equals(localPlayerId)) {
                    if (!players.containsKey(playerId)) {
                        Player newPlayer = new Player(playerId, x, y, false);
                        players.put(playerId, newPlayer);
                    }
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

        client.registerHandler("POS", parts -> {
            if (parts.length < 4) return;
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            if (!playerId.equals(localPlayerId)) {
                Gdx.app.postRunnable(() -> {
                    Player player = players.get(playerId);
                    if (player != null && !player.isLocal()) {
                        player.updateNetworkPosition(x, y);
                    }
                });
            }
        });

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
        if (localPlayer != null && !localPlayer.isDead()) {
            float moveX = 0, moveY = 0;
            if (Gdx.input.isKeyPressed(Keys.A)) moveX -= 1;
            if (Gdx.input.isKeyPressed(Keys.D)) moveX += 1;
            if (Gdx.input.isKeyPressed(Keys.W)) moveY += 1;
            if (Gdx.input.isKeyPressed(Keys.S)) moveY -= 1;

            if (moveX != 0 || moveY != 0) {
                float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
                moveX /= length;
                moveY /= length;
                
                localPlayer.update(delta, moveX, moveY);
                
                try {
                    client.sendPosition(localPlayer.id, localPlayer.position.x, localPlayer.position.y);
                } catch (IOException e) {
                    // Silent fail
                }
            }

            if (Gdx.input.isKeyPressed(Keys.SPACE) && shootCooldown <= 0) {
                shoot();
            }
        }

        for (Player player : players.values()) {
            if (player != null && !player.isLocal()) {
                player.update(delta, 0, 0);
            }
        }
        
        updateBullets(delta);
        updateRespawnTimer(delta);
        checkCollisions();

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
            // Silent fail
        }
    }

    private void renderGameElements() {
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
                    
                    if (player == localPlayer) {
                        if (player.getHealth() <= 0) {
                            try {
                                client.sendDeath();
                            } catch (IOException e) {
                                // Silent fail
                            }
                        } else {
                            try {
                                client.sendDamage(String.valueOf(bullet.shooterId), BULLET_DAMAGE);
                            } catch (IOException e) {
                                // Silent fail
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateRespawnTimer(float delta) {
        if (localPlayer != null && localPlayer.isDead()) {
            // Server handles respawn
        }
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