package plugins.colosseumv2.model;

import lombok.Value;

@Value
public class ColosseumStateChanged {
    ColosseumState previousState;
    ColosseumState newState;
}
