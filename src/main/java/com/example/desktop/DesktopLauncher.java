package com.example.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.example.MyGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Just Vibes");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        config.setForegroundFPS(60);
        
        String serverIp = args.length > 0 ? args[0] : "localhost";
        new Lwjgl3Application(new MyGame(serverIp), config);
    }
} 