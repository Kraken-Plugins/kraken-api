package plugins.api.world;

/**
 * A capability a location offers that a test may depend on.
 *
 * <p>Tests declare facilities rather than places wherever they can. "I need a bank booth" is true of
 * several locations, so the runner is free to satisfy it without travelling; "I need Varrock East
 * Bank" forces a trip even when the player is already standing at a perfectly good bank.</p>
 */
public enum Facility {

    /** A bank booth or chest offering the "Bank" menu action. */
    BANK_BOOTH,

    /** A banker NPC offering the "Bank" menu action, as found at the Grand Exchange. */
    BANKER_NPC,

    /** A bank deposit box offering the "Deposit" menu action. */
    DEPOSIT_BOX,

    /** A Grand Exchange clerk offering the "Exchange" menu action. */
    GRAND_EXCHANGE_CLERK,

    /** A water source that fills a bucket, such as a fountain. */
    WATER_SOURCE,

    /**
     * Free-to-play combat NPCs such as Guards, Men or Goblins, used by the combat and dps tests as
     * targets with known stats.
     */
    COMBAT_NPCS_F2P,

    /** A reliably populated area, needed by tests that assert against other players. */
    OTHER_PLAYERS
}
