//[kraken-api](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakConditions](index.md)/[runOnce](run-once.md)

# runOnce

[Kraken API]\
open fun [runOnce](run-once.md)(condition: [BreakCondition](../-break-condition/index.md)): [BreakCondition](../-break-condition/index.md)

Wraps a condition to ensure it only triggers once per session. Useful for conditions like Level Reached or Material Depleted to prevent break loops.

#### Return

A new BreakCondition that only returns true once.

#### Parameters

Kraken API

| | |
|---|---|
| condition | The condition to wrap |
