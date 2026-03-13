//[kraken-api](../../../index.md)/[com.kraken.api.core.mapping](../index.md)/[ClientDownloader](index.md)/[downloadInjectedClient](download-injected-client.md)

# downloadInjectedClient

[Kraken API]\
open fun [downloadInjectedClient](download-injected-client.md)(destination: [Path](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/Path.html))

Downloads the injected client JAR file to the specified destination path. If the destination file already exists, the method skips the download process. 

The method determines the required RuneLite version, constructs the URL for the injected client, and downloads the file to the specified location. If the RuneLite version is a @SNAPSHOT version, it handles the version accordingly by removing the @SNAPSHOT suffix. Unsupported versions will result in an exception. 

Exceptions such as [IOException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/IOException.html) during the file operation are logged.

#### Parameters

Kraken API

| | |
|---|---|
| destination | the [Path](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/Path.html) where the injected client will be downloaded. This must include both the path and the file name of the target file. |
