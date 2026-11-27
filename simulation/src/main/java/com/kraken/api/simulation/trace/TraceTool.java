package com.kraken.api.simulation.trace;

import com.kraken.api.simulation.data.ContentPack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line wave trace tooling, given the content pack class first (a {@link ContentPack} with a public static
 * {@code load()} or a public no argument constructor):
 *
 * <pre>
 *   &lt;pack&gt; replay &lt;live.jsonl&gt; [out.jsonl]   engine re run of a live trace to a replay trace
 *   &lt;pack&gt; diff &lt;a.jsonl&gt; &lt;b.jsonl&gt;          tick aligned comparison report to stdout
 *   &lt;pack&gt; check &lt;live.jsonl&gt;                replay then diff against the live trace
 *   &lt;pack&gt; stepfit &lt;live.jsonl...&gt;            fit smart NPC step policies against recorded steps
 *   &lt;pack&gt; ranges &lt;live.jsonl...&gt;             validate attack ranges and sight against recorded attacks
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TraceTool {

    /**
     * Runs one command.
     * @param args the pack class, the command and its files
     * @throws IOException when a trace cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            usage(System.out);
            return;
        }
        run(loadPack(args[0]), args[1], java.util.Arrays.copyOfRange(args, 2, args.length), System.out);
    }

    /**
     * Runs one command against a loaded pack.
     * @param pack the content pack
     * @param command replay, diff, check, stepfit or ranges
     * @param files the command's files
     * @param out where reports go
     * @throws IOException when a trace cannot be read or written
     */
    public static void run(ContentPack pack, String command, String[] files, PrintStream out) throws IOException {
        switch (command) {
            case "replay":
                replay(pack, Paths.get(files[0]), files.length > 1 ? Paths.get(files[1]) : replayPath(files[0]), out);
                break;
            case "diff":
                if (files.length < 2) {
                    usage(out);
                    return;
                }
                TraceDiff.diff(WaveTrace.read(Paths.get(files[0])), WaveTrace.read(Paths.get(files[1])),
                        pack.data(), pack.trace(), out);
                break;
            case "check": {
                Path replayed = replayPath(files[0]);
                replay(pack, Paths.get(files[0]), replayed, out);
                out.println();
                TraceDiff.diff(WaveTrace.read(Paths.get(files[0])), WaveTrace.read(replayed),
                        pack.data(), pack.trace(), out);
                break;
            }
            case "stepfit":
                StepFit.run(readAll(files), pack.data(), out);
                break;
            case "ranges":
                TraceRanges.run(readAll(files), pack.data(), pack.trace(), out);
                break;
            default:
                usage(out);
        }
    }

    /**
     * Loads a content pack by class name.
     * @param className the pack's class
     * @return the loaded pack
     */
    public static ContentPack loadPack(String className) {
        try {
            Class<?> type = Class.forName(className);
            try {
                Method load = type.getMethod("load");
                return (ContentPack) load.invoke(null);
            } catch (NoSuchMethodException noLoad) {
                return (ContentPack) type.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("cannot load content pack " + className, e);
        }
    }

    private static List<WaveTrace> readAll(String[] files) throws IOException {
        List<WaveTrace> traces = new ArrayList<>();
        for (String file : files) {
            traces.add(WaveTrace.read(Paths.get(file)));
        }
        return traces;
    }

    private static void replay(ContentPack pack, Path in, Path outFile, PrintStream out) throws IOException {
        WaveTrace live = WaveTrace.read(in);
        try (BufferedWriter w = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            WaveTraceReplayer.replay(live, pack, line -> {
                try {
                    w.write(line);
                    w.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, in.getFileName().toString());
        }
        out.println("replayed " + in + " -> " + outFile);
    }

    private static Path replayPath(String in) {
        String base = in.endsWith(".live.jsonl")
                ? in.substring(0, in.length() - ".live.jsonl".length()) : in;
        return Paths.get(base + ".replay.jsonl");
    }

    private static void usage(PrintStream out) {
        out.println("usage: <pack class> replay <live.jsonl> [out.jsonl] | diff <a.jsonl> <b.jsonl>"
                + " | check <live.jsonl> | stepfit <live.jsonl...> | ranges <live.jsonl...>");
    }
}
