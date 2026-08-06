package com.kraken.api.service.ui.login;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.hooks.LoginHooks;
import com.kraken.api.service.util.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
@Singleton
public class LoginService {

    private final LoginHooks hooks;
    private final ReflectionService reflectionService;
    private final Client client;
    private final ClientThread clientThread;

    private static final Path CREDENTIALS_FILE = RuneLite.RUNELITE_DIR.toPath().resolve("credentials.properties");

    @Inject
    public LoginService(ReflectionService reflectionService, Client client, ClientThread clientThread) {
        this.hooks = HooksLoader.getLoginHooks();

        if (this.hooks == null) {
            log.error("Failed to load login hook metadata from hooks.json. Login state injection will not work.");
        }

        this.reflectionService = reflectionService;
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Sets the login screen index using reflection.
     * Handles garbage value parameters automatically via ReflectionService.
     */
    private void setLoginIndex(int index) {
        log.debug("Setting login index to {}", index);
        reflectionService.invoke(
                hooks.getLoginIndexClassName(),
                hooks.getLoginIndexMethodName(),
                hooks.getLoginIndexGarbageValue(),
                null,
                index
        );
    }

    /**
     * Loads profile credentials from the RuneLite credentials file.
     * @return Profile the character profile loaded from the credentials file.
     */
    public Profile loadProfileFromCredentials() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CREDENTIALS_FILE.toFile())) {
            props.load(fis);
        } catch (IOException e) {
            log.error("Failed to load credentials from {}", CREDENTIALS_FILE, e);
            return null;
        }

        String sessionId = props.getProperty("JX_SESSION_ID");
        String characterId = props.getProperty("JX_CHARACTER_ID");
        String characterName = props.getProperty("JX_DISPLAY_NAME");

        if (sessionId == null || characterId == null || characterName == null) {
            log.warn("Missing required Jagex account credentials in {}", CREDENTIALS_FILE);
            return null;
        }

        return Profile.builder()
                .isJagexAccount(true)
                .characterId(characterId)
                .sessionId(sessionId)
                .characterName(characterName)
                .build();
    }

    /**
     * Loads Jagex account credentials and logs into the client.
     * Does NOT support legacy accounts. Use {@link #loginWithLegacyAccount} instead.
     */
    public void login() {
        Profile profile = loadProfileFromCredentials();
        if (profile != null) {
            loginWithJagexAccount(profile, true);
        } else {
            log.error("Failed to load profile from credentials");
        }
    }

    /**
     * Logs into a Jagex account using the provided profile.
     *
     * @param profile Profile containing Jagex account credentials
     * @param doLogin If true, triggers actual login; if false, only sets fields
     */
    public void loginWithJagexAccount(Profile profile, boolean doLogin) {
        if (profile == null) {
            log.error("Cannot login with null profile");
            return;
        }
        applyLoginState(AccountType.JAGEX, profile, doLogin);
    }

    /**
     * Logs into the game client using legacy (username/password) credentials.
     *
     * @param username The username to login with
     * @param password The password to login with
     * @param doLogin If true, triggers actual login; if false, only sets fields
     */
    public void loginWithLegacyAccount(String username, String password, boolean doLogin) {
        Profile legacyProfile = Profile.builder()
                .username(username)
                .password(password)
                .isJagexAccount(false)
                .build();

        applyLoginState(AccountType.LEGACY, legacyProfile, doLogin);
    }

    /**
     * Unified method to inject login data via reflection.
     * All reflection complexity is handled by ReflectionService.
     */
    private void applyLoginState(AccountType type, Profile profile, boolean doLogin) {
        clientThread.invokeLater(() -> {
            if (hooks == null) {
                log.error("Cannot apply login state - login hook metadata is unavailable");
                return;
            }

            if (client.getGameState() != GameState.LOGIN_SCREEN) {
                log.warn("Cannot apply login state - not on login screen");
                return;
            }

            // Set username/password based on account type
            if (type == AccountType.JAGEX) {
                client.setUsername("");
                client.setPassword("");
            } else {
                client.setUsername(profile.getUsername());
                client.setPassword(profile.getPassword());
            }

            boolean success = true;

            // Set the login screen index
            try {
                setLoginIndex(type.getLoginIndex());
            } catch (Exception e) {
                log.error("Failed to set login index", e);
                success = false;
            }

            // Inject Jagex-specific fields (session, account ID, display name)
            if (type == AccountType.JAGEX) {
                success &= setFieldSafely(
                        hooks.getSessionClassName(),
                        hooks.getSessionFieldName(),
                        profile.getSessionId(),
                        "session ID"
                );
                success &= setFieldSafely(
                        hooks.getAccountIdClassName(),
                        hooks.getAccountIdFieldName(),
                        profile.getCharacterId(),
                        "account ID"
                );
                success &= setFieldSafely(
                        hooks.getDisplayNameClassName(),
                        hooks.getDisplayNameFieldName(),
                        profile.getCharacterName(),
                        "display name"
                );
            } else {
                // For legacy accounts, clear these fields
                success &= setFieldSafely(
                        hooks.getSessionClassName(),
                        hooks.getSessionFieldName(),
                        null,
                        "session ID"
                );
                success &= setFieldSafely(
                        hooks.getAccountIdClassName(),
                        hooks.getAccountIdFieldName(),
                        null,
                        "account ID"
                );
                success &= setFieldSafely(
                        hooks.getDisplayNameClassName(),
                        hooks.getDisplayNameFieldName(),
                        null,
                        "display name"
                );
            }

            // Set the account type check field
            success &= setAccountTypeCheck(type);

            // Trigger login if all injections succeeded
            if (success && doLogin) {
                log.info("Login state applied successfully, triggering login for {} account", type);
                client.setGameState(GameState.LOGGING_IN);
            } else if (!success) {
                log.error("Login state application failed, aborting login");
            }
        });
    }

    /**
     * Sets a static field value with error handling and logging.
     *
     * @return true if successful, false otherwise
     */
    private boolean setFieldSafely(String className, String fieldName, Object value, String fieldDescription) {
        boolean set = reflectionService.setFieldValue(className, fieldName, null, value);
        if (set) {
            log.debug("Set {} successfully", fieldDescription);
        } else {
            log.error("Failed to set {}", fieldDescription);
        }
        return set;
    }

    /**
     * Sets the account type check field by reading from the appropriate source field
     * (Jagex vs Legacy) and copying it to the client.
     *
     * This handles the complex logic of:
     * 1. Reading the account type object from either jagexAccountType or legacyAccountType
     * 2. Writing it to the client's accountCheck field
     */
    private boolean setAccountTypeCheck(AccountType type) {
        try {
            String sourceClassName = type == AccountType.JAGEX
                    ? hooks.getJagexValueClassName()
                    : hooks.getLegacyValueClassName();
            String sourceFieldName = type == AccountType.JAGEX
                    ? hooks.getJagexValueFieldName()
                    : hooks.getLegacyValueFieldName();

            // Read the account type object from the static field
            Object accountTypeObject = reflectionService.getFieldValue(sourceClassName, sourceFieldName, null);

            if (accountTypeObject == null) {
                log.error("Failed to read account type object from {}.{}", sourceClassName, sourceFieldName);
                return false;
            }

            // Write it to the client's account check field
            boolean set = reflectionService.setFieldValue(
                    hooks.getAccountCheckClassName(),
                    hooks.getAccountCheckFieldName(),
                    null,
                    accountTypeObject
            );
            if (!set) {
                log.error("Failed to set account type check for {} account", type);
                return false;
            }
            log.debug("Set account type check for {} account", type);
            return true;

        } catch (Exception e) {
            log.error("Failed to set account type check field", e);
            return false;
        }
    }
}
