package com.example;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.example.screens.MainGameScreen;
import com.example.screens.ConnectionScreen;

public class MyGame extends Game {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private String serverIp;

    public MyGame(String serverIp) {
        this.serverIp = serverIp;
    }

    @Override
    public void create() {
        setScreen(new ConnectionScreen(this));
    }

    public void startGame(String serverIp) {
        getScreen().dispose();
        setScreen(new MainGameScreen(this, serverIp));
    }

    public void exit() {
        Gdx.app.exit();
    }

    public static void main(String[] args) {
        String serverIp = args.length > 0 ? args[0] : "localhost";
        
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Just Vibes");
        config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setResizable(false);  // Prevent window resizing
        config.setMaximized(false);  // Prevent maximizing
        config.setWindowIcon("icon.png");
        
        new Lwjgl3Application(new MyGame(serverIp), config);
    }
} 