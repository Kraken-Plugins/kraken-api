package com.kraken.api.input.mouse.strategy;

import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.bezier.BezierStrategy;
import com.kraken.api.input.mouse.strategy.instant.InstantStrategy;
import com.kraken.api.input.mouse.strategy.linear.LinearStrategy;
import com.kraken.api.input.mouse.strategy.none.NoMovement;
import com.kraken.api.input.mouse.strategy.replay.ReplayStrategy;
import com.kraken.api.input.mouse.strategy.wind.WindStrategy;
import com.kraken.api.core.Services;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

@Slf4j
@Singleton
public enum MouseMovementStrategy {
    NO_MOVEMENT(NoMovement.class),
    INSTANT(InstantStrategy.class),
    LINEAR(LinearStrategy.class),
    BEZIER(BezierStrategy.class),
    WIND(WindStrategy.class),
    REPLAY(ReplayStrategy.class);
    
    private final Class<? extends MoveableMouse> strategyClass;
    private volatile MoveableMouse strategy;

    MouseMovementStrategy(Class<? extends MoveableMouse> clazz) {
        this.strategyClass = clazz;
    }

    /**
     * Returns the movement implementation for this strategy, resolving it on first use.
     *
     * <p>Enum constants initialise as a group, so resolving in the constructor would build every
     * strategy the moment any one of them is referenced. Resolving here builds only the strategy that
     * is actually used, and keeps the enum usable if the first reference happens before RuneLite's
     * injector exists.</p>
     *
     * @return the {@link MoveableMouse} implementing this strategy.
     */
    public MoveableMouse getStrategy() {
        MoveableMouse local = strategy;
        if (local == null) {
            synchronized (this) {
                local = strategy;
                if (local == null) {
                    strategy = local = Services.get(strategyClass);
                }
            }
        }
        return local;
    }

    /**
     * Loads a specified library into the current {@code MouseMovementStrategy} instance.
     *
     * <p>This method delegates the library loading process to the active movement strategy
     * if it is of type {@code ReplayStrategyMoveable}. The actual loading operation is
     * handled by the underlying {@code ReplayStrategyMoveable} object.</p>
     *
     * <ul>
     *   <li>If the active strategy is not an instance of {@code ReplayStrategyMoveable}, this method does nothing.</li>
     *   <li>If the specified library name is invalid or null, it may result in no operation or an error within the
     *       {@code ReplayStrategyMoveable} implementation.</li>
     * </ul>
     *
     * @param libraryName The name of the library to be loaded. This must be a valid,
     *                    non-null string representing the name or path of the library
     *                    to ensure successful loading.
     */
    public void loadLibrary(String libraryName) {
        if (strategy instanceof ReplayStrategy) {
            ((ReplayStrategy) strategy).loadLibrary(libraryName);
        } else {
            log.warn("Strategy: {} does not require mouse gesture libraries to be loaded.", name());
        }
    }
}
