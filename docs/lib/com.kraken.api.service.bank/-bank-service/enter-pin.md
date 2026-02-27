//[lib](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[BankService](index.md)/[enterPin](enter-pin.md)

# enterPin

[Kraken API]\
open fun [enterPin](enter-pin.md)(pin: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Enters the bank pin using the provided 4 digits.

#### Return

boolean true if the pin was entered and false otherwise. This will return true if the pin was entered at all. This doesn't necessarily mean the pin was correct.

#### Parameters

Kraken API

| | |
|---|---|
| pin | An integer array of size 4 which contains the bank pin to enter. |
