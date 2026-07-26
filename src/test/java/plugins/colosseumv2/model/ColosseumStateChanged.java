package plugins.colosseumv2.model;

import lombok.Value;
import plugins.colosseum.model.ColosseumState;

@Value
public class ColosseumStateChanged {
    plugins.colosseum.model.ColosseumState previousState;
    ColosseumState newState;
}