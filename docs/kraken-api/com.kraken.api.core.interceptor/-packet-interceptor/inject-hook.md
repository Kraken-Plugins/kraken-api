//[kraken-api](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[PacketInterceptor](index.md)/[injectHook](inject-hook.md)

# injectHook

[Kraken API]\
open fun [injectHook](inject-hook.md)()

Modifies the bytecode of the &quot;addNode&quot; method within the client at runtime to invoke the [PacketHookAdvice](-packet-hook-advice/index.md) class whenever the method is called. This will publish the [PacketSent](../../com.kraken.api.core.interceptor.model/-packet-sent/index.md) event to the eventbus which can be net.runelite.client.eventbus.Subscribe to within plugins who need access to low-level packets.
