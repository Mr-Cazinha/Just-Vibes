package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Circle;

public class Player {
    private static final float SPEED = 200f;
    private static final float SIZE = 32f;
    private static final float COLLISION_RADIUS = SIZE/2;
    private static final float HEALTH_BAR_HEIGHT = 5f;
    private static final float DIRECTION_INDICATOR_LENGTH = SIZE;
    
    public final String id;
    public final Vector2 position;
    public final Vector2 direction;
    public Color color;
    private final Circle collisionBounds;
    private int health;
    private static final int MAX_HEALTH = 100;
    private boolean isDead;
    private boolean isLocal;
    @SuppressWarnings("unused")
    private float lastUpdateTime;
    private static final float NETWORK_INTERPOLATION = 0.1f;
    private Vector2 targetPosition;
    private Vector2 previousPosition;
    
    public Player(String id, float x, float y, boolean isLocal) {
        this.id = id;
        this.position = new Vector2(x, y);
        this.direction = new Vector2(1, 0);
        this.color = isLocal ? Color.GREEN : Color.BLUE;
        this.collisionBounds = new Circle(x, y, COLLISION_RADIUS);
        this.health = MAX_HEALTH;
        this.isDead = false;
        this.isLocal = isLocal;
        this.targetPosition = new Vector2(x, y);
        this.previousPosition = new Vector2(x, y);
        this.lastUpdateTime = 0;
    }
    
    public void update(float delta, float moveX, float moveY) {
        if (isDead) return;
        
        if (isLocal) {
            updateLocalPlayer(delta, moveX, moveY);
        } else {
            interpolatePosition(delta);
        }
        
        // Update collision bounds
        collisionBounds.setPosition(position.x, position.y);
    }
    
    private void updateLocalPlayer(float delta, float moveX, float moveY) {
        // Update position based on input
        position.x += moveX * SPEED * delta;
        position.y += moveY * SPEED * delta;
        
        // Update direction if moving
        if (moveX != 0 || moveY != 0) {
            direction.set(moveX, moveY).nor();
        }
    }
    
    private void interpolatePosition(float delta) {
        // Smoothly interpolate to target position
        position.x += (targetPosition.x - position.x) * NETWORK_INTERPOLATION;
        position.y += (targetPosition.y - position.y) * NETWORK_INTERPOLATION;
    }
    
    public void updateNetworkPosition(float x, float y) {
        if (!isLocal) {
            previousPosition.set(position);
            targetPosition.set(x, y);
            lastUpdateTime = 0;
        }
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        if (isDead) return;
        
        renderPlayerBody(shapeRenderer);
        renderDirectionIndicator(shapeRenderer);
        renderHealthBar(shapeRenderer);
    }
    
    private void renderPlayerBody(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(color);
        shapeRenderer.circle(position.x, position.y, SIZE/2);
    }
    
    private void renderDirectionIndicator(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(
            position.x,
            position.y,
            position.x + direction.x * DIRECTION_INDICATOR_LENGTH,
            position.y + direction.y * DIRECTION_INDICATOR_LENGTH
        );
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

    public boolean checkBulletCollision(Bullet bullet) {
        if (isDead || bullet.shooterId.equals(id)) return false;
        return collisionBounds.contains(bullet.position.x, bullet.position.y);
    }

    public void takeDamage(int damage) {
        if (isDead) return;
        health -= damage;
        if (health <= 0) {
            health = 0;
            isDead = true;
        }
    }

    public void setHealth(int health) {
        this.health = Math.min(Math.max(health, 0), MAX_HEALTH);
    }

    public int getHealth() {
        return health;
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

    public boolean isLocal() {
        return isLocal;
    }

    public void respawn(float x, float y) {
        position.set(x, y);
        targetPosition.set(x, y);
        previousPosition.set(x, y);
        collisionBounds.setPosition(x, y);
        health = MAX_HEALTH;
        isDead = false;
    }
} 