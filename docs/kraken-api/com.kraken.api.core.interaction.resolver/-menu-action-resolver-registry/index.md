//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[MenuActionResolverRegistry](index.md)

# MenuActionResolverRegistry

[Kraken API]\
open class [MenuActionResolverRegistry](index.md)

Registry that maps entity types to their [MenuActionResolver](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md) implementations. Resolvers are injected as a Set, so adding a new entity type means adding a new resolver class — zero changes to existing code (Open/Closed Principle).

## Constructors

| | |
|---|---|
| [MenuActionResolverRegistry](-menu-action-resolver-registry.md) | [Kraken API]<br>constructor(npcResolver: [NpcMenuActionResolver](../-npc-menu-action-resolver/index.md), playerResolver: [PlayerMenuActionResolver](../-player-menu-action-resolver/index.md), tileObjectResolver: [TileObjectMenuActionResolver](../-tile-object-menu-action-resolver/index.md), widgetResolver: [WidgetMenuActionResolver](../-widget-menu-action-resolver/index.md), groundItemResolver: [GroundItemMenuActionResolver](../-ground-item-menu-action-resolver/index.md), bankItemResolver: [BankItemMenuActionResolver](../-bank-item-menu-action-resolver/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [getResolver](get-resolver.md) | [Kraken API]<br>open fun &lt;[T](get-resolver.md)&gt; [getResolver](get-resolver.md)(entityType: [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)&lt;[T](get-resolver.md)&gt;): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[MenuActionResolver](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&lt;[T](get-resolver.md)&gt;&gt; |
