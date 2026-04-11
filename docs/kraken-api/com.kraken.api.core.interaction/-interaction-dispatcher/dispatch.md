//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[InteractionDispatcher](index.md)/[dispatch](dispatch.md)

# dispatch

[Kraken API]\
open fun [dispatch](dispatch.md)(point: Point, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), resolvedAction: [ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md))

Dispatches a resolved menu action at the given canvas point.

#### Parameters

Kraken API

| | |
|---|---|
| point | Canvas coordinates to click |
| action | Action label (e.g. &quot;Attack&quot;) — used for logging and the engine call |
| resolvedAction | The fully resolved menu option and target string |
