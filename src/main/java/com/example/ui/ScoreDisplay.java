package com.example.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Map;
import java.util.TreeMap;

public class ScoreDisplay {
    private final BitmapFont font;
    private final float x;
    private final float y;
    private final String localPlayerId;
    private Map<String, Integer> scores = new TreeMap<>();
    private String winner = null;

    public ScoreDisplay(float x, float y, String localPlayerId) {
        this.font = new BitmapFont();
        this.font.setColor(Color.WHITE);
        this.x = x;
        this.y = y;
        this.localPlayerId = localPlayerId;
    }

    public void updateScores(Map<String, Integer> newScores, String winner) {
        this.scores = new TreeMap<>(newScores);
        this.winner = winner;
    }

    public void render(SpriteBatch batch) {
        // Draw header
        font.setColor(Color.YELLOW);
        font.draw(batch, "SCORES", x, y);

        // Draw scores
        final float scoreStartY = y - 20;
        int index = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String playerId = entry.getKey();
            int score = entry.getValue();
            
            if (playerId.equals(localPlayerId)) {
                font.setColor(Color.GREEN);
            } else {
                font.setColor(Color.WHITE);
            }
            font.draw(batch, String.format("%s: %d", playerId, score), x, scoreStartY - (index * 20));
            index++;
        }

        // Draw winner if game is over
        if (winner != null) {
            font.setColor(Color.GOLD);
            font.draw(batch, "WINNER: " + winner, x, scoreStartY - (scores.size() + 1) * 20);
        }
    }

    public void dispose() {
        font.dispose();
    }
} 