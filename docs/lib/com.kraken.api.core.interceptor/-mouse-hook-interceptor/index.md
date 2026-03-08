//[lib](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[MouseHookInterceptor](index.md)

# MouseHookInterceptor

[Kraken API]\
open class [MouseHookInterceptor](index.md)

## Constructors

| | |
|---|---|
| [MouseHookInterceptor](-mouse-hook-interceptor.md) | [Kraken API]<br>constructor(client: Client) |

## Types

| Name | Summary |
|---|---|
| [MouseHookAdvice](-mouse-hook-advice/index.md) | [Kraken API]<br>open class [MouseHookAdvice](-mouse-hook-advice/index.md)<br>Advice injected into the obfuscated mouse hook method. |

## Properties

| Name | Summary |
|---|---|
| [client](client.md) | [Kraken API]<br>val [client](client.md): Client |
| [hooks](hooks.md) | [Kraken API]<br>val [hooks](hooks.md): [MouseHooks](../../com.kraken.api.service.util.reflect.hooks/-mouse-hooks/index.md) |
| [injected](injected.md) | [Kraken API]<br>open var [injected](injected.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |

## Functions

| Name | Summary |
|---|---|
| [injectHook](inject-hook.md) | [Kraken API]<br>open fun [injectHook](inject-hook.md)()<br>Redefines the obfuscated client mouse hook method so it returns immediately without executing the original implementation. |
