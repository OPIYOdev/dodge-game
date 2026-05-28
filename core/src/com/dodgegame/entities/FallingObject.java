package com.dodgegame.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * FallingObject - A hazard that falls from the top of the screen.
 *
 * Key concepts demonstrated:
 *   - Simple gravity / constant-velocity physics
 *   - Object pooling friendly design (reset() method)
 *   - Randomised visual variety (shape, color, size, speed)
 *   - Off-screen culling
 */
public class FallingObject {

    /** Visual shape enum — easy to extend with new hazard types. */
    public enum Shape { CIRCLE, SQUARE, TRIANGLE }

    // ----- Physics -----
    private final Rectangle bounds;
    private float            speedY;   // pixels per second (always negative = downward)
    private float            rotation; // degrees, for spinning effect
    private float            spinRate; // degrees per second

    // ----- Visual -----
    private final Shape shape;
    private final Color color;
    private boolean     active; // false = can be returned to the object pool

    // ----- Animation -----
    private float warningTimer = 0f;           // shows a warning flash before object appears
    private static final float WARNING_DURATION = 0.4f;

    public FallingObject(float x, float y, float size, float speed) {
        bounds   = new Rectangle(x - size / 2f, y - size / 2f, size, size);
        speedY   = -Math.abs(speed);           // ensure downward
        rotation = 0f;
        spinRate = MathUtils.random(-180f, 180f);
        shape    = Shape.values()[MathUtils.random(Shape.values().length - 1)];
        color    = randomHazardColor();
        active   = true;
    }

    /** Randomise a vibrant, readable hazard color. */
    private static Color randomHazardColor() {
        Color[] palette = {
            new Color(1f,   0.3f, 0.3f, 1f),  // red
            new Color(1f,   0.6f, 0.1f, 1f),  // orange
            new Color(1f,   0.9f, 0.1f, 1f),  // yellow
            new Color(0.8f, 0.2f, 1f,   1f),  // purple
            new Color(0.2f, 1f,   0.4f, 1f),  // green
        };
        return palette[MathUtils.random(palette.length - 1)];
    }

    /**
     * update() — called each frame by ObjectSpawner.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        if (!active) return;

        warningTimer += delta;

        // Only start falling after the brief warning flash
        if (warningTimer > WARNING_DURATION) {
            bounds.y += speedY * delta;
            rotation += spinRate * delta;
        }

        // Deactivate when fully off the bottom of the screen
        if (bounds.y + bounds.height < -50f) {
            active = false;
        }
    }

    /** Draw the object using primitive ShapeRenderer calls. */
    public void draw(ShapeRenderer sr) {
        if (!active) return;

        float cx = bounds.x + bounds.width  / 2f;
        float cy = bounds.y + bounds.height / 2f;
        float r  = bounds.width / 2f;

        // Warning flash: draw a translucent ring before falling
        if (warningTimer < WARNING_DURATION) {
            float alpha = (float) Math.abs(Math.sin(warningTimer * Math.PI * 8f));
            sr.setColor(new Color(color.r, color.g, color.b, alpha * 0.7f));
            sr.circle(cx, cy, r * 1.4f, 20);
            return; // skip normal drawing during warning phase
        }

        sr.setColor(color);

        switch (shape) {
            case CIRCLE:
                sr.circle(cx, cy, r, 24);
                break;

            case SQUARE:
                // Rotate around centre for spinning effect
                drawRotatedRect(sr, cx, cy, bounds.width, bounds.height, rotation);
                break;

            case TRIANGLE:
                drawRotatedTriangle(sr, cx, cy, r, rotation);
                break;
        }
    }

    /** Draw a rectangle rotated around its centre. */
    private void drawRotatedRect(ShapeRenderer sr,
                                  float cx, float cy,
                                  float w, float h,
                                  float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float hw = w / 2f, hh = h / 2f;

        // Four corners relative to centre, then rotated
        float[] xs = {-hw, hw, hw, -hw};
        float[] ys = {-hh, -hh, hh, hh};

        float[] rx = new float[4], ry = new float[4];
        for (int i = 0; i < 4; i++) {
            rx[i] = cx + xs[i] * cos - ys[i] * sin;
            ry[i] = cy + xs[i] * sin + ys[i] * cos;
        }

        // Draw as two triangles
        sr.triangle(rx[0], ry[0], rx[1], ry[1], rx[2], ry[2]);
        sr.triangle(rx[0], ry[0], rx[2], ry[2], rx[3], ry[3]);
    }

    /** Draw an equilateral triangle, rotated around its centroid. */
    private void drawRotatedTriangle(ShapeRenderer sr,
                                      float cx, float cy,
                                      float r, float angleDeg) {
        float[] vx = new float[3], vy = new float[3];
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(angleDeg + i * 120.0);
            vx[i] = cx + r * (float) Math.cos(a);
            vy[i] = cy + r * (float) Math.sin(a);
        }
        sr.triangle(vx[0], vy[0], vx[1], vy[1], vx[2], vy[2]);
    }

    // ----- Accessors -----

    public Rectangle getBounds() { return bounds; }
    public boolean   isActive()  { return active; }
    public void      deactivate(){ active = false; }

    /** Check AABB overlap with another rectangle (used for collision detection). */
    public boolean overlaps(Rectangle other) {
        return active && bounds.overlaps(other);
    }
}
