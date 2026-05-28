package com.dodgegame.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.dodgegame.DodgeGame;

/**
 * Player - The character controlled by the user.
 *
 * Key concepts demonstrated:
 *   - Input polling (Gdx.input.isKeyPressed) vs event-based input
 *   - Delta-time movement for frame-rate-independent physics
 *   - Axis-aligned bounding box (AABB) via libGDX Rectangle
 *   - Bounds clamping to keep entity inside the world
 */
public class Player {

    // ----- Gameplay constants -----
    private static final float SPEED      = 400f;  // pixels per second
    private static final float WIDTH      = 48f;
    private static final float HEIGHT     = 48f;

    // ----- State -----
    private final Rectangle bounds;          // used for both rendering position and collision
    private boolean isAlive = true;

    // ----- Cosmetic -----
    private float   pulseTimer  = 0f;        // drives a subtle size pulse animation
    private static final Color BODY_COLOR  = new Color(0.2f, 0.6f, 1.0f, 1f);
    private static final Color COCKPIT_COLOR = new Color(0.8f, 0.95f, 1.0f, 1f);

    public Player(float startX, float startY) {
        bounds = new Rectangle(startX - WIDTH / 2f, startY - HEIGHT / 2f, WIDTH, HEIGHT);
    }

    /**
     * update() is called once per frame from the GameScreen render loop.
     *
     * @param delta seconds since the last frame (from Gdx.graphics.getDeltaTime())
     */
    public void update(float delta) {
        if (!isAlive) return;

        pulseTimer += delta;

        handleInput(delta);
        clampToWorld();
    }

    /** Poll keyboard state and translate the player accordingly. */
    private void handleInput(float delta) {
        float dx = 0, dy = 0;

        // Arrow keys + WASD both work — libGDX keycodes are just ints
        if (Gdx.input.isKeyPressed(Keys.LEFT)  || Gdx.input.isKeyPressed(Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) dx += 1;
        if (Gdx.input.isKeyPressed(Keys.UP)    || Gdx.input.isKeyPressed(Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Keys.DOWN)  || Gdx.input.isKeyPressed(Keys.S)) dy -= 1;

        // Normalise diagonal movement so speed is consistent in all directions
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            dx /= len;
            dy /= len;
        }

        bounds.x += dx * SPEED * delta;
        bounds.y += dy * SPEED * delta;
    }

    /** Keep the player fully inside the world rectangle. */
    private void clampToWorld() {
        bounds.x = Math.max(0, Math.min(bounds.x, DodgeGame.WORLD_WIDTH  - bounds.width));
        bounds.y = Math.max(0, Math.min(bounds.y, DodgeGame.WORLD_HEIGHT - bounds.height));
    }

    /**
     * Draw a simple spaceship shape using ShapeRenderer.
     * Using ShapeRenderer (no texture) keeps the project dependency-free
     * while still illustrating sprite-like entity rendering.
     */
    public void draw(ShapeRenderer sr) {
        if (!isAlive) return;

        float cx = bounds.x + bounds.width  / 2f;
        float cy = bounds.y + bounds.height / 2f;

        // Subtle pulse: ±2 px breathing animation
        float pulse = (float) Math.sin(pulseTimer * 3f) * 2f;
        float w = bounds.width  + pulse;
        float h = bounds.height + pulse;

        // -- Body (filled triangle pointing up) --
        sr.setColor(BODY_COLOR);
        sr.triangle(
            cx,          cy + h / 2f,   // top point
            cx - w / 2f, cy - h / 2f,   // bottom-left
            cx + w / 2f, cy - h / 2f    // bottom-right
        );

        // -- Cockpit (small circle) --
        sr.setColor(COCKPIT_COLOR);
        sr.circle(cx, cy, w * 0.18f, 12);

        // -- Engine glow (ellipse below body) --
        sr.setColor(new Color(0.4f, 0.8f, 1f, 0.6f));
        sr.ellipse(cx - w * 0.15f, cy - h / 2f - 6f, w * 0.3f, 10f);
    }

    // ----- Accessors -----

    public Rectangle getBounds()  { return bounds; }
    public boolean   isAlive()    { return isAlive; }
    public void      kill()       { isAlive = false; }

    public float getCenterX() { return bounds.x + bounds.width  / 2f; }
    public float getCenterY() { return bounds.y + bounds.height / 2f; }
}
