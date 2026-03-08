import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;

import java.util.Arrays;

@Slf4j
public class PluginRunnerTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        try {
            ByteBuddyAgent.install();
            log.info("ByteBuddy agent installed successfully");
        } catch (Exception e) {
            log.error("Failed to install ByteBuddy agent — class reloading will not work", e);
        }

        String[] passArgs = args;

        if (args.length > 0) {
            try {
                Class<?> clazz = Class.forName(args[0]);
                if (Plugin.class.isAssignableFrom(clazz)) {
                    ExternalPluginManager.loadBuiltin((Class<? extends Plugin>) clazz);
                    passArgs = Arrays.copyOfRange(args, 1, args.length);
                }
            } catch (ClassNotFoundException e) {
                // Argument is not a class, proceed with default behavior
                log.error("Argument is not a valid plugin class to run: {}", args[0], e);
                return;
            }
        }

        RuneLite.main(passArgs);
    }
}
