# FlameQuest

FTB-style quest mod for Fabric 1.20.1 by FlameFragger45.

## How to compile

**Requirements:** Java 17, internet access (first build downloads Gradle + MC dependencies)

```bash
# Download Gradle wrapper first (one-time)
gradle wrapper --gradle-version=8.1.1

# Build the mod JAR
./gradlew build
```

The compiled JAR will be at: `build/libs/flamequest-1.0.0.jar`

Upload that JAR to Modrinth.

## How to add quests

Drop JSON files into `data/flamequest/quests/` in the mod JAR (or a datapack).

**Objective types:** `kill`, `collect`, `craft`
**Reward types:** `item`, `xp`

See the included sample quests for the format.

## Crafting the Quest Book

Craft: Leather + Book (shapeless) — or give via `/give @p flamequest:quest_book`

> Note: Add the shapeless recipe JSON under `data/flamequest/recipes/` if you want the craft recipe.

## In-game

Right-click the Quest Book to open the quest GUI.
Quest progress saves to the world automatically.
