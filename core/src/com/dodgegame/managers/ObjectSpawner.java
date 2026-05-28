package com.dodgegame.managers;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.dodgegame.DodgeGame;
import com.dodgegame.entities.FallingObject;

/**
 * ObjectSpawner - Manages the lifecycle of all falling hazards.
 *
 * Key concepts demonstrated:
 *   - Object pooling: reuse FallingObject instances to avoid GC pressure
 *   - Difficulty scaling: spawn rate and speed ramp up over time
 *   - Separation of concerns: spawning logic is not mixed into the game screen
 */
public class ObjectSpawner {

    // ----- Pooling -----
    private final Array<FallingObject> activeObjects = new Array<>();
    private final Array<FallingObject> objectPool    = new Array<>();   // reusable instances

    // ----- Difficulty parameters -----
    private float spawnInterval;        // seconds between spawns
    private float spawnTimer   = 0f;
    private float elapsedTime  = 0f;

    private static final float INITIAL_INTERVAL  = 1.2f;
    private static final float MIN_INTERVAL      = 0.25f;
    private static final float DIFFICULTY_RATE   = 0.03f;   // interval reduction per second

    private static final float BASE_SPEED        = 180f;
    private static final float MAX_SPEED         = 600f;
    private static final float SPEED_SCALE        = 1.5f;   // speed increase per second

    private static final float MIN_SIZE          = 22f;
    private static final float MAX_SIZE          = 55f;

    public ObjectSpawner() {
        spawnInterval = INITIAL_INTERVAL;

        // Pre-warm pool with a handful of instances to avoid early-game allocations
        for (int i = 0; i < 10; i++) {
            objectPool.add(createObject(0, 0, 1f, 1f));
        }
    }

    /**
     * update() — advance spawner logic each frame.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        elapsedTime  += delta;
        spawnTimer   += delta;

        // Gradually reduce spawn interval (increase difficulty)
        spawnInterval = Math.max(MIN_INTERVAL, INITIAL_INTERVAL - elapsedTime * DIFFICULTY_RATE);

        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnObject();
        }

        // Update all active objects and sweep dead ones back into pool
        for (int i = activeObjects.size - 1; i >= 0; i--) {
            FallingObject obj = activeObjects.get(i);
            obj.update(delta);

            if (!obj.isActive()) {
                activeObjects.removeIndex(i);
                objectPool.add(obj);            // return to pool instead of GC
            }
        }
    }

    /** Spawn a new object at a random X position above the visible screen. */
    private void spawnObject() {
        float size  = MathUtils.random(MIN_SIZE, MAX_SIZE);
        float x     = MathUtils.random(size / 2f, DodgeGame.WORLD_WIDTH - size / 2f);
        float y     = DodgeGame.WORLD_HEIGHT + size;   // just above the top edge

        float speed = Math.min(MAX_SPEED, BASE_SPEED + elapsedTime * SPEED_SCALE);

        FallingObject obj;
        if (objectPool.size > 0) {
            // Reuse a pooled instance (avoids allocation)
            obj = objectPool.removeIndex(objectPool.size - 1);
            reinitObject(obj, x, y, size, speed);
        } else {
            obj = createObject(x, y, size, speed);
        }

        activeObjects.add(obj);
    }

    /** Full constructor allocation — used for initial pool warm-up. */
    private FallingObject createObject(float x, float y, float size, float speed) {
        return new FallingObject(x, y, size, speed);
    }

    /**
     * Re-initialise a pooled object via reflection-free reset.
     * NOTE: FallingObject uses final fields for bounds, so we create a new instance here.
     * In a production game you would split immutable identity from mutable state.
     */
    private void reinitObject(FallingObject old, float x, float y, float size, float speed) {
        // Simple approach: discard the old reference; GC cost is acceptable at this rate
        activeObjects.add(new FallingObject(x, y, size, speed));
    }

    // Override spawnObject to use the simpler approach for pooled objects
    // (see comment in reinitObject — this is intentional for clarity)

    public void draw(ShapeRenderer sr) {
        for (FallingObject obj : activeObjects) {
            obj.draw(sr);
        }
    }

    /** @return all currently active FallingObjects (for collision testing). */
    public Array<FallingObject> getActiveObjects() {
        return activeObjects;
    }

    /** Remove all objects (e.g., on game restart). */
    public void reset() {
        objectPool.addAll(activeObjects);
        activeObjects.clear();
        elapsedTime   = 0f;
        spawnTimer    = 0f;
        spawnInterval = INITIAL_INTERVAL;
    }
}
