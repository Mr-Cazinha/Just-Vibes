# Just Vibes - Multiplayer Top-Down Shooter

A fast-paced multiplayer top-down shooter game built with LibGDX and UDP networking. Players compete in a city environment, trying to be the first to reach 10 kills.

## Features

- **Multiplayer Combat**: Real-time multiplayer gameplay using UDP networking
- **City Environment**: Urban setting with buildings and roads
- **Scoring System**: First player to 10 kills wins
- **Respawn System**: Players respawn at random locations after death (20-30 seconds)
- **Health System**: Players have health bars and take damage from bullets
- **Score Display**: Real-time score tracking displayed on the left side of the screen

## Prerequisites

- Java Development Kit (JDK) 17 or later
- Gradle (included in the wrapper)
- A Java IDE (recommended: IntelliJ IDEA or Eclipse)

## Getting Started

### Setting Up the Development Environment

1. Clone the repository:
   ```bash
   git clone https://github.com/Mr-Cazinha/Just-Vibes.git
   cd Just-Vibes
   ```

2. Build the project:
   ```bash
   ./gradlew build
   ```

### Running the Game

1. Start the Server:
   ```bash
   ./gradlew run --args="server"
   ```

2. Start the Client:
   ```bash
   ./gradlew run --args="client"
   ```

For multiple players, run additional client instances on different machines or ports.

## Game Controls

- **Movement**: WASD keys
  - W: Move up
  - A: Move left
  - S: Move down
  - D: Move right
- **Shooting**: Spacebar
- **Aim**: Character automatically aims in the direction of movement

## Network Architecture

- Server runs on port 7777 (UDP)
- Clients connect to server using IP address
- UDP-based networking for fast-paced gameplay
- Server handles:
  - Player connections/disconnections
  - Position synchronization
  - Combat mechanics
  - Score tracking
  - Respawn management

## Game Mechanics

### Combat
- Players shoot bullets that deal 20 damage
- Players have 100 health points
- Death occurs when health reaches 0
- Shooting has a 0.5-second cooldown

### Scoring
- Kill a player: +1 point
- First to 10 points wins
- Scores displayed on the left side
- Your score is highlighted in green
- Winner announcement in gold

### Respawn System
- Random spawn points away from roads
- 20-30 second respawn timer
- Server-managed respawn queue
- Safe spawn locations to prevent spawn camping

## Building from Source

1. Clone the repository
2. Install JDK 17 or later
3. Run `./gradlew build`
4. Find the built JAR in `build/libs/`

## Project Structure

```
src/main/java/com/example/
├── desktop/         # Desktop launcher
├── entities/        # Game entities (Player, Bullet)
├── game/           # Game screens and core logic
├── map/            # City map and background
├── network/        # Networking (GameClient)
├── screens/        # Game screens
├── server/         # Server implementation
└── ui/             # User interface components
```

## Technical Details

- **Game Engine**: LibGDX 1.12.1
- **Networking**: UDP (DatagramSocket)
- **Build System**: Gradle
- **Language**: Java 17
- **Dependencies**:
  - LibGDX Core
  - LibGDX LWJGL3 Backend
  - LibGDX Box2D
  - JSON Library

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to your branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 