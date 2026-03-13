//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse](../index.md)/[VirtualMouse](index.md)/[findLibraries](find-libraries.md)

# findLibraries

[Kraken API]\
open fun [findLibraries](find-libraries.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;

Scans a specified directory for JSON files and returns a list of library names derived from the filenames. 

 This method attempts to list all files in a predefined directory, extract the filenames, and remove the &quot;.json&quot; extension to identify the libraries. 

In the event of an error during file scanning or processing, it logs the error and returns an empty list.

#### Return

A List&lt;String&gt; containing the names of the libraries (filenames without the &quot;.json&quot; extension), or an empty list if an error occurs or no libraries are found.
