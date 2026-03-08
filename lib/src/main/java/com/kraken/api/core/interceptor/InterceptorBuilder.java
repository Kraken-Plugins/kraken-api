package com.kraken.api.core.interceptor;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration describing which runtime interceptors should be applied to the client.
 */
@Getter
@Builder(setterPrefix = "with")
public class InterceptorBuilder {

    @Builder.Default
    private final boolean packetInterceptor = true;

    @Builder.Default
    private final boolean mouseHookInterceptor = true;
}
