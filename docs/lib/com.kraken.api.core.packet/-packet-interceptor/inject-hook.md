//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketInterceptor](index.md)/[injectHook](inject-hook.md)

# injectHook

[Kraken API]\
open fun [injectHook](inject-hook.md)()

Modifies the bytecode of the &quot;addNode&quot; method within the client at runtime to invoke the [PacketHookAdvice](-packet-hook-advice/index.md) class whenever the method is called. This will publish the [PacketSent](../../com.kraken.api.core.packet.model/-packet-sent/index.md) event to the eventbus which can be net.runelite.client.eventbus.Subscribe to within plugins who need access to low level packets.

#### Throws

| | |
|---|---|
| [Exception](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Exception.html) | Throws an Illegal state exception if the hook is not able to be injected. |
