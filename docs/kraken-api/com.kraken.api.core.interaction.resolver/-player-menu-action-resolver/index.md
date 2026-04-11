//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[PlayerMenuActionResolver](index.md)

# PlayerMenuActionResolver

[Kraken API]\
open class [PlayerMenuActionResolver](index.md) : [MenuActionResolver](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&lt;[T](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&gt;

## Constructors

| | |
|---|---|
| [PlayerMenuActionResolver](-player-menu-action-resolver.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getEntityType](get-entity-type.md) | [Kraken API]<br>open fun [getEntityType](get-entity-type.md)(): [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)&lt;Player&gt;<br>The entity type this resolver handles. |
| [resolve](resolve.md) | [Kraken API]<br>open fun [resolve](resolve.md)(player: Player, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md)&gt;<br>Resolves the appropriate menu action for the given entity and action string. |
