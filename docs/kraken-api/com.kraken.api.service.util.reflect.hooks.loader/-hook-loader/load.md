//[kraken-api](../../../index.md)/[com.kraken.api.service.util.reflect.hooks.loader](../index.md)/[HookLoader](index.md)/[load](load.md)

# load

[Kraken API]\
open fun [load](load.md)(): [HookRegistry](../../com.kraken.api.service.util.reflect.hooks/-hook-registry/index.md)

Loads the `HookRegistry` from the predefined JSON resource file. 

 If the `HookRegistry` has already been loaded and cached, this method returns the cached instance. Otherwise, it deserializes the resource file located at `/reflection_hooks.json` into a `HookRegistry` instance using a Gson object configured with a custom [HookRegistryDeserializer](../-hook-registry-deserializer/index.md). 

 In case of an error during the loading or deserialization process, `null` is returned, and an appropriate error message is logged.

#### Return

the loaded [HookRegistry](../../com.kraken.api.service.util.reflect.hooks/-hook-registry/index.md) instance, or `null` if an error occurs
