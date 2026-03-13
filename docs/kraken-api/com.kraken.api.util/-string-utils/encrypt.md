//[kraken-api](../../../index.md)/[com.kraken.api.util](../index.md)/[StringUtils](index.md)/[encrypt](encrypt.md)

# encrypt

[Kraken API]\
open fun [encrypt](encrypt.md)(plaintext: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), key: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Encrypts the given plaintext using AES/CBC/PKCS5Padding. 

 This method generates a random 16-byte IV, performs the encryption, combines the IV and the ciphertext, and returns the result as a Base64 encoded string. 

#### Return

a Base64 encoded string containing the IV followed by the encrypted bytes

#### Parameters

Kraken API

| | |
|---|---|
| plaintext | the text to encrypt |
| key | A 32 byte base64 encoded key used to encrypt the string |

#### Throws

| | |
|---|---|
| [RuntimeException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/RuntimeException.html) | if the encryption process fails |
