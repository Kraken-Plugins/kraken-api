//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[initializeInterceptors](initialize-interceptors.md)

# initializeInterceptors

[Kraken API]\
open fun [initializeInterceptors](initialize-interceptors.md)()

Initializes all supported runtime interceptors using the default configuration.

[Kraken API]\
open fun [initializeInterceptors](initialize-interceptors.md)(configuration: [InterceptorBuilder](../../com.kraken.api.core.interceptor/-interceptor-builder/index.md))

Initializes the configured runtime interceptors. Each interceptor injection is isolated so failures do not prevent plugin startup or other interceptor injections from continuing.

#### Parameters

Kraken API

| | |
|---|---|
| configuration | the interceptor configuration to apply. |
