package com.kraken.api.core.hooks;

import lombok.Value;

/**
 * Security-related obfuscation hooks parsed from {@code hooks.json}. Gson allocates instances via
 * {@code Unsafe} without running any field initializers, so every value here comes from the JSON
 * resource — including {@code mouseHookDllClassName} and {@code callStackClassName}, which must be
 * present in the resource rather than defaulted in code.
 */
@Value
public class SecurityHooks {
    String mouseHookDllClassName;
    String mouseHookDllMethodName;
    String clientLogFieldName;
    String callStackClassName;
    String callStackMethodName;
    String cleanCallStackValue;
    String platformInfoClassName;
    String platformInfoMethodName;
    String callStackField;
    String agentField;
    boolean isPlatformInfoMethodStatic;
    int platformInfoMethodArgCount;
}

