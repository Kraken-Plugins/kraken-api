package com.kraken.api.core.hooks;

import lombok.Value;

@Value
public class SecurityHooks {
    String mouseHookDllClassName = "client";
    String mouseHookDllMethodName;
    String clientLogFieldName;
    String callStackClassName = "client";
    String callStackMethodName;
    String cleanCallStackValue;
    String platformInfoClassName;
    String platformInfoMethodName;
    String callStackField;
    String agentField;
    boolean isPlatformInfoMethodStatic;
    int platformInfoMethodArgCount;
}

