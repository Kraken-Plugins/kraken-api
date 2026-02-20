package com.kraken.api.simulation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configurable policy that unifies simulation capture settings, candidate-action generation,
 * state scoring, and executable step permissions.
 *
 * <p>This is intended to be the primary extension point for plugin developers who want to
 * control simulation behavior without hard-coding logic directly into a plugin tick loop.</p>
 */
@Getter
public final class SimulationActionPolicy {
    /**
     * Action provider for decision-tree candidate generation.
     */
    @FunctionalInterface
    public interface ActionProvider {
        /**
         * Provides candidate actions for a state/depth.
         *
         * @param context policy callback context.
         * @return candidate actions. Returning null/empty contributes nothing.
         */
        List<SimulationAction> provide(SimulationActionPolicyContext context);
    }

    /**
     * Scoring rule for decision-tree state evaluation.
     */
    @FunctionalInterface
    public interface ScoringRule {
        /**
         * Scores a simulation state contribution.
         *
         * @param context policy callback context.
         * @return score contribution; larger is better.
         */
        double score(SimulationActionPolicyContext context);
    }

    private static final ActionProvider DEFAULT_ACTION_PROVIDER = context -> SimulationAction.standardWalkActions();

    private final SimulationSnapshotService.CaptureOptions captureOptions;
    private final SimulationDecisionAdapter.AdaptOptions adaptOptions;
    private final Set<SimulationDecisionAdapter.ExecutableStepType> allowedExecutionSteps;
    private final List<ActionProvider> actionProviders;
    private final List<ScoringRule> scoringRules;

    private SimulationActionPolicy(
            SimulationSnapshotService.CaptureOptions captureOptions,
            SimulationDecisionAdapter.AdaptOptions adaptOptions,
            Set<SimulationDecisionAdapter.ExecutableStepType> allowedExecutionSteps,
            List<ActionProvider> actionProviders,
            List<ScoringRule> scoringRules
    ) {
        this.captureOptions = captureOptions == null ? new SimulationSnapshotService.CaptureOptions() : captureOptions;
        this.adaptOptions = adaptOptions == null ? SimulationDecisionAdapter.AdaptOptions.none() : adaptOptions;
        this.allowedExecutionSteps = copyOrAllSteps(allowedExecutionSteps);
        this.actionProviders = actionProviders == null || actionProviders.isEmpty()
                ? Collections.singletonList(DEFAULT_ACTION_PROVIDER)
                : List.copyOf(actionProviders);
        this.scoringRules = scoringRules == null
                ? Collections.emptyList()
                : List.copyOf(scoringRules);
    }

    /**
     * Creates a policy builder.
     *
     * @return builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generates candidate actions using all registered providers.
     *
     * <p>Returned actions are de-duplicated and null-filtered while preserving provider order.</p>
     *
     * @param context policy callback context.
     * @return candidate actions.
     */
    public List<SimulationAction> generateActions(SimulationActionPolicyContext context) {
        if (context == null) {
            return SimulationAction.standardWalkActions();
        }

        LinkedHashSet<SimulationAction> collected = new LinkedHashSet<>();
        for (ActionProvider provider : actionProviders) {
            if (provider == null) {
                continue;
            }

            List<SimulationAction> provided = provider.provide(context);
            if (provided == null || provided.isEmpty()) {
                continue;
            }

            for (SimulationAction action : provided) {
                if (action != null) {
                    collected.add(action);
                }
            }
        }

        if (collected.isEmpty()) {
            return SimulationAction.standardWalkActions();
        }
        return new ArrayList<>(collected);
    }

