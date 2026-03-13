//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[PriorityTask](index.md)

# PriorityTask

[Kraken API]\
abstract class [PriorityTask](index.md) : [AbstractTask](../-abstract-task/index.md)

Represents a task that includes a priority level, allowing tasks to be prioritized during selection or execution in workflows or script logic. 

 This class extends [AbstractTask](../-abstract-task/index.md), inheriting capabilities such as access to a Context instance for task configuration and runtime behavior. It introduces an additional abstract method for retrieving the priority of the task. 

### Key Characteristics:

- Must be subclassed to implement priority-based behavior.
- Integrates with the task execution framework through inheritance from [AbstractTask](../-abstract-task/index.md) and the [Task](../-task/index.md) interface.

### Priority Management:

 Subclasses are required to define the `getPriority()` method, which returns an integer value representing the priority level of the task. Higher priority values generally indicate tasks that should be executed earlier or given precedence over those with lower values. The specific interpretation of priority values is determined by the execution context in which this task is used.

## Constructors

| | |
|---|---|
| [PriorityTask](-priority-task.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [execute](../-task/execute.md) | [Kraken API]<br>abstract fun [execute](../-task/execute.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Executes the task logic. |
| [getPriority](get-priority.md) | [Kraken API]<br>abstract fun [getPriority](get-priority.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [status](../-task/status.md) | [Kraken API]<br>abstract fun [status](../-task/status.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Returns the name of the status for display. |
| [validate](../-task/validate.md) | [Kraken API]<br>abstract fun [validate](../-task/validate.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if this task should currently be executed. |
