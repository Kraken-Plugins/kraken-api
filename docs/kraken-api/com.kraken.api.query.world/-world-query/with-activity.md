//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[withActivity](with-activity.md)

# withActivity

[Kraken API]\
open fun [withActivity](with-activity.md)(activity: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [WorldQuery](index.md)

Filters the current query to include only worlds associated with the specified activity. 

 This method modifies the query to match worlds where the activity description (if not `null`) contains the provided activity string, after sanitizing and performing a case-insensitive comparison. 

#### Return

A `WorldQuery` object filtered to include only worlds matching the specified activity.

#### Parameters

Kraken API

| | |
|---|---|
| activity | The activity string to filter worlds by. This value is case-insensitive and will be sanitized before matching. |