    /**
     * Evaluates a state by summing all scoring-rule contributions.
     *
     * @param context policy callback context.
     * @return combined score.
     */
    public double evaluate(SimulationActionPolicyContext context) {
        if (context == null || scoringRules.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        for (ScoringRule rule : scoringRules) {
            if (rule == null) {
                continue;
            }
            score += rule.score(context);
        }
        return score;
    }

    /**
     * Converts this policy into a decision-tree action generator.
     *
     * @param engine simulation engine.
     * @return action generator.
     */
    public DecisionTreeSearch.ActionGenerator toActionGenerator(SimulationEngine engine) {
        Objects.requireNonNull(engine, "engine");
        return (state, depthRemaining) -> generateActions(new SimulationActionPolicyContext(engine, state, depthRemaining));
    }

    /**
     * Converts this policy into a decision-tree state evaluator.
     *
     * @param engine simulation engine.
     * @return state evaluator.
     */
    public DecisionTreeSearch.StateEvaluator toStateEvaluator(SimulationEngine engine) {
        Objects.requireNonNull(engine, "engine");
        return state -> evaluate(new SimulationActionPolicyContext(engine, state, 0));
    }

    private static Set<SimulationDecisionAdapter.ExecutableStepType> copyOrAllSteps(
            Set<SimulationDecisionAdapter.ExecutableStepType> allowedExecutionSteps
    ) {
        if (allowedExecutionSteps == null || allowedExecutionSteps.isEmpty()) {
            return Collections.unmodifiableSet(EnumSet.allOf(SimulationDecisionAdapter.ExecutableStepType.class));
        }
        EnumSet<SimulationDecisionAdapter.ExecutableStepType> copied = EnumSet.noneOf(SimulationDecisionAdapter.ExecutableStepType.class);
        copied.addAll(allowedExecutionSteps);
        return Collections.unmodifiableSet(copied);
    }

    /**
     * Builder for {@link SimulationActionPolicy}.
     */
    public static final class Builder {
        private SimulationSnapshotService.CaptureOptions captureOptions = new SimulationSnapshotService.CaptureOptions();
        private SimulationDecisionAdapter.AdaptOptions adaptOptions = SimulationDecisionAdapter.AdaptOptions.none();
        private final EnumSet<SimulationDecisionAdapter.ExecutableStepType> allowedExecutionSteps =
                EnumSet.allOf(SimulationDecisionAdapter.ExecutableStepType.class);
        private final List<ActionProvider> actionProviders = new ArrayList<>();
        private final List<ScoringRule> scoringRules = new ArrayList<>();

        /**
         * Sets snapshot capture options for this policy.
         *
         * @param captureOptions capture options.
         * @return builder.
         */
        public Builder captureOptions(SimulationSnapshotService.CaptureOptions captureOptions) {
            if (captureOptions != null) {
                this.captureOptions = captureOptions;
            }
            return this;
        }

        /**
         * Sets decision-adapter options for this policy.
         *
         * @param adaptOptions adaptation options.
         * @return builder.
         */
        public Builder adaptOptions(SimulationDecisionAdapter.AdaptOptions adaptOptions) {
            if (adaptOptions != null) {
                this.adaptOptions = adaptOptions;
            }
            return this;
        }

        /**
         * Replaces allowed executable steps.
         *
         * @param allowedExecutionSteps allowed step types.
         * @return builder.
         */
        public Builder allowedExecutionSteps(Set<SimulationDecisionAdapter.ExecutableStepType> allowedExecutionSteps) {
            this.allowedExecutionSteps.clear();
            if (allowedExecutionSteps != null && !allowedExecutionSteps.isEmpty()) {
                this.allowedExecutionSteps.addAll(allowedExecutionSteps);
            } else {
                this.allowedExecutionSteps.addAll(EnumSet.allOf(SimulationDecisionAdapter.ExecutableStepType.class));
            }
            return this;
        }

        /**
         * Adds allowed executable step types.
         *
         * @param steps step types to allow.
         * @return builder.
         */
        public Builder allowExecutionSteps(SimulationDecisionAdapter.ExecutableStepType... steps) {
            if (steps == null || steps.length == 0) {
                return this;
            }
            Collections.addAll(this.allowedExecutionSteps, steps);
            return this;
        }

        /**
         * Adds an action provider.
         *
         * @param provider action provider.
         * @return builder.
         */
        public Builder addActionProvider(ActionProvider provider) {
            if (provider != null) {
                this.actionProviders.add(provider);
            }
            return this;
        }

        /**
         * Adds a state scoring rule.
         *
         * @param rule scoring rule.
         * @return builder.
         */
        public Builder addScoringRule(ScoringRule rule) {
            if (rule != null) {
                this.scoringRules.add(rule);
            }
            return this;
        }

        /**
         * Builds the immutable policy.
         *
         * @return action policy.
         */
        public SimulationActionPolicy build() {
            return new SimulationActionPolicy(
                    captureOptions,
                    adaptOptions,
                    allowedExecutionSteps,
                    actionProviders,
                    scoringRules
            );
        }
    }
}
