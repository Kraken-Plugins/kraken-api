//[kraken-api](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[DoActionInterceptor](index.md)

# DoActionInterceptor

[Kraken API]\
open class [DoActionInterceptor](index.md)

## Constructors

| | |
|---|---|
| [DoActionInterceptor](-do-action-interceptor.md) | [Kraken API]<br>constructor(client: Client) |

## Types

| Name | Summary |
|---|---|
| [DoActionAdvice](-do-action-advice/index.md) | [Kraken API]<br>open class [DoActionAdvice](-do-action-advice/index.md)<br>Advice injected into the obfuscated doAction method. |

## Properties

| Name | Summary |
|---|---|
| [client](client.md) | [Kraken API]<br>val [client](client.md): Client |
| [hooks](hooks.md) | [Kraken API]<br>val [hooks](hooks.md): [DoActionHooks](../../com.kraken.api.service.util.reflect.hooks/-do-action-hooks/index.md) |
| [injected](injected.md) | [Kraken API]<br>open var [injected](injected.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |

## Functions

| Name | Summary |
|---|---|
| [injectHook](inject-hook.md) | [Kraken API]<br>open fun [injectHook](inject-hook.md)()<br>Redefines the obfuscated client doAction method to log arguments on entry without interrupting the original execution. |
