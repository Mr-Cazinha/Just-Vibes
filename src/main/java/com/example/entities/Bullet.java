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
    
    public Bullet(String ownerId, float x, float y, float dirX, float dirY) {
        this.ownerId = ownerId;
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(dirX, dirY).nor().scl(SPEED);
        this.bounds = new Circle(x, y, RADIUS);
        this.active = true;
    }
    
    public void update(float delta) {
        position.add(velocity.x * delta, velocity.y * delta);
        bounds.setPosition(position);
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(position.x, position.y, RADIUS);
    }
    
    public boolean isOutOfBounds(float width, float height) {
        return position.x < 0 || position.x > width || position.y < 0 || position.y > height;
    }
    
    public boolean checkCollision(Player player) {
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
} 