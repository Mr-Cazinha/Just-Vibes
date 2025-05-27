package com.example.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    private static final float SPEED = 500f;
    private static final float SIZE = 8f;
    
    public final Vector2 position;
    public final Vector2 direction;
    public final String shooterId;
    public boolean active = true;
    
    public Bullet(String shooterId, float x, float y, float dirX, float dirY) {
        this.shooterId = shooterId;
        this.position = new Vector2(x, y);
        this.direction = new Vector2(dirX, dirY).nor();
    }
    
    public void update(float delta) {
        position.x += direction.x * SPEED * delta;
        position.y += direction.y * SPEED * delta;
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(position.x, position.y, SIZE/2);
    }
    
    public boolean isOutOfBounds(float maxX, float maxY) {
        return position.x < 0 || position.x > maxX || position.y < 0 || position.y > maxY;
    }
} 