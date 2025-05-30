package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;

public class Bullet {
    private static final float SPEED = 400f;
    private static final float RADIUS = 5f;
    
    private final Vector2 position;
    private final Vector2 velocity;
    private final String ownerId;
    private final Circle bounds;
    public boolean active;
    protected boolean penetratesBoundary;  // Whether the bullet can pass through map boundaries
    
    public Bullet(String ownerId, float x, float y, float dirX, float dirY) {
        this.ownerId = ownerId;
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(dirX, dirY).nor().scl(SPEED);
        this.bounds = new Circle(x, y, RADIUS);
        this.active = true;
        this.penetratesBoundary = false;  // Default bullets stop at boundaries
    }
    
    public void update(float delta) {
        if (!active) return;
        
        // Store previous position
        float oldX = position.x;
        float oldY = position.y;
        
        // Update position
        position.add(velocity.x * delta, velocity.y * delta);
        bounds.setPosition(position);
        
        // Check map boundaries if bullet doesn't penetrate
        if (!penetratesBoundary) {
            if (position.x - RADIUS < 0 || position.x + RADIUS > 800 ||
                position.y - RADIUS < 0 || position.y + RADIUS > 600) {
                // Deactivate bullet at boundary
                active = false;
                // Restore position to boundary
                position.set(oldX, oldY);
                bounds.setPosition(position);
            }
        }
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        if (!active) return;
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(position.x, position.y, RADIUS);
    }
    
    public boolean isOutOfBounds(float width, float height) {
        if (penetratesBoundary) {
            // For penetrating bullets, only deactivate when completely out of view
            return position.x < -RADIUS || position.x > width + RADIUS || 
                   position.y < -RADIUS || position.y > height + RADIUS;
        }
        // For regular bullets, deactivate at boundary
        return position.x - RADIUS < 0 || position.x + RADIUS > width || 
               position.y - RADIUS < 0 || position.y + RADIUS > height;
    }
    
    public boolean checkCollision(Player player) {
        if (!active || player.isDead() || player.getId().equals(ownerId)) {
            return false;
        }
        return Intersector.overlaps(bounds, player.getBounds());
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public Vector2 getPosition() {
        return position;
    }
    
    public float getRadius() {
        return RADIUS;
    }
    
    // Method to set boundary penetration for different bullet types
    public void setPenetratesBoundary(boolean penetrates) {
        this.penetratesBoundary = penetrates;
    }
    
    // Method to check if bullet penetrates boundaries
    public boolean doesPenetrateBoundary() {
        return penetratesBoundary;
    }
} 