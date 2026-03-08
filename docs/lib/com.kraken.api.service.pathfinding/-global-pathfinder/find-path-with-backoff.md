//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[findPathWithBackoff](find-path-with-backoff.md)

# findPathWithBackoff

[Kraken API]\
open fun [findPathWithBackoff](find-path-with-backoff.md)(start: WorldPoint, target: WorldPoint, maxBackoffRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Attempts to find a path to the target tile and progressively backs off around the target if needed.

#### Return

A dense path including the start tile, or an empty list when no fallback path exists.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting tile. |
| target | The preferred destination tile. |
| maxBackoffRadius | Maximum radius used to search for a nearby unblocked fallback destination. |
