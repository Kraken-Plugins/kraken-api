package com.kraken.api.service.util.dps.search;

import com.kraken.api.service.util.dps.DpsResult;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearCategory;
import com.kraken.api.service.util.dps.model.GearSlot;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * The outcome of a smart gear search: the best loadout found overall, the best loadout
 * found per combat style, and the item changes needed to reach the best loadout.
 */
@Value
@Builder
public class GearSearchResult {

    /** The best result found across all searched styles, or null when nothing could hit. */
    DpsResult best;

    /** The best result found for each searched combat style class. */
    Map<GearCategory, DpsResult> bestPerStyle;

    /** Items that must be equipped (by slot) to move from the current setup to the best one. */
    Map<GearSlot, EquipmentItem> itemsToEquip;

    /** Slots that should be emptied to reach the best loadout. */
    List<GearSlot> slotsToRemove;

    /** Total number of full DPS evaluations performed during the search. */
    int evaluations;
}
