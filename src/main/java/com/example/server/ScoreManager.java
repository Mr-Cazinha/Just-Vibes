package com.example.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ScoreManager {
    private static final int WINNING_SCORE = 10;
    private final Map<String, Integer> playerScores = new ConcurrentHashMap<>();
    private String winner = null;

    public void addPlayer(String playerId) {
        playerScores.put(playerId, 0);
    }

    public void removePlayer(String playerId) {
        playerScores.remove(playerId);
    }

    public int addKill(String playerId) {
        if (!playerScores.containsKey(playerId)) return 0;
        
        int newScore = playerScores.compute(playerId, (id, score) -> (score == null ? 1 : score + 1));
        if (newScore >= WINNING_SCORE && winner == null) {
            winner = playerId;
        }
        return newScore;
    }

    public boolean hasWinner() {
        return winner != null;
    }

    public String getWinner() {
        return winner;
    }

    public Map<String, Integer> getScores() {
        return new ConcurrentHashMap<>(playerScores);
    }

    public void reset() {
        playerScores.clear();
        winner = null;
    }
} 