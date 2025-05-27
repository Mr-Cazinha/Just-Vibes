package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Circle;

public class Player {
    private static final float SPEED = 200f;
    private static final float SIZE = 32f;
    private static final float COLLISION_RADIUS = SIZE/2;
    
    public final String id;
    public final Vector2 position;
    public final Vector2 direction;
    public Color color;
    private final Circle collisionBounds;
    private int health;
    private static final int MAX_HEALTH = 100;
    private boolean isDead;
    
    public Player(String id, float x, float y) {
        this.id = id;
        this.position = new Vector2(x, y);
        this.direction = new Vector2(1, 0);
        this.color = Color.BLUE;
        this.collisionBounds = new Circle(x, y, COLLISION_RADIUS);
        this.health = MAX_HEALTH;
        this.isDead = false;
    }
    
    public void update(float delta, float moveX, float moveY) {
        if (isDead) return;
        
        // Update position based on input
        position.x += moveX * SPEED * delta;
        position.y += moveY * SPEED * delta;
        
        // Update collision bounds
        collisionBounds.setPosition(position.x, position.y);
        
        // Update direction if moving
        if (moveX != 0 || moveY != 0) {
            direction.set(moveX, moveY).nor();
        }
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        if (isDead) return;
        
        // Draw player body
        shapeRenderer.setColor(color);
        shapeRenderer.circle(position.x, position.y, SIZE/2);
        
        // Draw direction indicator
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.line(
            position.x, 
            position.y,
            position.x + direction.x * SIZE,
            position.y + direction.y * SIZE
        );

        // Draw health bar
        float healthBarWidth = SIZE;
        float healthBarHeight = 5;
        float healthPercentage = (float) health / MAX_HEALTH;
        
        // Health bar background
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(position.x - SIZE/2, position.y + SIZE/2 + 5, healthBarWidth, healthBarHeight);
        
        // Health bar fill
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.rect(position.x - SIZE/2, position.y + SIZE/2 + 5, healthBarWidth * healthPercentage, healthBarHeight);
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

    public boolean isDead() {
        return isDead;
    }

    public void respawn(float x, float y) {
        position.set(x, y);
        collisionBounds.setPosition(x, y);
        health = MAX_HEALTH;
        isDead = false;
    }
} 