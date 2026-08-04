package plugins.api.suite;

import lombok.Value;
import plugins.api.world.NamedLocation;

/**
 * One entry in a run's itinerary, carrying why it sits where it does.
 *
 * <p>The rationale is not decoration: it makes the ordering reviewable before the run starts, so you
 * can see that the teleporting test is last at the hub rather than having to infer it from behaviour
 * half an hour in.</p>
 */
@Value
public class PlannedStep {

    /** Zero based position in the itinerary. */
    int index;

    /** The test to run. */
    RegisteredTest test;

    /** Where it will run, or {@link NamedLocation#ANYWHERE} when it does not care. */
    NamedLocation location;

    /** Higher means more disruptive, and therefore later within its location group. */
    int disruptionRank;

    /** Human readable explanation of the placement. */
    String rationale;
}
