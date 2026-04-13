//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[BankItemMenuActionResolver](index.md)/[resolve](resolve.md)

# resolve

[Kraken API]\
open fun [resolve](resolve.md)(item: [BankItemWidget](../../com.kraken.api.query.container.bank/-bank-item-widget/index.md), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md)&gt;

Resolves the appropriate menu action for the given entity and action string.

#### Return

An Optional containing the resolved action, or empty if resolution failed

#### Parameters

Kraken API

| | |
|---|---|
| entity | The game entity to interact with |
| action | The action string (e.g. &quot;Attack&quot;, &quot;Talk-to&quot;, &quot;Examine&quot;) |
