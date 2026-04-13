//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[BankItemMenuActionResolver](index.md)

# BankItemMenuActionResolver

[Kraken API]\
open class [BankItemMenuActionResolver](index.md) : [MenuActionResolver](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&lt;[T](../../com.kraken.api.core.interaction/-menu-action-resolver/index.md)&gt;

## Constructors

| | |
|---|---|
| [BankItemMenuActionResolver](-bank-item-menu-action-resolver.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getEntityType](get-entity-type.md) | [Kraken API]<br>open fun [getEntityType](get-entity-type.md)(): [Class](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Class.html)&lt;[BankItemWidget](../../com.kraken.api.query.container.bank/-bank-item-widget/index.md)&gt;<br>The entity type this resolver handles. |
| [resolve](resolve.md) | [Kraken API]<br>open fun [resolve](resolve.md)(item: [BankItemWidget](../../com.kraken.api.query.container.bank/-bank-item-widget/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md)&gt;<br>Resolves the appropriate menu action for the given entity and action string. |
