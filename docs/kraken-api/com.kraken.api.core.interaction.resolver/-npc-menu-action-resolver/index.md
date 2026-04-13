//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[NpcMenuActionResolver](index.md)

# NpcMenuActionResolver

[Kraken API]\
open class [NpcMenuActionResolver](index.md) : [MenuActionResolver](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&lt;[T](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&gt;

## Constructors

| | |
|---|---|
| [NpcMenuActionResolver](-npc-menu-action-resolver.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getEntityType](get-entity-type.md) | [Kraken API]<br>open fun [getEntityType](get-entity-type.md)(): [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)&lt;NPC&gt;<br>The entity type this resolver handles. |
| [resolve](resolve.md) | [Kraken API]<br>open fun [resolve](resolve.md)(npc: NPC, action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md)&gt;<br>Resolves the appropriate menu action for the given entity and action string. |
