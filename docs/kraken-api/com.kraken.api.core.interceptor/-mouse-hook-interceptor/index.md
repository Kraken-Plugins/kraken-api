//[kraken-api](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[MouseHookInterceptor](index.md)

# MouseHookInterceptor

[Kraken API]\
open class [MouseHookInterceptor](index.md)

## Constructors

| | |
|---|---|
| [MouseHookInterceptor](-mouse-hook-interceptor.md) | [Kraken API]<br>constructor(client: Client) |

## Properties

| Name | Summary |
|---|---|
| [client](client.md) | [Kraken API]<br>val [client](client.md): Client |
| [injected](injected.md) | [Kraken API]<br>open var [injected](injected.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |

## Functions

| Name | Summary |
|---|---|
| [injectHook](inject-hook.md) | [Kraken API]<br>open fun [injectHook](inject-hook.md)()<br>Redefines the obfuscated client mouse hook method so that any read of the 'llimc' field returns 0, leaving the rest of the packet building logic intact. |
| [provideZero](provide-zero.md) | [Kraken API]<br>open fun [provideZero](provide-zero.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Delegate method injected by ByteBuddy. |
