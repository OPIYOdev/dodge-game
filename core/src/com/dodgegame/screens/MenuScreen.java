package com.dodgegame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.dodgegame.DodgeGame;

/**
 * MenuScreen - The main menu.
 *
 * Demonstrates:
 *   - Screen implementation with animated background
 *   - Centred BitmapFont text rendering
 *   - Keyboard input to transition between screens
 */
public class MenuScreen implements Screen {

    private final DodgeGame       game;
    private       OrthographicCamera camera;
    private       ShapeRenderer   sr;
    private       BitmapFont      titleFont;
    private       BitmapFont      bodyFont;
    private       GlyphLayout     layout;

    // ---- Demo falling objects (decorative, not interactive) ----
    private float[] demoX, demoY, demoSize, demoSpeed;
    private Color[] demoColor;
    private static final int DEMO_COUNT = 12;

    // ---- Animation timers ----
    private float timer = 0f;

    public MenuScreen(DodgeGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, DodgeGame.WORLD_WIDTH, DodgeGame.WORLD_HEIGHT);

        sr        = new ShapeRenderer();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        bodyFont  = new BitmapFont();
        bodyFont.getData().setScale(1.3f);
        layout    = new GlyphLayout();

        // Initialise decorative falling demo objects
        demoX     = new float[DEMO_COUNT];
        demoY     = new float[DEMO_COUNT];
        demoSize  = new float[DEMO_COUNT];
        demoSpeed = new float[DEMO_COUNT];
        demoColor = new Color[DEMO_COUNT];

        Color[] palette = {
            new Color(1f, 0.3f, 0.3f, 0.5f),
            new Color(1f, 0.6f, 0.1f, 0.5f),
            new Color(0.8f, 0.2f, 1f,   0.5f),
            new Color(0.2f, 1f,   0.4f, 0.5f),
        };

        for (int i = 0; i < DEMO_COUNT; i++) {
            demoX[i]    = MathUtils.random(0, DodgeGame.WORLD_WIDTH);
            demoY[i]    = MathUtils.random(0, DodgeGame.WORLD_HEIGHT);
            demoSize[i] = MathUtils.random(20f, 60f);
            demoSpeed[i]= MathUtils.random(40f, 120f);
            demoColor[i]= palette[MathUtils.random(palette.length - 1)];
        }
    }

    @Override
    public void render(float delta) {
        timer += delta;

        // Update decorative objects
        for (int i = 0; i < DEMO_COUNT; i++) {
            demoY[i] -= demoSpeed[i] * delta;
            if (demoY[i] + demoSize[i] < 0) {
                demoY[i] = DodgeGame.WORLD_HEIGHT + demoSize[i];
                demoX[i] = MathUtils.random(0, DodgeGame.WORLD_WIDTH);
            }
        }

        // Input
        if (Gdx.input.isKeyJustPressed(Keys.SPACE) || Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            game.setScreen(new GameScreen(game));
            return;
        }

        // Draw
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        sr.setProjectionMatrix(camera.combined);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Decorative falling shapes
        for (int i = 0; i < DEMO_COUNT; i++) {
            sr.setColor(demoColor[i]);
            sr.circle(demoX[i], demoY[i], demoSize[i] / 2f, 18);
        }

        // Pulsing title underline decoration
        float pulse  = (float) Math.abs(Math.sin(timer * 2f));
        float lineW  = 300f + pulse * 60f;
        sr.setColor(new Color(0.2f, 0.6f, 1f, 0.6f + pulse * 0.3f));
        sr.rect((DodgeGame.WORLD_WIDTH - lineW) / 2f,
                DodgeGame.WORLD_HEIGHT / 2f + 20f,
                lineW, 2f);

        sr.end();

        // Text
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Title
        titleFont.setColor(new Color(0.9f, 0.95f, 1f, 1f));
        String title = "DODGE!";
        layout.setText(titleFont, title);
        titleFont.draw(game.batch, title,
            (DodgeGame.WORLD_WIDTH  - layout.width)  / 2f,
            (DodgeGame.WORLD_HEIGHT / 2f) + 120f);

        // Sub-title
        bodyFont.setColor(new Color(0.6f, 0.75f, 1f, 1f));
        String sub = "The Falling Objects Game";
        layout.setText(bodyFont, sub);
        bodyFont.draw(game.batch, sub,
            (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
            DodgeGame.WORLD_HEIGHT / 2f + 60f);

        // Blinking start prompt
        if ((int) (timer * 2) % 2 == 0) {
            bodyFont.setColor(Color.WHITE);
            String start = "Press SPACE to Play";
            layout.setText(bodyFont, start);
            bodyFont.draw(game.batch, start,
                (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
                DodgeGame.WORLD_HEIGHT / 2f - 20f);
        }

        // Controls
        bodyFont.getData().setScale(0.9f);
        bodyFont.setColor(new Color(1f, 1f, 1f, 0.4f));
        String controls = "Move: WASD or Arrow Keys  |  Avoid the falling shapes";
        layout.setText(bodyFont, controls);
        bodyFont.draw(game.batch, controls,
            (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
            DodgeGame.WORLD_HEIGHT / 2f - 70f);
        bodyFont.getData().setScale(1.3f);

        game.batch.end();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        sr.dispose();
        titleFont.dispose();
        bodyFont.dispose();
    }
}
