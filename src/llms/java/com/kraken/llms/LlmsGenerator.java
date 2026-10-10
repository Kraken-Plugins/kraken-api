package com.kraken.llms;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.tools.DiagnosticCollector;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the {@code llms.txt} family of files (see https://llmstxt.org) for the Kraken API:
 * <ul>
 *     <li>{@code llms.txt} - the overview plus links to every guide and to the two files below</li>
 *     <li>{@code llms-full.txt} - the overview, every plugin-author guide and the API signatures in one file</li>
 *     <li>{@code llms-api.txt} - only the public API signatures</li>
 * </ul>
 * Run it with {@code ./gradlew generateLlmsTxt}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LlmsGenerator {

    private static final String REPOSITORY_URL = "https://github.com/Kraken-Plugins/kraken-api";
    private static final String RAW_URL = "https://raw.githubusercontent.com/Kraken-Plugins/kraken-api";

    // Guides written for plugin authors, in reading order. These are inlined into llms-full.txt.
    private static final Map<String, String> GUIDES = new LinkedHashMap<>();

    // Guides for people working on the API itself. These are only linked from llms.txt.
    private static final Map<String, String> CONTRIBUTOR_GUIDES = new LinkedHashMap<>();

    // Files under docs/ that are not guides.
    private static final List<String> EXCLUDED_DOCS = List.of("index.md");

    static {
        GUIDES.put("API.md", "Services vs. queries, the Context entry point and how to use each query and service");
        GUIDES.put("SCRIPTING.md", "Script lifecycle, the game-tick loop, tasks, pausing and break handling");
        GUIDES.put("INTERACTION.md", "How interactions reach the client through doAction and packets");
        GUIDES.put("WALKER.md", "Walking anywhere in the world, including doors, shortcuts, boats and teleports");
        GUIDES.put("SHOPS.md", "Opening NPC shops, reading stock and buying or selling under limits");
        GUIDES.put("MOUSE.md", "VirtualMouse and the instant, linear, Bezier, wind and replay movement strategies");
        GUIDES.put("UTILITIES.md", "Overlay and utility helpers for plugins");

        CONTRIBUTOR_GUIDES.put("UPDATING.md", "Updating the API after RuneLite releases and client revisions");
        CONTRIBUTOR_GUIDES.put("TESTS.md", "Running the in-client API test harness");
        CONTRIBUTOR_GUIDES.put("SIMULATION.md", "The Colosseum simulators kept in the API repository");
    }

    /**
     * Entry point.
     * @param args {@code --source <dir> --docs <dir> --classpath <path> --output <dir> --version <version>
     *             [--git-ref <ref>] [--site-url <url>]}
     * @throws IOException When a source or output file cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> options = parseOptions(args);
        Path sourceDirectory = Path.of(require(options, "--source"));
        Path docsDirectory = Path.of(require(options, "--docs"));
        Path outputDirectory = Path.of(require(options, "--output"));
        String classpath = require(options, "--classpath");
        String version = require(options, "--version");
        String gitRef = options.getOrDefault("--git-ref", version);
        String siteUrl = options.getOrDefault("--site-url", "https://kraken-plugins.com").replaceAll("/+$", "");

        Files.createDirectories(outputDirectory);
        Path signaturesFile = outputDirectory.resolve("llms-api.txt");
        String signatures = generateSignatures(sourceDirectory, classpath, signaturesFile);

        String overview = Files.readString(docsDirectory.resolve("llms").resolve("overview.md"), StandardCharsets.UTF_8)
                .replace("{{VERSION}}", version)
                .strip();

        Map<String, String> guideDescriptions = new LinkedHashMap<>(GUIDES);
        Map<String, String> contributorDescriptions = new LinkedHashMap<>(CONTRIBUTOR_GUIDES);
        for (String unlisted : unlistedDocs(docsDirectory)) {
            contributorDescriptions.put(unlisted, title(docsDirectory.resolve(unlisted)));
        }

        String signaturesHeader = "# Kraken API " + version + " public API signatures\n\n"
                + "> Every public and protected type and member of the Kraken API as Java stubs, grouped by package.\n"
                + "> Each member is preceded by the first sentence of its Javadoc. Type names are written without their\n"
                + "> package: Kraken types live in the package they are listed under, and the rest are JDK types or\n"
                + "> RuneLite types from net.runelite.api and net.runelite.client. Members generated by Lombok\n"
                + "> (getters, setters, constructors and builders) are included.\n\n";
        Files.writeString(signaturesFile, signaturesHeader + "```java\n" + signatures + "```\n", StandardCharsets.UTF_8);

        Files.writeString(outputDirectory.resolve("llms.txt"),
                buildIndex(overview, guideDescriptions, contributorDescriptions, siteUrl, gitRef), StandardCharsets.UTF_8);
        Files.writeString(outputDirectory.resolve("llms-full.txt"),
                buildFull(overview, guideDescriptions, docsDirectory, signaturesHeader, signatures), StandardCharsets.UTF_8);

        System.out.println("Wrote llms.txt, llms-full.txt and llms-api.txt for Kraken API " + version + " to " + outputDirectory);
    }

    private static String generateSignatures(Path sourceDirectory, String classpath, Path signaturesFile) throws IOException {
        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = List.of(
                "--source-path", sourceDirectory.toString(),
                "-classpath", classpath,
                "-subpackages", "com.kraken.api",
                "-protected",
                "-quiet",
                "--output", signaturesFile.toString()
        );

        try (StandardJavaFileManager fileManager = tool.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            boolean success = tool.getTask(null, fileManager, diagnostics, ApiSignatureDoclet.class, options, null).call();
            if (!success) {
                String errors = diagnostics.getDiagnostics().stream()
                        .filter(diagnostic -> diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));
                throw new IllegalStateException("Signature generation failed:\n" + errors);
            }
        }
        return Files.readString(signaturesFile, StandardCharsets.UTF_8);
    }

    private static String buildIndex(String overview, Map<String, String> guides, Map<String, String> contributorGuides,
                                     String siteUrl, String gitRef) {
        StringBuilder index = new StringBuilder(overview).append("\n\n");

        index.append("## Docs\n\n");
        guides.forEach((file, description) -> index.append(link(file, RAW_URL + "/" + gitRef + "/docs/" + file, description)));

        index.append("\n## API reference\n\n");
        index.append(link("llms-api.txt", siteUrl + "/llms-api.txt",
                "Every public type and member as Java stubs with one-line Javadoc summaries"));
        index.append(link("llms-full.txt", siteUrl + "/llms-full.txt",
                "This overview, every guide above and the API signatures in a single file"));

        index.append("\n## Optional\n\n");
        contributorGuides.forEach((file, description) -> index.append(link(file, RAW_URL + "/" + gitRef + "/docs/" + file, description)));
        index.append(link("Source repository", REPOSITORY_URL, "Kraken API source code"));
        index.append(link("Release files", REPOSITORY_URL + "/releases/latest",
                "The jars and these llms files are attached to every release"));
        return index.toString();
    }

    private static String buildFull(String overview, Map<String, String> guides, Path docsDirectory,
                                    String signaturesHeader, String signatures) throws IOException {
        StringBuilder full = new StringBuilder(overview).append("\n\n");
        for (String file : guides.keySet()) {
            Path guide = docsDirectory.resolve(file);
            if (!Files.exists(guide)) {
                continue;
            }
            String content = Files.readString(guide, StandardCharsets.UTF_8).replace("﻿", "").strip();
            full.append("---\n\n<!-- Source: docs/").append(file).append(" -->\n\n").append(content).append("\n\n");
        }
        full.append("---\n\n").append(signaturesHeader).append("```java\n").append(signatures).append("```\n");
        return full.toString();
    }

    private static List<String> unlistedDocs(Path docsDirectory) throws IOException {
        try (Stream<Path> files = Files.list(docsDirectory)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".md"))
                    .filter(name -> !GUIDES.containsKey(name) && !CONTRIBUTOR_GUIDES.containsKey(name) && !EXCLUDED_DOCS.contains(name))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String title(Path markdown) throws IOException {
        try (Stream<String> lines = Files.lines(markdown, StandardCharsets.UTF_8)) {
            return lines.map(line -> line.replace("﻿", ""))
                    .filter(line -> line.startsWith("# "))
                    .map(line -> line.substring(2).trim())
                    .findFirst()
                    .orElse(markdown.getFileName().toString());
        }
    }

    private static String link(String name, String url, String description) {
        return "- [" + name + "](" + url + "): " + description + "\n";
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        List<String> unknown = new ArrayList<>();
        for (int index = 0; index < args.length; index++) {
            if (args[index].startsWith("--") && index + 1 < args.length) {
                options.put(args[index], args[++index]);
            } else {
                unknown.add(args[index]);
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unexpected arguments: " + unknown);
        }
        return options;
    }

    private static String require(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option " + name);
        }
        return value;
    }
}
