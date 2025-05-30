package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Circle;

public class Player {
    public static final float SPEED = 200.0f; // pixels per second
    private static final float SIZE = 32f;
    private static final float RADIUS = SIZE / 2;
    private static final float HEALTH_BAR_HEIGHT = 5f;
    private static final float DIRECTION_INDICATOR_LENGTH = SIZE;
    
    protected final String id;
    private final Vector2 position;
    private final Vector2 direction;
    private final boolean isLocal;
    private final Circle bounds;
    private boolean isDead;
    private int health;
    private static final int MAX_HEALTH = 100;
    private Vector2 targetPosition;
    private Vector2 previousPosition;
    private static final float LERP_ALPHA = 0.1f;
    
    public Player(String id, float x, float y, boolean isLocal) {
        this.id = id;
        this.position = new Vector2(x, y);
        this.direction = new Vector2(1, 0);
        this.isLocal = isLocal;
        this.isDead = false;
        this.health = MAX_HEALTH;
        this.bounds = new Circle(x, y, RADIUS);
        this.targetPosition = new Vector2(x, y);
        this.previousPosition = new Vector2(x, y);
    }
    
    public void update(float delta, float moveX, float moveY) {
        if (isDead) return;
        
        if (isLocal) {
            // Local player movement
            if (moveX != 0 || moveY != 0) {
                Vector2 movement = new Vector2(moveX, moveY).nor().scl(SPEED * delta);
                position.add(movement);
                
                // Check map boundaries (including player radius to prevent partial clipping)
                position.x = Math.max(RADIUS, Math.min(800 - RADIUS, position.x));
                position.y = Math.max(RADIUS, Math.min(600 - RADIUS, position.y));
                
                // Update collision bounds
                bounds.setPosition(position);
            }
        } else {
            // Network player movement - smooth interpolation
            if (targetPosition != null) {
                position.lerp(targetPosition, LERP_ALPHA);
                bounds.setPosition(position);
            }
        }
    }
    
    public void updateNetworkPosition(float x, float y) {
        if (!isLocal) {
            if (targetPosition == null) {
                targetPosition = new Vector2(x, y);
            } else {
                targetPosition.set(x, y);
            }
            // If this is the first position update, set the position directly
            if (position.x == 0 && position.y == 0) {
                position.set(x, y);
            }
        }
    }
    
    public void setPosition(float x, float y) {
        position.set(x, y);
        if (targetPosition != null) {
            targetPosition.set(x, y);
        }
        bounds.setPosition(position);
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        if (isDead) return;
        
        if (isLocal) {
            shapeRenderer.setColor(Color.GREEN);
        } else {
            shapeRenderer.setColor(Color.RED);
        }
        shapeRenderer.circle(position.x, position.y, RADIUS);
        
        // Draw direction indicator
        Vector2 dirEnd = new Vector2(direction).scl(DIRECTION_INDICATOR_LENGTH).add(position);
        shapeRenderer.line(position, dirEnd);
        
        renderHealthBar(shapeRenderer);
    }
    
    private void renderHealthBar(ShapeRenderer shapeRenderer) {
        float healthBarWidth = SIZE;
        float healthPercentage = (float) health / MAX_HEALTH;
        float barX = position.x - SIZE/2;
        float barY = position.y + SIZE/2 + 5;
        
        // Health bar background
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, barY, healthBarWidth, HEALTH_BAR_HEIGHT);
        
        // Health bar fill
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(barX, barY, healthBarWidth * healthPercentage, HEALTH_BAR_HEIGHT);
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
        position.set(x, y);
        targetPosition.set(x, y);
        previousPosition.set(x, y);
        bounds.setPosition(position);
        health = MAX_HEALTH;
        isDead = false;
    }
    
    public Circle getBounds() {
        return bounds;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getDirection() {
        return direction;
    }

    public void setDirection(float x, float y) {
        direction.set(x, y).nor();
    }
} 