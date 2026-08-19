package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.dialogue.DialogueService;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.TransportArrival;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.WarningWidgets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Crosses a transport that answers with a conversation: boats, ships, charter ships and magic carpets.
 *
 * <p>The first click already carries the destination for most of these — the dataset's menu option is
 * something like {@code "Brimhaven"} on Captain Barnaby — so the conversation that follows is usually
 * a confirmation. What it asks varies enough that the handler works through it generically: prefer an
 * option naming the destination, otherwise accept, otherwise continue.</p>
 */
@Slf4j
public class DialogueTransportHandler extends ObjectTransportHandler {

    /** How long to wait for the conversation to open after the click. */
    private static final long DIALOGUE_TIMEOUT_MS = 5_000;

    /** How many exchanges to work through before deciding the conversation is not progressing. */
    private static final int MAX_DIALOGUE_STEPS = 12;

    /** Options that accept whatever was offered, tried in order. */
    private static final String[] ACCEPTING_OPTIONS = {"Yes", "Okay", "Ok", "Continue", "Travel", "Board"};

    @Override
    public boolean execute(TransportContext context) {
        WorldPoint before = context.playerLocation();
        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        WarningWidgets.dismiss(context.getCtx());

        // A destination-named option such as Musa Point on the Locations sailor teleports without
        // opening a conversation. Waiting the full dialogue timeout would stall on the island.
        if (TransportArrival.crossed(before, context.playerLocation(), context.getDestination())) {
            return true;
        }

        DialogueService dialogue = context.getCtx().getService(DialogueService.class);
        SleepService.sleepUntil(dialogue::isDialoguePresent, DIALOGUE_TIMEOUT_MS);

        for (int step = 0; step < MAX_DIALOGUE_STEPS && dialogue.isDialoguePresent(); step++) {
            if (!advance(dialogue, context)) {
                break;
            }
            SleepService.sleepFor(1);
        }

        return true;
    }

    /**
     * Takes one step through the conversation, preferring the option that names where we are going.
     *
     * @return false when nothing could be selected, which ends the loop
     */
    private boolean advance(DialogueService dialogue, TransportContext context) {
        String destinationOption = destinationOption(context);
        if (destinationOption != null && dialogue.isOptionPresent(destinationOption)) {
            return dialogue.selectOption(destinationOption);
        }

        for (String option : ACCEPTING_OPTIONS) {
            if (dialogue.isOptionPresent(option)) {
                return dialogue.selectOption(option);
            }
        }

        return dialogue.continueDialogue();
    }

    /**
     * Works out what the destination is called, so the right conversation option can be picked.
     *
     * <p>Display info is the port name the dataset recorded ({@code "Musa Point"}). The parsed menu
     * option is the same after the entity name is recovered, but the first-word split of a multi-word
     * destination yields {@code "Musa"}, so display info is tried first.</p>
     */
    private String destinationOption(TransportContext context) {
        String displayInfo = context.getDisplayInfo();
        if (displayInfo != null && !displayInfo.isEmpty()) {
            int separator = displayInfo.lastIndexOf(':');
            return separator >= 0 ? displayInfo.substring(separator + 1).trim() : displayInfo;
        }

        if (context.getObjectInfo() != null && !context.getObjectInfo().getMenuOption().isEmpty()) {
            return context.getObjectInfo().getMenuOption();
        }

        return null;
    }
}
