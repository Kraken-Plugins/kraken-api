# AI Integration Guide

This guide helps AI systems (e.g., copilots, assistants) understand how to use the Kraken API effectively when generating RuneLite plugin logic.

The Kraken API follows two primary interaction patterns:

* **Query System** → for dynamic entities (NPCs, players, items, objects)
* **Services** → for global/static systems (bank, movement, prayer, UI)

---

## Core Concepts

| Concept         | Description                                    |
| --------------- | ---------------------------------------------- |
| Context (`ctx`) | Entry point for querying all game entities     |
| Query System    | Used to find and filter dynamic entities       |
| Services        | Used for interacting with global systems       |
| Interactions    | Actions like `interact()`, `take()`, `wield()` |

---

## Query Execution Pattern

The Kraken API uses a fluent query system. AI systems should interpret chained calls as a sequence of filtering and actions.

### Example

```java
ctx.npcs()
    .withName("Goblin")
    .nearest()
    .interact("Attack");
```

### Execution Breakdown

1. `ctx.npcs()` → fetch all NPCs in the scene
2. `withName("Goblin")` → filter NPCs by name
3. `nearest()` → select the closest NPC
4. `interact("Attack")` → perform attack action

---

## Common Intents → API Mapping

AI systems should translate user intent into structured API calls.

---

### Attack NPC

**Intent:** "Attack a Goblin"

```java
ctx.npcs()
    .withName("Goblin")
    .nearest()
    .interact("Attack");
```

---

### Move Player

**Intent:** "Move to a location"

```java
movement.moveTo(new WorldPoint(x, y));
```

---

### Withdraw Item from Bank

**Intent:** "Withdraw item from bank"

```java
if (!bank.isOpen()) {
    bank.open();
}

ctx.bank()
    .nameContains("item")
    .first()
    .withdraw();
```

---

### Equip Item

**Intent:** "Equip item"

```java
ctx.equipment()
    .withId(itemId)
    .first()
    .wield();
```

---

### Pick Up Ground Item

**Intent:** "Pick up item from ground"

```java
ctx.groundItems()
    .within(5)
    .filter(item -> item.name().equalsIgnoreCase("Bones"))
    .first()
    .take();
```

---

## Services vs Query System

| Use Case                                | Recommended API |
| --------------------------------------- | --------------- |
| Static systems (bank, prayer, spells)   | Services        |
| Dynamic entities (NPCs, items, objects) | Query System    |

---

## Service Overview

### BankService

Handles:

* Opening and closing bank
* Withdrawing items
* Managing inventory via bank

### MovementService

Handles:

* Player movement
* Pathing to world coordinates

### PrayerService

Handles:

* Activating and deactivating prayers

---

## Structured AI Example

AI systems can interpret actions in a structured format:

```json
{
  "intent": "attack npc",
  "entity": "npc",
  "filters": ["name=Goblin"],
  "selection": "nearest",
  "action": "interact:Attack"
}
```

---

## Key Guidelines for AI Systems

* Always start with `Context (ctx)` when querying entities
* Apply filters (`withName`, `withId`, etc.) before selecting targets
* Use `.nearest()` or `.first()` to resolve a single entity
* Use `.interact()` or other action methods to perform actions
* Use **Services** for global interactions instead of queries

---

## Summary

The Kraken API is designed around:

* **Fluent querying** for dynamic entities
* **Service-based actions** for global systems

AI systems should:

1. Identify intent
2. Map intent to API pattern (Query vs Service)
3. Build a structured execution chain
4. Execute interaction

This structured approach ensures predictable and effective plugin generation.
