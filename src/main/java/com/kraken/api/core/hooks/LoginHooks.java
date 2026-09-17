package com.kraken.api.core.hooks;

import lombok.Value;

/**
 * Login screen hooks from {@code hooks.json}. The session id and account id are mapped either as a static
 * String field or, when the client keeps the value in a constant-dynamic array, as the
 * {@code static void name(String)} setter to invoke instead; the class name applies to whichever is present.
 */
@Value
public class LoginHooks {
    Integer loginIndexGarbageValue;
    String loginIndexMethodName;
    String loginIndexClassName;
    String sessionClassName;
    String sessionMethodName;
    String accountIdClassName;
    String accountIdMethodName;
    String displayNameFieldName;
    String displayNameClassName;
    String accountCheckFieldName;
    String accountCheckClassName;
    String jagexValueFieldName;
    String jagexValueClassName;
    String legacyValueFieldName;
    String legacyValueClassName;
}
