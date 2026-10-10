# Kraken API

> Kraken API is a Java 17 library that extends the RuneLite (Old School RuneScape client) plugin API with fluent entity
> queries, game-system services, menu-action and packet interactions, long-running scripts, mouse movement and world
> walking. Plugins built with it are ordinary RuneLite plugins that run inside the Kraken client, which provides the
> API at runtime.

This file describes Kraken API {{VERSION}}. Release jars are public GitHub release assets, so downloading them needs no
GitHub token or account.

## Getting the jar

- Pinned version: `https://github.com/Kraken-Plugins/kraken-api/releases/download/{{VERSION}}/kraken-api-{{VERSION}}.jar`
  (also `-sources.jar` and `-javadoc.jar` with the same prefix)
- Always the newest release: `https://github.com/Kraken-Plugins/kraken-api/releases/latest/download/kraken-api.jar`
  (also `kraken-api-sources.jar` and `kraken-api-javadoc.jar`)
- Newest version number: the `tag_name` field of `https://api.github.com/repos/Kraken-Plugins/kraken-api/releases/latest`

To read the API source directly, download the sources jar and unzip it:

```shell
curl -L -o kraken-api-sources.jar https://github.com/Kraken-Plugins/kraken-api/releases/latest/download/kraken-api-sources.jar
unzip -q kraken-api-sources.jar -d kraken-api-sources
```

## Gradle setup

Install the Kraken client first: install RuneLite (https://runelite.net), then download
https://seaweed.kraken-plugins.com/kraken-bootstrap-static/KrakenLauncher-Windows.zip, unzip it and run
`KrakenInstaller.exe`. The Kraken Gradle plugin (https://github.com/Kraken-Plugins/kraken-gradle-plugin) then launches
that client with the plugin being built sideloaded. Both the Gradle plugin and the Kraken API come from the public
Maven repository `https://repo.kraken-plugins.com`, which needs no token.

```groovy
// settings.gradle
pluginManagement {
    repositories {
        maven { url = 'https://repo.kraken-plugins.com' }
        gradlePluginPortal()
    }
}

rootProject.name = 'my-plugin'
```

```groovy
// build.gradle
plugins {
    id 'com.krakenplugins.plugin' version '0.1.1'
}

def krakenApiVersion = '{{VERSION}}'

repositories {
    maven { url = 'https://repo.runelite.net' }
    maven { url = 'https://repo.kraken-plugins.com' }
    mavenCentral()
}

dependencies {
    // The Kraken client provides RuneLite and the Kraken API at runtime, so both are compileOnly.
    compileOnly 'net.runelite:client:latest.release'
    compileOnly "com.github.kraken:kraken-api:$krakenApiVersion"

    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'
}
```

The Gradle plugin applies `java` and adds two tasks:

- `./gradlew runKraken` builds the plugin jar and launches the installed Kraken client with it sideloaded.
  `--debug-jvm` waits for a debugger on port 5005; `--profile <name>` logs in as a linked Jagex profile. On Linux, set
  `kraken { runeliteDirectory = file('/path/to/RuneLite') }` to the folder holding `RuneLite.jar`.
- `./gradlew krakenVersions` checks that the Kraken API the project compiles against works with the one the client
  runs. Compiling against a newer API than the client runs causes `NoSuchMethodError` at runtime.

Without Gradle, download the jar into `libs/` and use `compileOnly files('libs/kraken-api.jar')`.

## Minimal plugin

```java
import com.google.inject.Inject;
import com.kraken.api.Context;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "Example", description = "Banks at the nearest booth")
public class ExamplePlugin extends Plugin {

    @Inject
    private Context ctx;

    @Subscribe
    private void onGameTick(GameTick event) {
        if (ctx.inventory().isFull()) {
            ctx.gameObjects().withName("Bank booth").nearest().ifPresent(booth -> booth.interact("Bank"));
        }
    }
}
```

For anything longer than a few actions, extend `com.kraken.api.core.script.Script` (or split the work into
`AbstractTask` subclasses that the script loops over), call `script.start()` in the plugin's `startUp()` and
`script.stop()` in `shutDown()`. The scripting guide below covers this in detail.

## Key rules

- The API only works inside a running RuneLite/Kraken client. It is injected with Guice: `@Inject Context ctx` or any
  service class (for example `BankService`, `PrayerService`, `Walker`). Do not construct services with `new`.
- `com.kraken.api.Context` is the entry point. It exposes the queries (`npcs()`, `players()`, `gameObjects()`,
  `groundItems()`, `inventory()`, `bank()`, `equipment()`, `widgets()`, `worlds()` and more) and client-thread helpers.
- Queries (`com.kraken.api.query`) are for things you filter and pick from: chain filters such as `withName`,
  `withId`, `nameContains`, `within`, `reachable`, `except` and `filter`, then end with `first()`, `nearest()`,
  `random()` (each returns `Optional`), `list()`, or act on the matches with `interact(...)`. Every entity's `raw()`
  returns the underlying RuneLite object.
- Services (`com.kraken.api.service`) are for singular game systems: bank interface, prayer, magic, dialogue, camera,
  movement, walker, grand exchange, shops and UI.
- `Script.loop()` runs off the RuneLite client thread. Read or change client state from it through
  `ctx.runOnClientThread(...)` or the query and service methods, which handle threading for you.
- `interact(...)` calls go through the client's menu-action handler and do not move the mouse. Use `VirtualMouse`
  when the cursor should visibly move.
- Package a plugin as a jar. RuneLite, Guice, Guava, Gson, SLF4J, Lombok and the Kraken API are provided at runtime;
  shade any other dependency into the plugin jar. During development `./gradlew runKraken` loads it; to install a
  built jar (`./gradlew jar`, in `build/libs`), put it in `~/.runelite/kraken/sideloaded-plugins` and restart the
  Kraken client.

## Example plugins

Complete plugins that use the API (mining, woodcutting, fishing, firemaking, runecrafting, jewelry, combat):
https://github.com/Kraken-Plugins/kraken-example-plugin. Its
`./gradlew runKraken` launches the Kraken client with every example plugin sideloaded.
