package com.example;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.example.screens.MainGameScreen;
import com.example.screens.ConnectionScreen;

public class MyGame extends Game {
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new ConnectionScreen(this));
    }

    public void startGame(String serverIp) {
        getScreen().dispose();
        setScreen(new MainGameScreen(this, serverIp));
    }

    @Override
    public void dispose() {
        batch.dispose();
        getScreen().dispose();
        super.dispose();
    }
} 