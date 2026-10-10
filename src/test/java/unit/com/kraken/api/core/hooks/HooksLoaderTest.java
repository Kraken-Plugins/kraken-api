package unit.com.kraken.api.core.hooks;

import com.kraken.api.core.hooks.GameHooks;
import com.kraken.api.core.hooks.HooksLoader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the hooks loader's fail-fast contract: when the resource loads, every accessor returns
 * a populated group; when loading has failed, every accessor throws an {@link IllegalStateException}
 * that carries the original cause instead of handing back a null that NPEs deep in reflection code.
 */
class HooksLoaderTest {

    @Test
    void accessorsReturnPopulatedHooksWhenLoaded() {
        assertNotNull(HooksLoader.getGameHooks());
        assertNotNull(HooksLoader.getReflectionHooks());
        assertNotNull(HooksLoader.getLoginHooks());
        assertNotNull(HooksLoader.getSecurityHooks());
        assertNotNull(HooksLoader.getPackets());
    }

    @Test
    void securityHookDefaultsComeFromTheJsonNotFieldInitializers() {
        assertEquals("client", HooksLoader.getSecurityHooks().getMouseHookDllClassName());
    }

    @Test
    void accessorsThrowWithTheOriginalCauseWhenLoadingFailed() throws Exception {
        Field loadFailureField = HooksLoader.class.getDeclaredField("loadFailure");
        loadFailureField.setAccessible(true);
        Object previous = loadFailureField.get(null);

        Exception cause = new IllegalStateException("simulated stale hooks");
        try {
            loadFailureField.set(null, cause);

            IllegalStateException thrown = assertThrows(IllegalStateException.class, HooksLoader::getReflectionHooks);
            assertSame(cause, thrown.getCause());
            assertThrows(IllegalStateException.class, HooksLoader::getPackets);
            assertThrows(IllegalStateException.class, HooksLoader::getSecurityHooks);
            assertThrows(IllegalStateException.class, HooksLoader::getLoginHooks);
            assertThrows(IllegalStateException.class, HooksLoader::getGameHooks);
            assertThrows(IllegalStateException.class, HooksLoader::requireLoaded);
        } finally {
            loadFailureField.set(null, previous);
        }
    }

    @Test
    void warnsOnlyWhenBothRevisionsAreKnownAndDiffer() {
        GameHooks hooks = HooksLoader.getGameHooks();
        Integer previous = hooks.getRevision();
        try {
            hooks.setRevision(241);
            assertTrue(HooksLoader.warnOnRevisionMismatch(242));
            assertFalse(HooksLoader.warnOnRevisionMismatch(241));
            assertFalse(HooksLoader.warnOnRevisionMismatch(0), "an uninitialised client reports revision 0");

            hooks.setRevision(null);
            assertFalse(HooksLoader.warnOnRevisionMismatch(242), "hooks mapped before the revision was recorded");
        } finally {
            hooks.setRevision(previous);
        }
    }
}
