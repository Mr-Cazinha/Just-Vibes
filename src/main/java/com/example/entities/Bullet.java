package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    private static final float SPEED = 400f;
    private static final float SIZE = 8f;
    
    public final String shooterId;
    public final Vector2 position;
    private final Vector2 velocity;
    public boolean active;
    
    public Bullet(String shooterId, float x, float y, float dirX, float dirY) {
        this.shooterId = shooterId;
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(dirX, dirY).nor().scl(SPEED);
        this.active = true;
    }
    
    public void update(float delta) {
        position.mulAdd(velocity, delta);
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(position.x, position.y, SIZE/2);
    }
    
    public boolean isOutOfBounds(float width, float height) {
        return position.x < 0 || position.x > width || position.y < 0 || position.y > height;
    }
} 