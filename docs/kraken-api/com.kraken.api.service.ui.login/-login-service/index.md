//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.login](../index.md)/[LoginService](index.md)

# LoginService

[Kraken API]\
open class [LoginService](index.md)

## Constructors

| | |
|---|---|
| [LoginService](-login-service.md) | [Kraken API]<br>constructor(reflectionService: [ReflectionService](../../com.kraken.api.service.util.reflect/-reflection-service/index.md), client: Client, clientThread: ClientThread) |

## Functions

| Name | Summary |
|---|---|
| [loadProfileFromCredentials](load-profile-from-credentials.md) | [Kraken API]<br>open fun [loadProfileFromCredentials](load-profile-from-credentials.md)(): [Profile](../-profile/index.md)<br>Loads profile credentials from the RuneLite credentials file. |
| [login](login.md) | [Kraken API]<br>open fun [login](login.md)()<br>Loads Jagex account credentials and logs into the client. |
| [loginWithJagexAccount](login-with-jagex-account.md) | [Kraken API]<br>open fun [loginWithJagexAccount](login-with-jagex-account.md)(profile: [Profile](../-profile/index.md), doLogin: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Logs into a Jagex account using the provided profile. |
| [loginWithLegacyAccount](login-with-legacy-account.md) | [Kraken API]<br>open fun [loginWithLegacyAccount](login-with-legacy-account.md)(username: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), password: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), doLogin: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Logs into the game client using legacy (username/password) credentials. |
