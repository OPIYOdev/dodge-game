package com.dodgegame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.dodgegame.screens.MenuScreen;

/**
 * DodgeGame - Main game class.
 *
 * Extends libGDX's Game class, which manages a stack of Screens.
 * This is the application entry point for all platforms.
 *
 * Key libGDX concepts demonstrated:
 *   - Game lifecycle (create, render, dispose)
 *   - Screen management for different game states
 *   - Shared resources (SpriteBatch, AssetManager)
 */
public class DodgeGame extends Game {

    // SpriteBatch is expensive to create — share one instance across all screens
    public SpriteBatch batch;

    // AssetManager handles async loading and reference-counted asset disposal
    public AssetManager assets;

    // Game configuration constants
    public static final String TITLE       = "Dodge the Falling Objects";
    public static final int    WORLD_WIDTH  = 800;
    public static final int    WORLD_HEIGHT = 600;

    @Override
    public void create() {
        batch  = new SpriteBatch();
        assets = new AssetManager();

        // Start at the main menu
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        // Always dispose of native resources to avoid memory leaks
        batch.dispose();
        assets.dispose();
    }
}
