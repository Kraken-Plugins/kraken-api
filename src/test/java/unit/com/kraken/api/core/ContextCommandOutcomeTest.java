package unit.com.kraken.api.core;

import com.kraken.api.Context;
import com.kraken.api.core.ClientThreadException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContextCommandOutcomeTest {
    @Test
    void fallbackAndOptionalDoNotHideUnknownOutcomes() {
        Context context = mock(Context.class, CALLS_REAL_METHODS);
        ClientThreadException unknown = new ClientThreadException("already started", null, true);
        doThrow(unknown).when(context).runOnClientThread(any(Callable.class));
        assertSame(unknown, assertThrows(ClientThreadException.class, () -> context.runOnClientThread(() -> true, false)));
        assertSame(unknown, assertThrows(ClientThreadException.class, () -> context.runOnClientThreadOptional(() -> true)));
    }

    @Test
    void ordinaryRejectionStillUsesFallbackAndEmptyOptional() {
        Context context = mock(Context.class, CALLS_REAL_METHODS);
        doThrow(new ClientThreadException("cancelled before execution")).when(context).runOnClientThread(any(Callable.class));
        assertFalse(context.runOnClientThread(() -> true, false));
        assertTrue(context.runOnClientThreadOptional(() -> true).isEmpty());
    }
}
