//[lib](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Task](index.md)

# Task

interface [Task](index.md)

Represents a generic task that can be executed as part of a workflow or script logic. 

 This interface provides three primary methods that allow implementers to define: 

- Validation logic to determine whether the task should be executed.
- The core execution logic of the task.
- A status string for display or tracking purposes.

#### Inheritors

| |
|---|
| [AbstractTask](../-abstract-task/index.md) |

## Functions

| Name | Summary |
|---|---|
| [execute](execute.md) | [Kraken API]<br>abstract fun [execute](execute.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Executes the task logic. |
| [status](status.md) | [Kraken API]<br>abstract fun [status](status.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Returns the name of the status for display. |
| [validate](validate.md) | [Kraken API]<br>abstract fun [validate](validate.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if this task should currently be executed. |
