package validation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Development-only JVM reflection of the independently checked structural audit.
 * Class.forName uses initialize=false. No client instance, login or sockets are created.
 */
public final class HookAbiValidator {
    private HookAbiValidator() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: HookAbiValidator audit.json output.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject input = gson.fromJson(Files.readString(Path.of(args[0])), JsonObject.class);
        ClassLoader loader = HookAbiValidator.class.getClassLoader();
        Class<?> clientType = Class.forName("client", false, loader);
        Path clientJar = Path.of(clientType.getProtectionDomain().getCodeSource().getLocation().toURI());
        requireDigest(Files.readAllBytes(clientJar), input.get("jar_sha256").getAsString(), "injected client");
        try (java.io.InputStream hooks = HookAbiValidator.class.getResourceAsStream("/hooks.json")) {
            if (hooks == null) throw new IllegalStateException("Missing hooks resource");
            requireDigest(hooks.readAllBytes(), input.get("hooks_sha256").getAsString(), "hooks resource");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        int failures = 0;
        JsonArray rows = input.getAsJsonArray("structural");
        for (int i = 0; i < rows.size(); i++) {
            JsonObject row = rows.get(i).getAsJsonObject();
            Map<String, Object> result = new LinkedHashMap<>();
            String hook = row.get("hook").getAsString();
            String ownerName = row.get("owner").getAsString();
            String member = row.get("mapped_name").getAsString();
            String expected = row.get("expected_type").getAsString();
            result.put("hook", hook); result.put("owner", ownerName); result.put("mapped_name", member);
            result.put("expected_type", expected);
            try {
                Class<?> owner = Class.forName(ownerName, false, loader);
                if (hook.equals("bufferInheritance")) {
                    if (!owner.getSuperclass().getName().equals(expected)) throw new IllegalStateException("Unexpected superclass");
                    result.put("actual_type", owner.getSuperclass().getName());
                } else if (expected.equals("class")) {
                    result.put("actual_type", owner.getName());
                } else {
                    boolean staticExpected = expected.startsWith("static ");
                    String descriptor = expected.substring(expected.indexOf(' ') + 1);
                    int modifiers;
                    if (descriptor.startsWith("(")) {
                        MethodType type = MethodType.fromMethodDescriptorString(descriptor, loader);
                        Method method = owner.getDeclaredMethod(member, type.parameterArray());
                        if (method.getReturnType() != type.returnType()) throw new IllegalStateException("Return type mismatch");
                        modifiers = method.getModifiers();
                        result.put("actual_type", method.toGenericString());
                    } else {
                        Field field = owner.getDeclaredField(member);
                        Class<?> type = MethodType.fromMethodDescriptorString("()"+descriptor, loader).returnType();
                        if (field.getType() != type) throw new IllegalStateException("Field type mismatch");
                        modifiers = field.getModifiers();
                        result.put("actual_type", field.toGenericString());
                    }
                    if (Modifier.isStatic(modifiers) != staticExpected) throw new IllegalStateException("Static/instance mismatch");
                }
                result.put("status", "PASS");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
                result.put("status", "FAIL"); result.put("reason", ex.toString()); failures++;
            }
            results.add(result);
        }
        Path output = Path.of(args[1]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, gson.toJson(results)+"\n");
        System.out.println("Offline JVM reflection: "+(results.size()-failures)+" PASS, "+failures+" FAIL. No client launched.");
        if (failures != 0) throw new IllegalStateException("Hook ABI validation failed: "+failures);
    }

    private static void requireDigest(byte[] bytes, String expected, String label) throws Exception {
        StringBuilder actual = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) actual.append(String.format("%02x", b & 255));
        if (!actual.toString().equals(expected)) throw new IllegalStateException(label+" differs from the audited artifact");
    }
}