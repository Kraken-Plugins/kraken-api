//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[AbstractTask](index.md)

# AbstractTask

abstract class [AbstractTask](index.md) : [Task](../-task/index.md)

Abstract base class for Script tasks. Provides a [Context](../../com.kraken.api/-context/index.md) instance injected by Guice.

#### Inheritors

| |
|---|
| [PriorityTask](../-priority-task/index.md) |

## Constructors

| | |
|---|---|
| [AbstractTask](-abstract-task.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [execute](../-task/execute.md) | [Kraken API]<br>abstract fun [execute](../-task/execute.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Executes the task logic. |
| [status](../-task/status.md) | [Kraken API]<br>abstract fun [status](../-task/status.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Returns the name of the status for display. |
| [validate](../-task/validate.md) | [Kraken API]<br>abstract fun [validate](../-task/validate.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if this task should currently be executed. |
