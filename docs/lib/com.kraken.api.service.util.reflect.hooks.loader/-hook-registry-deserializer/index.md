//[lib](../../../index.md)/[com.kraken.api.service.util.reflect.hooks.loader](../index.md)/[HookRegistryDeserializer](index.md)

# HookRegistryDeserializer

[Kraken API]\
open class [HookRegistryDeserializer](index.md)

Custom deserializer that directly creates domain hook objects from JSON. Eliminates the need for intermediate DTO objects entirely.

## Constructors

| | |
|---|---|
| [HookRegistryDeserializer](-hook-registry-deserializer.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [deserialize](deserialize.md) | [Kraken API]<br>open fun [deserialize](deserialize.md)(json: JsonElement, type: [Type](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/reflect/Type.html), context: JsonDeserializationContext): [HookRegistry](../../com.kraken.api.service.util.reflect.hooks/-hook-registry/index.md) |
