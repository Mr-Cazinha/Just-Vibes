package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class Player {
    public static final float SPEED = 200.0f; // pixels per second
    private static final float SIZE = 32f;
    private static final float RADIUS = SIZE / 2;
    private static final float HEALTH_BAR_HEIGHT = 5f;
    private static final float DIRECTION_INDICATOR_LENGTH = SIZE;
    
    protected final String id;
    private final Vector2 worldPosition;
    private final Vector2 screenPosition;
    private final Vector2 direction;
    private final boolean isLocal;
    private final Circle bounds;
    private boolean isDead;
    private int health;
    private static final int MAX_HEALTH = 100;
    private Vector2 targetWorldPosition;
    private static final float LERP_ALPHA = 0.1f;
    
    public Player(String id, float worldX, float worldY, boolean isLocal) {
        this.id = id;
        this.worldPosition = new Vector2(worldX, worldY);
        this.screenPosition = new Vector2(worldX, worldY); // Will be updated in updateScreenPosition
        this.direction = new Vector2(1, 0);
        this.isLocal = isLocal;
        this.isDead = false;
        this.health = MAX_HEALTH;
        this.bounds = new Circle(worldX, worldY, RADIUS);
        this.targetWorldPosition = new Vector2(worldX, worldY);
    }
    
    public void update(float delta, float moveX, float moveY) {
        if (isDead) return;
        
        if (isLocal) {
            // Local player movement
            if (moveX != 0 || moveY != 0) {
                Vector2 movement = new Vector2(moveX, moveY).nor().scl(SPEED * delta);
                worldPosition.add(movement);
                
                // Check map boundaries (including player radius to prevent partial clipping)
                worldPosition.x = Math.max(RADIUS, Math.min(800 - RADIUS, worldPosition.x));
                worldPosition.y = Math.max(RADIUS, Math.min(600 - RADIUS, worldPosition.y));
                
                // Update collision bounds
                bounds.setPosition(worldPosition);
            }
        } else {
            // Network player movement - smooth interpolation
            if (targetWorldPosition != null) {
                worldPosition.lerp(targetWorldPosition, LERP_ALPHA);
                bounds.setPosition(worldPosition);
            }
        }
    }
    
    public void updateScreenPosition(OrthographicCamera camera) {
        screenPosition.set(
            worldPosition.x - (camera.position.x - camera.viewportWidth/2),
            worldPosition.y - (camera.position.y - camera.viewportHeight/2)
        );
    }
    
    public void setWorldPosition(float x, float y) {
        worldPosition.set(x, y);
        if (targetWorldPosition != null) {
            targetWorldPosition.set(x, y);
        }
        bounds.setPosition(worldPosition);
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        if (isDead) return;
        
        if (isLocal) {
            shapeRenderer.setColor(Color.GREEN);
        } else {
            shapeRenderer.setColor(Color.RED);
        }
        shapeRenderer.circle(screenPosition.x, screenPosition.y, RADIUS);
        
        // Draw direction indicator
        Vector2 dirEnd = new Vector2(direction).scl(DIRECTION_INDICATOR_LENGTH).add(screenPosition);
        shapeRenderer.line(screenPosition, dirEnd);
        
        renderHealthBar(shapeRenderer);
    }
    
    private void renderHealthBar(ShapeRenderer shapeRenderer) {
        float healthBarWidth = SIZE;
        float healthPercentage = (float) health / MAX_HEALTH;
        float barX = screenPosition.x - SIZE/2;
        float barY = screenPosition.y + SIZE/2 + 5;
        
        // Health bar background
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, barY, healthBarWidth, HEALTH_BAR_HEIGHT);
        
        // Health bar fill
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(barX, barY, healthBarWidth * healthPercentage, HEALTH_BAR_HEIGHT);
    }
    
    public Vector2 getPosition() {
        return worldPosition;
    }
    
    public Vector2 getScreenPosition() {
        return screenPosition;
    }
    
    public boolean isLocal() {
        return isLocal;
    }
    
    public String getId() {
        return id;
    }
    
    public void setDead(boolean dead) {
        isDead = dead;
        if (dead) {
            health = 0;
        }
    }
    
    public boolean isDead() {
        return isDead;
    }
    
    public void setHealth(int health) {
        this.health = Math.min(Math.max(health, 0), MAX_HEALTH);
        if (this.health <= 0 && !isDead) {
            setDead(true);
        }
    }
    
    public int getHealth() {
        return health;
    }
    
    public void respawn(float x, float y) {
        worldPosition.set(x, y);
        targetWorldPosition.set(x, y);
        bounds.setPosition(worldPosition);
        health = MAX_HEALTH;
        isDead = false;
    }
    
    public Circle getBounds() {
        return bounds;
    }

    public Vector2 getDirection() {
        return direction;
    }

    public void setDirection(float x, float y) {
        direction.set(x, y).nor();
    }
} 