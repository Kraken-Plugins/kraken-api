package plugins.colosseum.model;

import lombok.Value;

@Value
public class ColosseumStateChanged {
    ColosseumState previousState;
    ColosseumState newState;
}