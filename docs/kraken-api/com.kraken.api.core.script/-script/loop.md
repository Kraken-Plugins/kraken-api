//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)/[loop](loop.md)

# loop

[Kraken API]\
abstract fun [loop](loop.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Executes a specific loop logic and returns an integer result based on the implementation. 

 This abstract method needs to be implemented by subclasses to define the specific behavior of the loop. 

#### Return

an integer value representing the amount of time to sleep in milliseconds. Since this is called every game tick, any value &lt;= 600 will execute on the next game tick. 

Example Usage:

```kotlin
{public class CustomScript extends Script {    @Override    public int loop() {        // Do something to automate game        return 100;    }}}
```
