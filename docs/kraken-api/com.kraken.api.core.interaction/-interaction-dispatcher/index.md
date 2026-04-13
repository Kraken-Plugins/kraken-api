//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[InteractionDispatcher](index.md)

# InteractionDispatcher

[Kraken API]\
open class [InteractionDispatcher](index.md)

Handles the low-level sending of a resolved menu interaction: queuing the mouse click packet and invoking the menu action via [DoActionInvoker](../-do-action-invoker/index.md).

## Constructors

| | |
|---|---|
| [InteractionDispatcher](-interaction-dispatcher.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [dispatch](dispatch.md) | [Kraken API]<br>open fun [dispatch](dispatch.md)(point: Point, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), resolvedAction: [ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md))<br>Dispatches a resolved menu action at the given canvas point. |
