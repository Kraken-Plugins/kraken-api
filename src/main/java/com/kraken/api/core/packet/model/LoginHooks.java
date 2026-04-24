package com.kraken.api.core.packet.model;

import lombok.Value;

@Value
public class LoginHooks {
    Integer loginIndexGarbageValue;
    String loginIndexMethodName;
    String loginIndexClassName;
    String sessionFieldName;
    String sessionClassName;
    String accountIdFieldName;
    String accountIdClassName;
    String displayNameFieldName;
    String displayNameClassName;
    String accountCheckFieldName;
    String accountCheckClassName;
    String jagexValueFieldName;
    String jagexValueClassName;
    String legacyValueFieldName;
    String legacyValueClassName;
}
