//[kraken-api](../../../index.md)/[com.kraken.api.simulation.tree](../index.md)/[DecisionTreeSearch](index.md)

# DecisionTreeSearch

[Kraken API]\
class [DecisionTreeSearch](index.md)

Searches a generated simulation tree and returns the best root decision.

## Constructors

| | |
|---|---|
| [DecisionTreeSearch](-decision-tree-search.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [NodeEvaluator](-node-evaluator/index.md) | [Kraken API]<br>@[FunctionalInterface](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/FunctionalInterface.html)<br>interface [NodeEvaluator](-node-evaluator/index.md)<br>Scores a tree node; larger is better. |
| [Result](-result/index.md) | [Kraken API]<br>class [Result](-result/index.md)<br>Search output for the root decision. |

## Functions

| Name | Summary |
|---|---|
| [search](search.md) | [Kraken API]<br>open fun [search](search.md)(tree: [SimulationTree](../-simulation-tree/index.md), evaluator: [DecisionTreeSearch.NodeEvaluator](-node-evaluator/index.md)): [DecisionTreeSearch.Result](-result/index.md)<br>Searches a generated tree. |
