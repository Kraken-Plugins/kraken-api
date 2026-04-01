//[kraken-api](../../../index.md)/[com.kraken.api.core.mapping](../index.md)/[ClientDownloader](index.md)/[downloadInjectedClient](download-injected-client.md)

# downloadInjectedClient

[Kraken API]\
open fun [downloadInjectedClient](download-injected-client.md)(version: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), destination: [Path](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/Path.html)): [Path](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/Path.html)

Downloads the injected client JAR file for a specified version to the given destination path. 

This method retrieves the injected client file from a predefined base URL. It validates the version string, ensuring it meets compatibility requirements, and processes @SNAPSHOT versions by removing the snapshot suffix. If the destination directory does not exist, it attempts to create the necessary directory structure. 

If the version does not meet predefined compatibility criteria, an [UnsupportedOperationException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/UnsupportedOperationException.html) is thrown. During the download or file writing process, any [IOException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/IOException.html) encountered is logged but not rethrown.

#### Parameters

Kraken API

| | |
|---|---|
| version | the version of the injected client to download. This must follow the expected versioning scheme and meet the compatibility criteria. |
| destination | the [Path](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/Path.html) where the injected client JAR will be downloaded. The path must include the file name for the target file, and parent directories will be created if they do not exist. |

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
