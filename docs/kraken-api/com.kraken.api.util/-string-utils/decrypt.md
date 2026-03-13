//[kraken-api](../../../index.md)/[com.kraken.api.util](../index.md)/[StringUtils](index.md)/[decrypt](decrypt.md)

# decrypt

[Kraken API]\
open fun [decrypt](decrypt.md)(base64IvAndCiphertext: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), key: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Decrypts a Base64 encoded string containing an IV and ciphertext. 

 This method expects the input string to be the result of the [encrypt](encrypt.md) method. It extracts the IV from the first 16 bytes and decrypts the remaining bytes. 

#### Return

the decrypted plaintext string

#### Parameters

Kraken API

| | |
|---|---|
| base64IvAndCiphertext | the Base64 encoded string containing the IV and encrypted data |
| key | a 32 byte base64 encoded key for decrypting the string. |

#### Throws

| | |
|---|---|
| [RuntimeException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/RuntimeException.html) | if the decryption process fails |
