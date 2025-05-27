# LibGDX Game Project

A simple game project using LibGDX framework.

## Prerequisites

- Java Development Kit (JDK) 17 or later
- Gradle build tool (included via wrapper)

## Project Structure

```
src/main/java/com/example/
├── MyGame.java              # Main game class
├── desktop/                 # Desktop-specific code
│   └── DesktopLauncher.java
└── screens/                 # Game screens
    └── MainGameScreen.java
```

## Building and Running

1. Clone the repository
2. Navigate to the project directory
3. Run the game:
   ```bash
   ./gradlew run
   ```

## Development

- The main game logic is in `MyGame.java`
- Game screens are in the `screens` package
- Desktop-specific configuration is in `DesktopLauncher.java`

## Dependencies

- LibGDX 1.12.1
- Box2D Physics Engine 