package com.dodgegame.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.dodgegame.DodgeGame;

/**
 * DesktopLauncher - Entry point for the desktop (Windows / macOS / Linux) build.
 *
 * libGDX uses platform-specific backends; this class wires the core game
 * to the LWJGL 3 backend which provides an OpenGL window via GLFW.
 *
 * To run: gradle desktop:run
 * To package: gradle desktop:jar  (produces a fat JAR with all dependencies)
 */
public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle(DodgeGame.TITLE);
        config.setWindowedMode(DodgeGame.WORLD_WIDTH, DodgeGame.WORLD_HEIGHT);

        // Lock framerate to 60 fps using VSync
        config.useVsync(true);
        config.setForegroundFPS(60);

        // Window icon would be set here in a production game:
        // config.setWindowIcon("icon128.png", "icon64.png", "icon32.png");

        new Lwjgl3Application(new DodgeGame(), config);
    }
}
