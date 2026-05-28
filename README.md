# 🚀 Dodge the Falling Objects — libGDX Portfolio Sample

A complete 2D game built with **Java + libGDX**, demonstrating the core pillars of game development:
game loop, sprite-like entity rendering, input handling, collision detection, particle effects, and
persistent high-score storage.

---

## 📁 Project Structure

```
dodge-game/
├── core/                        ← Platform-agnostic game logic (shared by all platforms)
│   └── src/com/dodgegame/
│       ├── DodgeGame.java           ← Main Game class; owns shared resources (SpriteBatch, AssetManager)
│       ├── entities/
│       │   ├── Player.java          ← Player entity: input, movement, AABB bounds, animation
│       │   └── FallingObject.java   ← Hazard entity: physics, shape variety, warning flash
│       ├── managers/
│       │   ├── ObjectSpawner.java   ← Spawns and pools FallingObjects; scales difficulty over time
│       │   ├── ScoreManager.java    ← Time-based score + dodge bonus; persists high score via Preferences
│       │   └── ParticleManager.java ← Pool-based particle system for explosions and dodge streaks
│       └── screens/
│           ├── MenuScreen.java      ← Animated main menu with decorative falling shapes
│           └── GameScreen.java      ← Core gameplay: game loop, collision, HUD, screen-shake
│
├── desktop/                     ← LWJGL3 desktop backend (Windows / macOS / Linux)
│   └── src/com/dodgegame/desktop/
│       └── DesktopLauncher.java     ← main() entry point; configures window & framerate
│
├── build.gradle                 ← Root Gradle config; defines libGDX version & dependencies
├── settings.gradle              ← Declares subprojects (core, desktop)
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

---

## 🎮 How to Play

| Key | Action |
|-----|--------|
| **WASD** / **Arrow Keys** | Move your ship |
| **SPACE** / **ENTER** | Start game (from menu) |
| **SPACE** / **R** | Restart (after game over) |
| **ESC** | Return to menu |

- Survive as long as possible while avoiding the falling shapes.
- You have **3 lives** — each collision costs one.
- Score accumulates automatically over time **+ 5 bonus points** for every object you dodge past.
- Difficulty ramps up continuously: objects fall faster and spawn more frequently.

---

## 🛠️ Setup & Running

### Prerequisites

| Tool | Version |
|------|---------|
| JDK  | 11 or higher |
| Gradle | 7.6 (via wrapper) |

### Run (desktop)

```bash
# Clone / unzip the project, then:
cd dodge-game
./gradlew desktop:run          # macOS / Linux
gradlew.bat desktop:run        # Windows
```

### Build a distributable JAR

```bash
./gradlew desktop:jar
# Output: desktop/build/libs/DodgeGame-desktop-1.0.jar
java -jar desktop/build/libs/DodgeGame-desktop-1.0.jar
```

---

## 🏗️ Architecture & Key Concepts

### 1. Game Loop (GameScreen.render)

```
render(delta)
  └─ update(delta)          ← advance simulation
  │    ├─ player.update()
  │    ├─ spawner.update()
  │    ├─ scoreManager.update()
  │    ├─ particles.update()
  │    ├─ checkCollisions()  ← AABB tests
  │    └─ checkDodges()      ← bonus scoring
  └─ draw()                  ← render all objects
       ├─ ShapeRenderer pass (shapes, particles, player)
       └─ SpriteBatch pass   (HUD text)
```

**Delta-time movement** ensures physics runs at the same speed on any hardware:
```java
bounds.x += speedX * delta;   // pixels-per-second × seconds = pixels
```

### 2. Screen Management

libGDX's `Game` class owns a single active `Screen`. Transitioning is one method call:
```java
game.setScreen(new GameScreen(game));
```
Each screen owns its own resources and disposes them in `dispose()`.

### 3. Collision Detection (AABB)

Each entity exposes a `Rectangle bounds`. Overlap is tested with libGDX's built-in method:
```java
if (fallingObject.getBounds().overlaps(player.getBounds())) { handleHit(); }
```
`Rectangle.overlaps()` checks the four axis-aligned conditions — O(1) per pair.

### 4. Object Pooling (ObjectSpawner + ParticleManager)

Allocating and garbage-collecting hundreds of objects per second causes GC pauses.
Instead, we maintain a **free list** and reset/reuse objects:
```java
// Return to pool instead of letting GC collect
active.removeIndex(i);
pool.add(particle);

// Next spawn: reset and re-add rather than `new`
Particle p = pool.removeIndex(pool.size - 1);
p.reset(...);
active.add(p);
```

### 5. Difficulty Scaling

```java
// spawnInterval decreases over time, clamped to a minimum
spawnInterval = Math.max(MIN_INTERVAL, INITIAL_INTERVAL - elapsed * DIFFICULTY_RATE);

// object fall speed increases over time, clamped to a maximum
float speed = Math.min(MAX_SPEED, BASE_SPEED + elapsed * SPEED_SCALE);
```

### 6. Persistent High Score (libGDX Preferences)

`Gdx.app.getPreferences()` maps to the platform-appropriate storage:
- **Desktop** → `~/.prefs/dodge_game_prefs`
- **Android** → `SharedPreferences`
```java
prefs.putInteger("high_score", newHighScore);
prefs.flush();   // write to disk
```

### 7. Screen Shake

A decaying random camera offset creates a hit-feedback effect without modifying any entity position:
```java
float t  = shakeTimer / SHAKE_DURATION;          // 1 → 0 over duration
float dx = (random * 2 - 1) * shakeMagnitude * t; // random, decaying offset
camera.position.set(worldCX + dx, worldCY + dy, 0);
```

---

## 🎨 Rendering Approach

The game uses **ShapeRenderer** (OpenGL primitives) rather than textures to keep the project
dependency-free and focus on code concepts. In a production game you would swap these for:

- `SpriteBatch` + `TextureAtlas` (sprite sheets)
- `ParticleEffect` (libGDX's built-in particle editor output)
- `Scene2D` (for menus and UI)

---

## 📈 Extending the Project

| Idea | Where to start |
|------|---------------|
| Add power-ups (shield, slow-mo) | New entity class + collision branch in GameScreen |
| Mobile touch input | Add touch handling in Player.handleInput() |
| Sound effects | Load `.ogg` files via `AssetManager`; play on hit/dodge |
| Leaderboard | Replace Preferences with a REST API call |
| Multiple levels | Add a `LevelManager` that changes background/speed profile |
| Texture-based sprites | Replace ShapeRenderer calls with `SpriteBatch.draw(TextureRegion, ...)` |

---

## 📚 libGDX Resources

- [Official documentation](https://libgdx.com/wiki/)
- [libGDX GitHub](https://github.com/libgdx/libgdx)
- [Game dev patterns](https://gameprogrammingpatterns.com/) — covers many concepts used here

---

## 📄 License

MIT — free to use, modify, and redistribute for any purpose.
