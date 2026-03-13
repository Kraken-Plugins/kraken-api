//[kraken-api](../../../index.md)/[com.kraken.api.service.camera](../index.md)/[CameraService](index.md)/[isAngleGood](is-angle-good.md)

# isAngleGood

[Kraken API]\
open fun [isAngleGood](is-angle-good.md)(targetAngle: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), desiredMaxAngle: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

#  Checks if the angle to the target is within the desired max angle 

 The desired max angle should not go over 80-90 degrees as the target will be out of view

#### Return

true if the angle to the target is within the desired max angle

#### Parameters

Kraken API

| | |
|---|---|
| targetAngle | the angle to the target |
| desiredMaxAngle | the maximum angle to the target (Should be a positive number) |
