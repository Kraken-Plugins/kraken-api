package com.kraken.api.service.util.dps.model;

import lombok.Value;

/**
 * A single attack option on a weapon's combat tab: its display name (e.g. "Lunge"),
 * the damage type it uses, and the stance behaviour it grants.
 */
@Value
public class AttackStyle {
    String name;
    CombatStyleType type;
    CombatStance stance;
}
