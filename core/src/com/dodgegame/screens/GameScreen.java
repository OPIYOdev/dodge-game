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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.dodgegame.DodgeGame;
import com.dodgegame.entities.FallingObject;
import com.dodgegame.entities.Player;
import com.dodgegame.managers.ObjectSpawner;
import com.dodgegame.managers.ParticleManager;
import com.dodgegame.managers.ScoreManager;

/**
 * GameScreen - The core gameplay state.
 *
 * Implements libGDX's Screen interface, which provides a clean lifecycle:
 *   show() → render() loop → hide() → dispose()
 *
 * Key concepts demonstrated:
 *   - Orthographic camera and viewport management
 *   - The game loop: update → collision → render
 *   - ShapeRenderer batching (begin/end must bracket all draw calls)
 *   - BitmapFont for HUD text rendering
 *   - State machine: PLAYING → GAME_OVER → (back to menu or restart)
 */
public class GameScreen implements Screen {

    // ----- Core -----
    private final DodgeGame game;
    private OrthographicCamera camera;

    // ----- Rendering -----
    private ShapeRenderer shapeRenderer;
    private BitmapFont    font;
    private BitmapFont    bigFont;
    private GlyphLayout   layout;       // reused to measure text width

    // ----- Game objects -----
    private Player        player;
    private ObjectSpawner spawner;
    private ScoreManager  scoreManager;
    private ParticleManager particles;

    // ----- Game state -----
    private enum State { PLAYING, GAME_OVER }
    private State  state;
    private float  gameOverTimer = 0f;   // brief delay before inputs are accepted
    private int    lives;
    private static final int MAX_LIVES = 3;

    // ----- Screen-shake effect on hit -----
    private float shakeTimer     = 0f;
    private float shakeMagnitude = 0f;
    private static final float SHAKE_DURATION = 0.4f;

    // ----- Starfield background -----
    private float[] starX, starY, starSize, starSpeed;
    private static final int STAR_COUNT = 80;

    public GameScreen(DodgeGame game) {
        this.game = game;
    }

    // =========================================================================
    // Screen lifecycle
    // =========================================================================

