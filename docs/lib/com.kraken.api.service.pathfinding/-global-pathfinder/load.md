//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[load](load.md)

# load

[Kraken API]\
open fun [load](load.md)(): [GlobalPathfinder](index.md)

Loads a global pathfinder from the bundled `/map.dat` collision map.

#### Return

A fully initialized GlobalPathfinder.

#### Throws

| | |
|---|---|
| [IOException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/IOException.html) | If the map resource cannot be read. |
| [ClassNotFoundException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassNotFoundException.html) | If the serialized map payload cannot be deserialized. |

[Kraken API]\
open fun [load](load.md)(filePath: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [GlobalPathfinder](index.md)

Loads a global pathfinder from a specific serialized map file path.

#### Return

A fully initialized GlobalPathfinder, or null when the file is missing.

#### Parameters

Kraken API

| | |
|---|---|
| filePath | The path to the serialized global collision map file. |

#### Throws

| | |
|---|---|
| [IOException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/IOException.html) | If the map file cannot be read. |
| [ClassNotFoundException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ClassNotFoundException.html) | If the serialized map payload cannot be deserialized. |
