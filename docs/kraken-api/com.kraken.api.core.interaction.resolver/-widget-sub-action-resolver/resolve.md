//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[WidgetSubActionResolver](index.md)/[resolve](resolve.md)

# resolve

[Kraken API]\
open fun [resolve](resolve.md)(widget: Widget, primaryMenu: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), subActionName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[ResolvedMenuAction](../../com.kraken.api.core.interaction.model/-resolved-menu-action/index.md)&gt;

Resolves a widget sub-action menu interaction, i.e. the nested option menu like Max cape teleports, games necklace teleports, or fairy ring sub options.

#### Return

The resolved sub-action menu as an optional

#### Parameters

Kraken API

| | |
|---|---|
| widget | The widget to interact with (e.g. Ring of Dueling in inventory) |
| primaryMenu | The primary action label (e.g. &quot;Rub&quot;) |
| subActionName | The sub-action label (e.g. &quot;Fortis Colosseum&quot;) |
