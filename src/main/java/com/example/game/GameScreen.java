package com.example.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.example.ui.ScoreDisplay;
import com.example.network.GameClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GameScreen implements Screen {
    private final SpriteBatch batch;
    private final ScoreDisplay scoreDisplay;
    @SuppressWarnings("unused")
    private final GameClient gameClient;

    public GameScreen(Game game, String playerId, GameClient gameClient) {
        this.batch = new SpriteBatch();
        this.scoreDisplay = new ScoreDisplay(10, 600 - 10, playerId);
        this.gameClient = gameClient;
        
        gameClient.registerJsonHandler("scores", this::handleScoreUpdate);
        gameClient.registerJsonHandler("gameOver", this::handleGameOver);
    }

    private void handleScoreUpdate(JSONObject json) {
        try {
            JSONObject scores = json.getJSONObject("scores");
            Map<String, Integer> scoreMap = new HashMap<>();
            for (String key : scores.keySet()) {
                scoreMap.put(key, scores.getInt(key));
            }
            scoreDisplay.updateScores(scoreMap, json.optString("winner", null));
        } catch (JSONException e) {
            // Silent fail
        }
    }

    private void handleGameOver(JSONObject json) {
        try {
            @SuppressWarnings("unused")
            String winner = json.getString("winner");
            // Game over handling is done through score display
        } catch (JSONException e) {
            // Silent fail
        }
    }

    @Override
    public void render(float delta) {
        batch.begin();
        scoreDisplay.render(batch);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        scoreDisplay.dispose();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
} 