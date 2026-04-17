//[kraken-api](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[MouseHookInterceptor](index.md)/[injectHook](inject-hook.md)

# injectHook

[Kraken API]\
open fun [injectHook](inject-hook.md)()

Redefines the obfuscated client mouse hook method so that any read of the 'llimc' field returns 0, leaving the rest of the packet building logic intact.
