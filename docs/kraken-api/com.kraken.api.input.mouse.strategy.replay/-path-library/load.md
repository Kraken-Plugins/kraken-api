//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[PathLibrary](index.md)/[load](load.md)

# load

[Kraken API]\
open fun [load](load.md)(library: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)&gt;

Loads and normalizes a set of mouse gestures from a specified library file, converting them into [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) objects. The method searches for a corresponding JSON file within the directory defined by `DATA_DIR`. If the file is found, it parses the content, normalizes valid mouse gestures, and logs the results. 

Mouse gestures are read line-by-line. Invalid or malformed entries are skipped with appropriate warnings logged. Normalization is performed through the `PathNormalizer.normalize()` method, which filters out gestures that cannot be reliably normalized.

#### Return

A `List` of [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) objects representing the normalized mouse gestures loaded from the library file. Returns null if no matching file is found or if an error occurs during the loading process.

#### Parameters

Kraken API

| | |
|---|---|
| library | The name of the library for which to load mouse gestures. This name is used to search for a JSON file in the `DATA_DIR`. Spaces in the library name are replaced with underscores during the filename matching. |
