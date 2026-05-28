package com.dodgegame.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * ScoreManager - Handles score accumulation and persistent high score storage.
 *
 * Key concepts demonstrated:
 *   - libGDX Preferences API for cross-platform persistent storage
 *     (maps to SharedPreferences on Android, .prefs file on desktop)
 *   - Score system: time-survival base + bonus for dodged objects
 */
public class ScoreManager {

    private static final String PREFS_NAME  = "dodge_game_prefs";
    private static final String HISCORE_KEY = "high_score";

    private int   currentScore  = 0;
    private int   highScore;
    private float timeAccumulator = 0f;   // fractional seconds → score points

    private static final float SCORE_PER_SECOND      = 10f;
    private static final int   DODGE_BONUS            = 5;

    private final Preferences prefs;

    public ScoreManager() {
        prefs     = Gdx.app.getPreferences(PREFS_NAME);
        highScore = prefs.getInteger(HISCORE_KEY, 0);
    }

    /**
     * update() — called every frame to add time-based score.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        timeAccumulator += delta * SCORE_PER_SECOND;

        // Convert accumulated float score to whole integer increments
        int earned = (int) timeAccumulator;
        if (earned > 0) {
            currentScore    += earned;
            timeAccumulator -= earned;
        }
    }

    /** Called each time the player successfully dodges a falling object. */
    public void registerDodge() {
        currentScore += DODGE_BONUS;
    }

    /**
     * Persist the high score if the current session beat it.
     * Called when the game session ends.
     */
    public void saveHighScore() {
        if (currentScore > highScore) {
            highScore = currentScore;
            prefs.putInteger(HISCORE_KEY, highScore);
            prefs.flush();  // writes to disk immediately
        }
    }

    public void reset() {
        currentScore    = 0;
        timeAccumulator = 0f;
    }

    // ----- Accessors -----

    public int getCurrentScore() { return currentScore; }
    public int getHighScore()    { return highScore; }
    public boolean isNewHighScore() { return currentScore > 0 && currentScore >= highScore; }
}
