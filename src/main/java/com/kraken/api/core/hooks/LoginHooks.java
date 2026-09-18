package com.kraken.api.core.hooks;

import lombok.Value;

/**
 * Login screen hooks from {@code hooks.json}. The session id, account id, access token and refresh token are
 * mapped as the {@code static void name(String)} setter to invoke, since the client keeps each value in a
 * constant-dynamic array; the class name applies to that setter. The credential lookup is the
 * {@code static String name(String)} method the client reads every JX_* value through at startup, checking the
 * environment before the credentials file.
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
    String accessTokenClassName;
    String accessTokenMethodName;
    String refreshTokenClassName;
    String refreshTokenMethodName;
    String credentialLookupClassName;
    String credentialLookupMethodName;
    String displayNameFieldName;
    String displayNameClassName;
    String accountCheckFieldName;
    String accountCheckClassName;
    String jagexValueFieldName;
    String jagexValueClassName;
    String legacyValueFieldName;
    String legacyValueClassName;
}
