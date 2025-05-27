package com.example.map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import java.util.Random;

public class CityBackground {
    private static final Color ROAD_COLOR = new Color(0.2f, 0.2f, 0.2f, 1);
    private static final Color SIDEWALK_COLOR = new Color(0.7f, 0.7f, 0.7f, 1);
    private static final Color BUILDING_COLOR_1 = new Color(0.4f, 0.4f, 0.5f, 1);
    private static final Color BUILDING_COLOR_2 = new Color(0.5f, 0.5f, 0.6f, 1);
    private static final Color BUILDING_WINDOW = new Color(0.9f, 0.9f, 0.7f, 1);
    
    private final int width;
    private final int height;
    private final Random random;
    
    public CityBackground(int width, int height) {
        this.width = width;
        this.height = height;
        this.random = new Random(123); // Fixed seed for consistent building layout
    }
    
    public void render(ShapeRenderer shapeRenderer) {
        drawRoads(shapeRenderer);
        drawBuildings(shapeRenderer);
    }
    
    private void drawRoads(ShapeRenderer shapeRenderer) {
        // Main road
        shapeRenderer.set(ShapeType.Filled);
        shapeRenderer.setColor(ROAD_COLOR);
        
        // Horizontal roads
        shapeRenderer.rect(0, height/3 - 50, width, 100);
        shapeRenderer.rect(0, 2*height/3 - 50, width, 100);
        
        // Vertical roads
        shapeRenderer.rect(width/3 - 50, 0, 100, height);
        shapeRenderer.rect(2*width/3 - 50, 0, 100, height);
        
        // Sidewalks
        shapeRenderer.setColor(SIDEWALK_COLOR);
        // Horizontal sidewalks
        drawSidewalks(shapeRenderer, true);
        // Vertical sidewalks
        drawSidewalks(shapeRenderer, false);
    }
    
    private void drawSidewalks(ShapeRenderer shapeRenderer, boolean horizontal) {
        int[] positions = {height/3, 2*height/3};
        if (!horizontal) {
            positions = new int[]{width/3, 2*width/3};
        }
        
        for (int pos : positions) {
            if (horizontal) {
                // Top sidewalk
                shapeRenderer.rect(0, pos - 60, width, 10);
                // Bottom sidewalk
                shapeRenderer.rect(0, pos + 50, width, 10);
            } else {
                // Left sidewalk
                shapeRenderer.rect(pos - 60, 0, 10, height);
                // Right sidewalk
                shapeRenderer.rect(pos + 50, 0, 10, height);
            }
        }
    }
    
    private void drawBuildings(ShapeRenderer shapeRenderer) {
        // Draw buildings in each block
        int blockWidth = width/3;
        int blockHeight = height/3;
        
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                // Skip road intersections
                if (!isRoadIntersection(x, y)) {
                    drawBuildingBlock(shapeRenderer, 
                        x * blockWidth + 70, 
                        y * blockHeight + 70,
                        blockWidth - 140,
                        blockHeight - 140);
                }
            }
        }
    }
    
    private boolean isRoadIntersection(int x, int y) {
        return (x == 1 && y == 1) || // Center
               (x == 1 && y == 0) || // Middle bottom
               (x == 1 && y == 2) || // Middle top
               (x == 0 && y == 1) || // Left middle
               (x == 2 && y == 1);   // Right middle
    }
    
    private void drawBuildingBlock(ShapeRenderer shapeRenderer, int x, int y, int w, int h) {
        random.setSeed(x * 10000L + y); // Consistent random buildings per block
        
        int numBuildings = 2 + random.nextInt(2);
        int buildingWidth = w / numBuildings;
        
        for (int i = 0; i < numBuildings; i++) {
            int bx = x + i * buildingWidth;
            int by = y;
            int bw = buildingWidth - 10;
            int bh = h - random.nextInt(h/3);
            
            // Building base
            shapeRenderer.setColor(i % 2 == 0 ? BUILDING_COLOR_1 : BUILDING_COLOR_2);
            shapeRenderer.rect(bx, by, bw, bh);
            
            // Windows
            shapeRenderer.setColor(BUILDING_WINDOW);
            int windowRows = bh / 30;
            int windowCols = bw / 30;
            
            for (int row = 0; row < windowRows; row++) {
                for (int col = 0; col < windowCols; col++) {
                    if (random.nextFloat() > 0.3f) { // 70% chance of window
                        shapeRenderer.rect(
                            bx + col * 30 + 5,
                            by + row * 30 + 5,
                            20, 20);
                    }
                }
            }
        }
    }
} 