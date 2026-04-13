//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[MenuActionResolver](index.md)

# MenuActionResolver

interface [MenuActionResolver](index.md)&lt;[T](index.md)&gt;

Strategy for resolving a [ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md) for a specific entity type.

#### Parameters

Kraken API

| | |
|---|---|
| &lt;T&gt; | The type of game entity this resolver handles (NPC, Widget, TileObject, etc.) |

#### Inheritors

| |
|---|
| [BankItemMenuActionResolver](../../com.kraken.api.core.interaction.resolver/-bank-item-menu-action-resolver/index.md) |
| [GroundItemMenuActionResolver](../../com.kraken.api.core.interaction.resolver/-ground-item-menu-action-resolver/index.md) |
| [NpcMenuActionResolver](../../com.kraken.api.core.interaction.resolver/-npc-menu-action-resolver/index.md) |
| [PlayerMenuActionResolver](../../com.kraken.api.core.interaction.resolver/-player-menu-action-resolver/index.md) |
| [TileObjectMenuActionResolver](../../com.kraken.api.core.interaction.resolver/-tile-object-menu-action-resolver/index.md) |
| [WidgetMenuActionResolver](../../com.kraken.api.core.interaction.resolver/-widget-menu-action-resolver/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getEntityType](get-entity-type.md) | [Kraken API]<br>abstract fun [getEntityType](get-entity-type.md)(): [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)&lt;[T](index.md)&gt;<br>The entity type this resolver handles. |
| [resolve](resolve.md) | [Kraken API]<br>abstract fun [resolve](resolve.md)(entity: [T](index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md)&gt;<br>Resolves the appropriate menu action for the given entity and action string. |
