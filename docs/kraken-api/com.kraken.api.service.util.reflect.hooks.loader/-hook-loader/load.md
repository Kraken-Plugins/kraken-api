//[kraken-api](../../../index.md)/[com.kraken.api.service.util.reflect.hooks.loader](../index.md)/[HookLoader](index.md)/[load](load.md)

# load

[Kraken API]\
open fun [load](load.md)(): [HookRegistry](../../com.kraken.api.service.util.reflect.hooks/-hook-registry/index.md)

Initializes the packet factory by loading packet definitions from local resources or a remote source. 

 This method attempts to load a JSON file containing packet definitions from a predefined local path. If the local file is unavailable or an exception occurs while processing it, the method falls back to retrieving the packet definitions from a remote URL.
