package com.dodgegame.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * ParticleManager - Manages a pool of simple coloured particles.
 *
 * Key concepts demonstrated:
 *   - Particle system fundamentals: position, velocity, lifetime
 *   - Manual object pool to avoid per-frame allocation
 *   - Alpha fade-out using colour interpolation
 */
public class ParticleManager {

    /** A single particle — tiny, mutable, pool-friendly. */
    private static class Particle {
        float x, y;        // position
        float vx, vy;      // velocity (px/s)
        float life;        // remaining lifetime (seconds)
        float maxLife;     // used to compute alpha
        float radius;
        Color color = new Color();
        boolean active;

        void reset(float x, float y, float vx, float vy, float life, float radius, Color c) {
            this.x       = x;
            this.y       = y;
            this.vx      = vx;
            this.vy      = vy;
            this.life    = life;
            this.maxLife = life;
            this.radius  = radius;
            this.color.set(c);
            this.active  = true;
        }
    }

    private static final int   POOL_SIZE  = 200;
    private final Array<Particle> pool    = new Array<>(POOL_SIZE);
    private final Array<Particle> active  = new Array<>(POOL_SIZE);

    public ParticleManager() {
        for (int i = 0; i < POOL_SIZE; i++) pool.add(new Particle());
    }

    /**
     * Emit an explosion burst at (cx, cy) with the given colour.
     *
     * @param count number of particles to emit
     */
    public void explode(float cx, float cy, Color color, int count) {
        for (int i = 0; i < count; i++) {
            if (pool.size == 0) break;                // pool exhausted — skip

            float angle  = MathUtils.random(0f, MathUtils.PI2);
            float speed  = MathUtils.random(60f, 280f);
            float life   = MathUtils.random(0.4f, 1.0f);
            float radius = MathUtils.random(2f, 7f);

            Particle p = pool.removeIndex(pool.size - 1);
            p.reset(cx, cy,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    life, radius, color);
            active.add(p);
        }
    }

    /** Emit a smaller "dodge streak" effect when an object passes below the player. */
    public void emitDodgeStreak(float x, float y) {
        explode(x, y, new Color(0.4f, 0.9f, 1f, 1f), 6);
    }

    public void update(float delta) {
        for (int i = active.size - 1; i >= 0; i--) {
            Particle p = active.get(i);

            p.x    += p.vx * delta;
            p.y    += p.vy * delta;
            p.vy   -= 200f * delta;   // simple gravity on particles
            p.life -= delta;

            if (p.life <= 0) {
                p.active = false;
                active.removeIndex(i);
                pool.add(p);           // return to pool
            }
        }
    }

    public void draw(ShapeRenderer sr) {
        for (Particle p : active) {
            float alpha = p.life / p.maxLife;           // linear fade-out
            sr.setColor(p.color.r, p.color.g, p.color.b, alpha);
            sr.circle(p.x, p.y, p.radius * alpha, 8);  // also shrinks as it fades
        }
    }
}