    @Override
    public void show() {
        // Camera uses the fixed world coordinate system (800×600)
        camera = new OrthographicCamera();
        camera.setToOrtho(false, DodgeGame.WORLD_WIDTH, DodgeGame.WORLD_HEIGHT);

        shapeRenderer = new ShapeRenderer();
        font          = new BitmapFont();             // built-in 15px Arial font
        bigFont       = new BitmapFont();
        bigFont.getData().setScale(2.5f);
        layout        = new GlyphLayout();

        // Initialise starfield (decorative background)
        starX    = new float[STAR_COUNT];
        starY    = new float[STAR_COUNT];
        starSize = new float[STAR_COUNT];
        starSpeed= new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]    = (float) (Math.random() * DodgeGame.WORLD_WIDTH);
            starY[i]    = (float) (Math.random() * DodgeGame.WORLD_HEIGHT);
            starSize[i] = (float) (Math.random() * 2f + 0.5f);
            starSpeed[i]= (float) (Math.random() * 30f + 10f);
        }

        startGame();
    }

    /** (Re-)initialise all game objects for a fresh game. */
    private void startGame() {
        player      = new Player(DodgeGame.WORLD_WIDTH / 2f, 60f);
        spawner     = new ObjectSpawner();
        scoreManager = new ScoreManager();
        particles   = new ParticleManager();
        lives       = MAX_LIVES;
        state       = State.PLAYING;
        gameOverTimer = 0f;
    }

    // =========================================================================
    // Game loop — called every frame by libGDX
    // =========================================================================

    @Override
    public void render(float delta) {
        // Cap delta to avoid huge jumps if the window is moved/resized
        float safeDelta = Math.min(delta, 0.05f);

        update(safeDelta);
        draw();
    }

    /** Update phase: advance all simulation state. */
    private void update(float delta) {
        // Scroll starfield regardless of game state
        for (int i = 0; i < STAR_COUNT; i++) {
            starY[i] -= starSpeed[i] * delta;
            if (starY[i] < 0) {
                starY[i] = DodgeGame.WORLD_HEIGHT;
                starX[i] = (float) (Math.random() * DodgeGame.WORLD_WIDTH);
            }
        }

        if (state == State.PLAYING) {
            updatePlaying(delta);
        } else {
            updateGameOver(delta);
        }
    }

    private void updatePlaying(float delta) {
        player.update(delta);
        spawner.update(delta);
        scoreManager.update(delta);
        particles.update(delta);

        if (shakeTimer > 0) shakeTimer -= delta;

        checkCollisions();
        checkDodges();
    }

    private void updateGameOver(float delta) {
        gameOverTimer += delta;
        particles.update(delta);

        // Accept restart/menu input after a short grace period
        if (gameOverTimer > 1.0f) {
            if (Gdx.input.isKeyJustPressed(Keys.SPACE) || Gdx.input.isKeyJustPressed(Keys.R)) {
                startGame();
            }
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    // =========================================================================
    // Collision detection
    // =========================================================================

    /**
     * AABB collision check between player and every active falling object.
     *
     * libGDX Rectangle.overlaps() performs a standard 2D AABB intersection test:
     *   overlap iff not (A.right < B.left || A.left > B.right || A.top < B.bottom || A.bottom > B.top)
     */
    private void checkCollisions() {
        Rectangle playerBounds = player.getBounds();
        Array<FallingObject> objects = spawner.getActiveObjects();

        for (FallingObject obj : objects) {
            if (obj.overlaps(playerBounds)) {
                handleHit(obj);
            }
        }
    }

    private void handleHit(FallingObject obj) {
        obj.deactivate();
        lives--;

        // Explosion particle burst at player centre
        particles.explode(
            player.getCenterX(), player.getCenterY(),
            new Color(1f, 0.4f, 0.1f, 1f), 30
        );

        // Trigger screen shake
        shakeTimer     = SHAKE_DURATION;
        shakeMagnitude = 12f;

        if (lives <= 0) {
            player.kill();
            scoreManager.saveHighScore();
            state = State.GAME_OVER;
            gameOverTimer = 0f;

            // Big final explosion
            particles.explode(player.getCenterX(), player.getCenterY(),
                              new Color(1f, 0.6f, 0.0f, 1f), 60);
        }
    }

    /**
     * Award a dodge bonus when a falling object passes below the player
     * without hitting it — rewarding active play rather than corner-camping.
     */
    private void checkDodges() {
        float playerBottom = player.getBounds().y;
        Array<FallingObject> objects = spawner.getActiveObjects();

        for (FallingObject obj : objects) {
            float objTop = obj.getBounds().y + obj.getBounds().height;
            if (objTop < playerBottom - 10f && obj.isActive()) {
                scoreManager.registerDodge();
                particles.emitDodgeStreak(
                    obj.getBounds().x + obj.getBounds().width / 2f,
                    playerBottom
                );
            }
        }
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    private void draw() {
        // Clear to deep space colour
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Apply camera (and optional screen shake)
        applyShake();
        camera.update();

        // --- Shape rendering pass ---
        // ShapeRenderer.begin()/end() marks a GPU draw call boundary.
        // Switching type (Filled/Line/Point) requires a begin/end pair.
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        drawStarfield();
        spawner.draw(shapeRenderer);
        particles.draw(shapeRenderer);

        if (player.isAlive()) {
            player.draw(shapeRenderer);
        }

        shapeRenderer.end();

        // --- Text / HUD pass ---
        // SpriteBatch is used for BitmapFont; separate from ShapeRenderer batch
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        drawHUD();
        if (state == State.GAME_OVER) drawGameOver();

        game.batch.end();

        // Reset camera after shake each frame
        camera.position.set(DodgeGame.WORLD_WIDTH / 2f, DodgeGame.WORLD_HEIGHT / 2f, 0);
    }

    private void drawStarfield() {
        for (int i = 0; i < STAR_COUNT; i++) {
            float brightness = 0.3f + starSize[i] * 0.2f;
            shapeRenderer.setColor(brightness, brightness, brightness + 0.1f, 1f);
            shapeRenderer.circle(starX[i], starY[i], starSize[i], 4);
        }
    }

    private void drawHUD() {
        // Score
        font.setColor(Color.WHITE);
        font.draw(game.batch, "SCORE: " + scoreManager.getCurrentScore(), 10, DodgeGame.WORLD_HEIGHT - 10);

        // High score
        font.setColor(new Color(1f, 0.9f, 0.3f, 1f));
        font.draw(game.batch, "BEST: " + scoreManager.getHighScore(), 10, DodgeGame.WORLD_HEIGHT - 28);

        // Lives (drawn as coloured hearts ♥)
        font.setColor(new Color(1f, 0.3f, 0.3f, 1f));
        StringBuilder livesStr = new StringBuilder("LIVES: ");
        for (int i = 0; i < lives; i++) livesStr.append("♥ ");
        font.draw(game.batch, livesStr.toString(), DodgeGame.WORLD_WIDTH - 140, DodgeGame.WORLD_HEIGHT - 10);

        // Controls hint (bottom of screen)
        font.setColor(new Color(1f, 1f, 1f, 0.4f));
        font.draw(game.batch, "WASD / Arrow keys to move", 10, 20);
    }

    private void drawGameOver() {
        String title = "GAME OVER";
        bigFont.setColor(new Color(1f, 0.3f, 0.2f, 1f));
        layout.setText(bigFont, title);
        bigFont.draw(game.batch, title,
            (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
            DodgeGame.WORLD_HEIGHT / 2f + 60f);

        font.setColor(Color.WHITE);
        String scoreStr = "Score: " + scoreManager.getCurrentScore();
        layout.setText(font, scoreStr);
        font.draw(game.batch, scoreStr,
            (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
            DodgeGame.WORLD_HEIGHT / 2f + 10f);

        if (scoreManager.isNewHighScore()) {
            font.setColor(new Color(1f, 0.9f, 0.2f, 1f));
            String newBest = "★ NEW HIGH SCORE! ★";
            layout.setText(font, newBest);
            font.draw(game.batch, newBest,
                (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
                DodgeGame.WORLD_HEIGHT / 2f - 15f);
        }

        font.setColor(new Color(1f, 1f, 1f, 0.7f));
        String restart = "[SPACE / R] Play Again    [ESC] Menu";
        layout.setText(font, restart);
        font.draw(game.batch, restart,
            (DodgeGame.WORLD_WIDTH - layout.width) / 2f,
            DodgeGame.WORLD_HEIGHT / 2f - 50f);
    }

    /** Offset the camera by a decaying random amount for the screen-shake effect. */
    private void applyShake() {
        if (shakeTimer > 0) {
            float t = shakeTimer / SHAKE_DURATION;
            float dx = (float) ((Math.random() * 2 - 1) * shakeMagnitude * t);
            float dy = (float) ((Math.random() * 2 - 1) * shakeMagnitude * t);
            camera.position.set(
                DodgeGame.WORLD_WIDTH  / 2f + dx,
                DodgeGame.WORLD_HEIGHT / 2f + dy,
                0
            );
        }
    }

    // =========================================================================
    // Screen lifecycle (continued)
    // =========================================================================

    @Override
    public void resize(int width, int height) {
        // Keep the logical world size constant; letterbox/pillarbox as needed
        camera.setToOrtho(false, DodgeGame.WORLD_WIDTH, DodgeGame.WORLD_HEIGHT);
    }

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        bigFont.dispose();
    }
}
