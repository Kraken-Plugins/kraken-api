<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://kraken-plugins.com">
    <img src="src/main/resources/kraken.png" alt="Logo" width="128" height="128">
  </a>

<h3 align="center">Kraken API</h3>

  <p align="center">
   An API for building extended RuneLite plugins.
    <br />
</div>

[![Release Kraken API](https://github.com/Kraken-Plugins/kraken-api/actions/workflows/release.yml/badge.svg?branch=master)](https://github.com/Kraken-Plugins/kraken-api/actions/workflows/release.yml)
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/runewraith)

---

# Getting Started

Kraken API is designed to extend the RuneLite API with additional client interaction utilities for writing RuneLite plugins with extended functionality.
This API is not meant for "botting", reverse engineering, automation or rule-breaking purposes, and any material provided by this API or its documentation is purely for 
educational purposes only.

Use at your own risk. The developers are not responsible for any consequences resulting from the use of this software.

## Quick Start

No GitHub account or token is needed. The API jar is attached to every [GitHub release](https://github.com/Kraken-Plugins/kraken-api/releases)
and Gradle can download it from there directly.

**1. Start from a normal RuneLite plugin project.** If you don't have one yet, use RuneLite's
[example plugin template](https://github.com/runelite/example-plugin) (click "Use this template", then open it in IntelliJ).
You'll need [Java 17+](https://adoptium.net/).

**2. Add the Kraken API to your `build.gradle`.** Add the `ivy` repository and the two `kraken-api` lines below to what the
template already has. Set `krakenApiVersion` to the latest release:
[![Latest release](https://img.shields.io/github/v/release/Kraken-Plugins/kraken-api?label=latest)](https://github.com/Kraken-Plugins/kraken-api/releases/latest)

```groovy
def runeLiteVersion = 'latest.release'
def krakenApiVersion = 'X.Y.Z' // the latest release, e.g. 5.1.6

repositories {
    mavenLocal()
    maven { url = 'https://repo.runelite.net' }
    mavenCentral()

    // Downloads the Kraken API from its GitHub releases. No token required.
    ivy {
        name = 'KrakenApiReleases'
        url = 'https://github.com/Kraken-Plugins/kraken-api/releases/download/'
        patternLayout { artifact '[revision]/[module]-[revision](-[classifier]).[ext]' }
        metadataSources { artifact() }
        content { includeModule 'com.github.kraken', 'kraken-api' }
    }
}

dependencies {
    compileOnly group: 'net.runelite', name: 'client', version: runeLiteVersion
    compileOnly group: 'com.github.kraken', name: 'kraken-api', version: krakenApiVersion

    compileOnly 'org.projectlombok:lombok:1.18.30'
    annotationProcessor 'org.projectlombok:lombok:1.18.30'

    // Puts RuneLite and the API on the classpath when you launch the client from the test runner below.
    testImplementation group: 'net.runelite', name: 'client', version: runeLiteVersion
    testImplementation group: 'com.github.kraken', name: 'kraken-api', version: krakenApiVersion
}
```

**3. Inject the API into your plugin.**

```java
@PluginDescriptor(name = "Example")
public class ExamplePlugin extends Plugin {

    @Inject
    private Context ctx; // com.kraken.api.Context, the entry point to the whole API

    @Subscribe
    private void onGameTick(GameTick event) {
        if (ctx.inventory().isFull()) {
            ctx.gameObjects().withName("Bank booth").nearest().ifPresent(booth -> booth.interact("Bank"));
        }
    }
}
```

**4. Run it.** Run the template's test class (`src/test/java/.../ExamplePluginTest.java`). It starts RuneLite with your
plugin loaded:

```java
public class ExamplePluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(ExamplePlugin.class);
        RuneLite.main(args);
    }
}
```

That's it. See [API Usage](#api-usage) for more of what the API can do. Point an AI coding agent at
[`llms-full.txt`](#ai-agents-and-llmstxt) so it knows the whole API too.

> When your plugin is ready, build it with `./gradlew jar` and drop the jar into `~/.runelite/kraken/sideloaded-plugins`.
> The Kraken client provides RuneLite and the Kraken API at runtime, which is why both are `compileOnly` above.

## API Usage

The following RuneLite "plugin" is purely for an example of the API's capabilities:

```java
@PluginDescriptor(
        name = "Example",
        description = "Example plugin"
)
public class ExamplePlugin extends Plugin {
    
    @Inject
    private Context ctx;
    
    @Inject
    private BankService bank;
    
    @Inject
    private MovementService movement;
    
    @Inject
    private PrayerService prayer;
    
    @Subscribe
    private void onGameTick(GameTick e) {
      Player local = ctx.players().local().raw();
      
      if(local.isInteracting()) {
          return;
      }
      
      if(!bank.isOpen()) {
          // Open a bank
          ctx.gameObjects().withName("Bank booth").sortByDistance().interact("Open");
      } else {
          // Withdraw a Rune Scimitar
          ctx.bank().nameContains("Rune scimitar").first().ifPresent(item -> item.withdraw(1));
      }
      
      // Wield the Rune Scimitar from the inventory
      ctx.inventory().withId(1333).interact("Wield");
      
      // Move to a new position
      movement.moveTo(new WorldPoint(3253, 3420, 0));
      
      // Activate a protection prayer
      prayer.activatePrayer(Prayer.PROTECT_FROM_MELEE);
      
      // "Click" on a Goblin and attack it.
      ctx.npcs().withName("Goblin")
            .except(n -> n.raw().isInteracting())
            .sortByDistance()
            .interact("Attack");
      
      
      // Take the goblin bones
      ctx.groundItems().withName("Bones")
              .reachable()
              .within(5)
              .nearest()
              .ifPresent(GroundObjectEntity::take);
      
      // Bury the bones
      ctx.inventory().withName("Bones").interact("Bury");
    }
}
```

To use the API in an actual RuneLite plugin, you should check out the [Kraken Example Plugins](https://github.com/cbartram/kraken-example-plugin)
which shows the best practice usage of the API within an actual plugin.
To set up your development environment for running plugins, we recommend following [this guide on RuneLite's Wiki](https://github.com/runelite/runelite/wiki/Building-with-IntelliJ-IDEA).

Once you have the example plugin cloned and setup within Intellij, you can run the main class in `src/test/java/PluginRunnerTest.java plugins.api.ApiTestPlugin` to run RuneLite with
the example plugin loaded in the plugin panel within RuneLite's sidebar. See the [Quick Start](#quick-start) for integrating the API into
your own plugin's build.

![example-plugin](./images/example-plugin-2.png)

> If you are just looking to use pre-existing plugins, you can skip this repository and head over to our website: [kraken-plugins.com](https://kraken-plugins.com). 
> For more documentation on the API and Kraken plugins, please see our [official documentation here](https://kraken-plugins.com/docs/).

### Prerequisites
- [Java 17+](https://adoptium.net/) (JDK required)
- [Gradle](https://gradle.org/) (wrapper included, no need to install globally)
- [Git](https://git-scm.com/)
- [RuneLite](https://runelite.net) (for testing and running plugins)

### Building from source

You only need this to work on the API itself or to test unreleased changes. To build the project with Gradle:

```bash
./gradlew clean build publishToMavenLocal shadowJar

# Optionally you can set a specific version to build
export VERSION=5.0.0-SNAPSHOT-LOCAL
./gradlew clean build publishToMavenLocal shadowJar

# Will be found here:
# ~/.m2/repository/com/github/kraken/kraken-api/5.0.0-SNAPSHOT-LOCAL/kraken-api-5.0.0-SNAPSHOT-LOCAL.jar
```

The output API `.jar` can be found in your `~/.m2/repository/com/github/kraken/kraken-api` directory and the default version is `1.0.0`.
Include the JAR file in your local plugins project with:

```groovy
repositories {
  mavenLocal() // Ensure this is included to pull from the locally built API JAR
  mavenCentral()
  maven {
    url = 'https://repo.runelite.net'
  }
}

dependencies {
    compileOnly group: 'com.github.kraken', name: 'kraken-api', version: '1.0.0' // or whichever version you built with when export VERSION=...
}
```

This also builds the shaded jar that is published and loaded by the Kraken client. It bundles the `shortest-path` pathfinding library and its data;
RuneLite, Guice, Guava, Gson, SLF4J and Lombok are `compileOnly` and provided by RuneLite at runtime. It is located in:

```shell
build/libs/kraken-api-<version>.jar
```

## Other ways to get the API

### Downloading the jar directly

Every [release](https://github.com/Kraken-Plugins/kraken-api/releases) attaches the API jar, its sources and javadoc jars,
and the [llms.txt files](#ai-agents-and-llmstxt) as public assets:

- Pinned: `https://github.com/Kraken-Plugins/kraken-api/releases/download/<version>/kraken-api-<version>.jar` (also `-sources.jar` / `-javadoc.jar`)
- Latest: `https://github.com/Kraken-Plugins/kraken-api/releases/latest/download/kraken-api.jar` (also `kraken-api-sources.jar` / `kraken-api-javadoc.jar`)

Put the jar in your project's `libs/` folder and add `compileOnly files('libs/kraken-api.jar')` if you'd rather not use the
`ivy` repository from the [Quick Start](#quick-start). The `ivy` repository needs an exact release version; dynamic versions
like `5.+` don't work with it.

### AI agents and llms.txt

Each release also attaches [`llms.txt`](https://llmstxt.org) files, which are hosted at
[kraken-plugins.com/llms.txt](https://kraken-plugins.com/llms.txt) as well:

- `llms.txt`: setup steps, a minimal plugin, the key rules, and links to every guide
- `llms-full.txt`: the same overview plus every plugin-author guide and the full API signatures in one file
- `llms-api.txt`: every public type and member (including Lombok-generated getters and builders) as Java stubs with one-line Javadoc summaries

Point a coding agent at `https://github.com/Kraken-Plugins/kraken-api/releases/latest/download/llms-full.txt` to give it
everything it needs to write a plugin. To regenerate them locally, run `VERSION=X.Y.Z ./gradlew generateLlmsTxt` (output in `build/llms`).

### GitHub Packages

The API is also published to GitHub Packages, which requires authentication even for public packages. To use it, either:
- `export GITHUB_ACTOR=<YOUR_GITHUB_USERNAME>; export GITHUB_TOKEN=<GITHUB_PAT`
- or add the following to your `gradle.properties` file: `gpr.user=your-github-username gpr.key=your-personal-access-token`

More information on generating a GitHub Personal Access token can [be found below](#authentication).

###  Authentication

Since the API packages are hosted on [GitHub Packages](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages) you will
need to generate a [Personal Access Token (PAT)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens?versionId=free-pro-team%40latest&productId=packages&restPage=learn-github-packages%2Cintroduction-to-github-packages) on GitHub
to authenticate and pull down the API.

You can generate a GitHub PAT by navigating to your [GitHub Settings](https://github.com/settings/personal-access-tokens)
and clicking "Generate new Token." Give the token a unique name and optional description with read-only access to public repositories. Store the token
in a safe place as it won't be viewable again. It can be used to authenticate to GitHub and pull Kraken API packages.

> :warning: Do **NOT** share this token with anyone.

```groovy
plugins {
    id 'java'
    id 'application'
}


// Replace with the package version of the API you need
def krakenApiVersion = 'X.Y.Z'

// Alternatively, you can use: `+` or `2.2.+` (for example) as the krakenApiVersion to float on the latest version within a safe boundary
// so you don't have to constantly bump the version when new API changes are released!

allprojects {
    apply plugin: 'java'
    repositories {
        // You must declare this maven repository to be able to search and pull Kraken API packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Kraken-Plugins/kraken-api")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }

        // Jitpack is an alternative means of accessing the API Jar file
        maven { url 'https://jitpack.io' }
    }
}


dependencies {
    compileOnly group: 'com.github.kraken', name:'kraken-api', version: krakenApiVersion
    // ... other dependencies
}
```

## API Design & Methodology

For more information around how the API is designed, please see [API docs](docs/API.md)

## Scripting

For more information on writing scripts using the Kraken API,  
check out the detailed [scripting guide](docs/SCRIPTING.md).

## Mouse Movement

For more information on mouse movement in the API check out the
detailed [mouse movement guide](docs/MOUSE.md)

## Utilities

The Kraken API also ships with a variety of useful utilities for plugins from logging, mouse, and table overlays to
custom RuneLite events, randomization, math and string methods! To learn more about
Kraken's extra utilities, check out [this doc](docs/UTILITIES.md).

## Simulation

For information on how to use Kraken's API to simulate game outcomes,  
see the [simulation docs](docs/SIMULATION.md).

To see an example plugin using the simulation API, you can run the main class in:

```
src/test/java/PluginRunnerTest.java plugins.simulation.SimulationPlugin
```

to load an example simulation plugin alongside RuneLite.

![sim-example-image](images/sim.png)


### Colosseum Simulator 

There is a separate port of the [Colosseum Line of Sight Simulation](https://los.colosim.com/) to Java contained in the repository's test sources for reference
that can be run in its own GUI from the `colosim` package under `src/test/java`. It is not part of the published jar.

![colosseum-sim](./images/colosim.png)

## Game Updates

Please see the game updates and [how to update the API guide](docs/UPDATING.md) for more detailed information.

## Running Tests

Please see the [testing guide](docs/TESTS.md) for more information on running tests.

## Development Workflow

Clone this repository with: `git clone --recurse-submodules https://github.com/Kraken-Plugins/kraken-api.git` to ensure
that all submodules (shortest-path plugin) are cloned as well.

1. Create a new branch from `master`
2. Implement or update your plugin/feature for the API
3. Add tests for new functionality
4. Run `./gradlew clean build publishToMavenLocal shadowJar` to verify that the API builds and tests pass
5. Commit your changes with a clear message `git commit -m "feat(api): Add feature X to Kraken API"`
6. Open a Pull Request

---

## Deployment

The Kraken API is automatically built and deployed via GitHub actions on every push to the `master` branch.
The latest version can be found in the [releases](https://github.com/Kraken-Plugins/kraken-api/releases) section of the repository.

The deployment is fully automated and consists of:

-  Building the API JAR
- Publishing a new version to the GitHub Releases section
  - This will be picked up by Github Packages for easy integration into other gradle projects.
- Uploading the JAR file to the SeaweedFS storage server used by the Kraken Client at runtime.

---

## 🛠 Built With

* [Java](https://www.java.org/) — Core language
* [Gradle](https://gradle.org/) — Build tool
* [RuneLite](https://runelite.net) — Used for as the backbone for the API

---

## Buy me a coffee

If you enjoy the API and want to support the development of the project, please consider buying me a coffee!

[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/runewraith)

---

## 🤝 Contributing

Fork the repository, branch from `master`, and open a pull request following the [development workflow](#development-workflow) above. Sign your commits with `git commit -s`.

If you'd like to see the work in our backlog, check out this [project board](https://github.com/orgs/Kraken-Plugins/projects/1/views/1).

---

## 🔖 Versioning

We use [Semantic Versioning](http://semver.org/).
See the [tags on this repository](https://github.com/Kraken-Plugins/kraken-api/tags) for available releases.

CI will automatically bump the patch version on each merge to master i.e. `1.1.4` -> `1.1.5`. If you want to bump 
a minor or major version then update the `version.txt` file in the root of the repository with the new version you
want to use as a base.

For example, moving from: `1.3.5` -> `1.4.0` the `version.txt` should be `1.4.0`. The next automatic build will use `1.4.1` then `1.4.2` etc...
until `version.txt` is updated to `1.5.0` or `2.0.0`.

---

## 📜 License

This project is licensed under the [GNU General Public License 3.0](LICENSE).

---

## 🙏 Acknowledgments

* **RuneLite** — For API's to work with and view in game data for Old School RuneScape
* **Packet Utils** – [Plugin](https://github.com/Ethan-Vann/PacketUtils) from Ethan Vann providing access to complex packet sending functionality which was used to develop the `core.packet` package of the API
* **Vitalite** – Vitalite for showing some incredible open source examples of dialogue, GE interactions, packets, mouse movement, and just working with the client in general
* **VitaLite Mappings** – Huge shoutout for the VitaLite devs to maintain and publish these mappings for obfuscated classes and methods
* **Microbot** — For clever ideas on client and plugin interaction using reflection.
* **[Lucid](https://github.com/lucid-plugins/SideloadPlugins) & [Kotori](https://github.com/OreoCupcakes/kotori-plugins/blob/master/kotoriutils/src/main/java/com/theplug/kotori/kotoriutils/rlapi/table/TableComponent.java) plugins** — For their open source implementation on the Table UI element.

[contributors-shield]: https://img.shields.io/github/contributors/Kraken-Plugins/kraken-api.svg?style=for-the-badge
[contributors-url]: https://github.com/Kraken-Plugins/kraken-api/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/Kraken-Plugins/kraken-api.svg?style=for-the-badge
[forks-url]: https://github.com/Kraken-Plugins/kraken-api/network/members
[stars-shield]: https://img.shields.io/github/stars/Kraken-Plugins/kraken-api.svg?style=for-the-badge
[stars-url]: https://github.com/Kraken-Plugins/kraken-api/stargazers
[issues-shield]: https://img.shields.io/github/issues/Kraken-Plugins/kraken-api.svg?style=for-the-badge
[issues-url]: https://github.com/Kraken-Plugins/kraken-api/issues
[coffee-url]: https://ko-fi.com/runewraith

