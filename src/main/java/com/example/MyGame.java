package com.example;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.example.screens.MainGameScreen;
import com.example.screens.ConnectionScreen;
import com.badlogic.gdx.Gdx;

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

    public void exit() {
        dispose();
        Gdx.app.exit();
    }

    @Override
    public void dispose() {
        batch.dispose();
        getScreen().dispose();
        super.dispose();
    }
} 