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

    public MainGameScreen(MyGame game, String serverIp) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        this.shapeRenderer = new ShapeRenderer();
        this.cityBackground = new CityBackground(800, 600);

        try {
            this.client = new GameClient(serverIp);
            setupNetworkHandlers();
            client.sendJoin();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize network client", e);
        }
    }

    private void setupNetworkHandlers() {
        client.registerHandler("JOIN", parts -> {
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                Player player = new Player(playerId, x, y);
                players.put(playerId, player);
                
                // If this is our first JOIN message, set up the local player
                if (localPlayer == null) {
                    localPlayerId = playerId;
                    localPlayer = player;
                    localPlayer.color = com.badlogic.gdx.graphics.Color.GREEN; // Distinguish local player
                }
            });
        });

        client.registerHandler("POS", parts -> {
            String playerId = parts[1];
            float x = Float.parseFloat(parts[2]);
            float y = Float.parseFloat(parts[3]);
            
            // Only update position for other players
            if (!playerId.equals(localPlayerId)) {
                Gdx.app.postRunnable(() -> {
                    Player player = players.get(playerId);
                    if (player != null) {
                        player.position.set(x, y);
                    }
                });
            }
        });

        client.registerHandler("SHOOT", parts -> {
            String playerId = parts[1];
            float dirX = Float.parseFloat(parts[2]);
            float dirY = Float.parseFloat(parts[3]);
            
            Gdx.app.postRunnable(() -> {
                Player shooter = players.get(playerId);
                if (shooter != null) {
                    bullets.add(new Bullet(playerId, 
                        shooter.position.x, 
                        shooter.position.y,
                        dirX, dirY));
                }
            });
        });
    }

    @Override
    public void render(float delta) {
        // Update
        handleInput(delta);
        updateBullets(delta);
        updateRespawnTimer(delta);
        checkCollisions();
        
        // Clear screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Render background
        shapeRenderer.begin(ShapeType.Filled);
        cityBackground.render(shapeRenderer);
        shapeRenderer.end();

        // Render players
        shapeRenderer.begin(ShapeType.Filled);
        for (Player player : players.values()) {
            player.render(shapeRenderer);
        }
        shapeRenderer.end();

        // Render bullets
        shapeRenderer.begin(ShapeType.Filled);
        for (Bullet bullet : bullets) {
            bullet.render(shapeRenderer);
        }
        shapeRenderer.end();

        // Update shoot cooldown
        if (shootCooldown > 0) {
            shootCooldown -= delta;
        }
    }

    private void checkCollisions() {
        for (Bullet bullet : bullets) {
            for (Player player : players.values()) {
                if (player.checkBulletCollision(bullet)) {
                    player.takeDamage(BULLET_DAMAGE);
                    bullet.active = false;
                    
                    // If this is our player, notify server about damage
                    if (player == localPlayer) {
                        try {
                            client.sendDamage(bullet.shooterId, BULLET_DAMAGE);
                        } catch (IOException e) {
                            Gdx.app.error("MainGameScreen", "Failed to send damage", e);
                        }
                    }
                }
            }
        }
    }

    private void updateRespawnTimer(float delta) {
        if (localPlayer != null && localPlayer.isDead()) {
            respawnCooldown -= delta;
            if (respawnCooldown <= 0) {
                respawnPlayer();
            }
        }
    }

    private void respawnPlayer() {
        if (localPlayer == null || !localPlayer.isDead()) return;
        
        try {
            // Request respawn from server
            client.sendRespawn();
            respawnCooldown = RESPAWN_DELAY;
        } catch (IOException e) {
            Gdx.app.error("MainGameScreen", "Failed to send respawn request", e);
        }
    }

    private void handleInput(float delta) {
        if (localPlayer == null || localPlayer.isDead()) return;

        // Movement
        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Keys.D)) moveX += 1;
        if (Gdx.input.isKeyPressed(Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Keys.S)) moveY -= 1;

        // Normalize diagonal movement
        if (moveX != 0 && moveY != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= length;
            moveY /= length;
        }

        if (moveX != 0 || moveY != 0) {
            localPlayer.update(delta, moveX, moveY);
            try {
                client.sendPosition(localPlayer.position.x, localPlayer.position.y);
            } catch (IOException e) {
                Gdx.app.error("MainGameScreen", "Failed to send position", e);
            }
        }

        // Shooting
        if (Gdx.input.isKeyPressed(Keys.SPACE) && shootCooldown <= 0) {
            shoot();
        }
    }

    private void shoot() {
        if (localPlayer == null) return;
        
        try {
            client.sendShoot(localPlayer.direction.x, localPlayer.direction.y);
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
        shapeRenderer.dispose();
        client.stop();
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