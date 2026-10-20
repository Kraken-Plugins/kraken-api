package com.kraken.api.service.ui.login;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The kind of account to log in with. Each constant holds the login index the client expects for that account kind.
 */
@Getter
@AllArgsConstructor
public enum AccountType {
    /** A legacy account, logged in with login index 2. */
    LEGACY(2),
    /** A Jagex account, logged in with login index 10. */
    JAGEX(10);

    private final int loginIndex;
}
