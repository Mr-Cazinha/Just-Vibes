package com.example.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.example.MyGame;

public class ConnectionScreen implements Screen {
    private final MyGame game;
    private final Stage stage;
    private final Skin skin;

    public ConnectionScreen(MyGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        // Create a simple custom skin
        skin = new Skin();
        
        // Generate a white pixel texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 1));
        textFieldStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1));
        skin.add("default", textFieldStyle);

        // TextButton style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.up = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1));
        textButtonStyle.over = skin.newDrawable("white", new Color(0.3f, 0.3f, 0.3f, 1));
        textButtonStyle.down = skin.newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1));
        skin.add("default", textButtonStyle);
        
        createUI();
    }
    
    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);
        
        Label titleLabel = new Label("Enter Server IP Address", skin);
        TextField ipInput = new TextField("127.0.0.1", skin);
        TextButton connectButton = new TextButton("Connect", skin);
        
        table.add(titleLabel).padBottom(20).row();
        table.add(ipInput).width(200).padBottom(20).row();
        table.add(connectButton).width(150).row();
        
        // Handle connect button click
        connectButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                String serverIp = ipInput.getText().trim();
                if (serverIp.isEmpty()) {
                    serverIp = "127.0.0.1";
                }
                game.startGame(serverIp);
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override
    public void show() {}

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}
} 