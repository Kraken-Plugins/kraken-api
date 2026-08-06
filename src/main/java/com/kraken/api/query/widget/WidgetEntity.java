package com.kraken.api.query.widget;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.util.StringUtils;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.widgets.Widget;

public class WidgetEntity extends AbstractEntity<Widget> {
    public WidgetEntity(Context ctx, Widget raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        Widget w = raw();
        return w != null ? w.getName() : null;
    }


    /**
     * Returns the widget's packed component id (its group and child id combined), which is the value
     * {@link WidgetQuery#withId(int)}, {@code unique()} and {@code distinctById()} key on. For the item
     * a widget holds, use {@link #getItemId()}.
     * @return the packed widget id, or -1 if the underlying widget is null.
     */
    @Override
    public int getId() {
        Widget w = raw();
        return w != null ? w.getId() : -1;
    }

    /**
     * Returns the item id of the item this widget holds.
     * @return the item id, or -1 if the widget is null or holds no item.
     */
    public int getItemId() {
        Widget w = raw();
        return w != null ? w.getItemId() : -1;
    }

    /**
     * Checks if the widget text, name, or actions match the input.
     * @param search The search string to match
     * @param exact True if only exact matches of the search string should be accepted
     * @return true if there is a match within the search string for a specific widget and false otherwise
     */
    public boolean matches(String search, boolean exact) {
        Widget raw = raw();
        if (raw == null) return false;

        if (isMatchFound(search, exact)) return true;

        // Check Actions
        if (raw.getActions() != null) {
            for (String action : raw.getActions()) {
                if (action != null) {
                    String cleanAction = StringUtils.stripColTags(action);
                    if (exact) {
                        if (cleanAction.equalsIgnoreCase(search)) return true;
                    } else {
                        if (cleanAction.toLowerCase().contains(search.toLowerCase())) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isMatchFound(String search, boolean exact) {
        Widget raw = raw();
        String cleanText = StringUtils.stripColTags(raw.getText());
        String cleanName = StringUtils.stripColTags(raw.getName());

        boolean matchFound = false;

        // Check Text and Name
        if (exact) {
            if (cleanText.equalsIgnoreCase(search) || cleanName.equalsIgnoreCase(search)) matchFound = true;
        } else {
            if (cleanText.toLowerCase().contains(search.toLowerCase())
                    || cleanName.toLowerCase().contains(search.toLowerCase())) matchFound = true;
        }
        return matchFound;
    }

    @Override
    public boolean interact(String action) {
        Widget w = raw();
        if (w == null) return false;
        return ctx.getInteractionManager().interact(w, action);
    }

    /**
     * Interacts with a widget by invoking a specified menu and action.
     *
     * <p>This method attempts to perform an interaction on a widget identified
     * by its underlying raw representation. If the raw widget is {@code null},
     * the method will immediately return {@code false}. Otherwise, it delegates
     * the interaction process to the interaction manager.</p>
     *
     * @param menu The menu option to be selected during the interaction.
     *             For example, this could represent a contextual menu option like "Use" or "Examine".
     * @param action The specific action to be invoked within the selected menu option.
     *               This typically represents the intended effect of the interaction, such as "Wield".
     *
     * @return {@code true} if the sub-action resolved and was dispatched; {@code false} if the
     *         underlying widget was {@code null} or the menu/action pair could not be resolved.
     */
    public boolean interact(String menu, String action) {
        Widget w = raw();
        if (w == null) return false;
        return ctx.getInteractionManager().interact(w, menu, action);
    }


    /**
     * Interacts with a widget using the specified action index.
     * @param packedId The packed widget id
     * @param childId The child id of the widget
     * @param itemId The item id of the widget.
     * @param action The action index to take.
     * @return True if the widget resolved and the action was dispatched, false otherwise
     */
    public boolean interact(int packedId, int childId, int itemId, int action) {
        return ctx.getInteractionManager().interact(packedId, childId, itemId, action);
    }

    /**
     * Checks if the widget is currently visible.
     *
     * <p>A widget is considered visible if it is not marked as hidden in its underlying raw state.</p>
     *
     * @return {@code true} if the widget is visible; {@code false} otherwise.
     */
    public boolean isVisible() {
        if(raw() == null) return false;
        return ctx.runOnClientThread(() -> !raw().isHidden() && !raw().isSelfHidden());
    }

    /**
     * Uses a widget on another widget. (i.e. High Alchemy)
     * @param destinationWidget The destination widget to use this entity on
     * @return True if the action is successful and false otherwise.
     */
    public boolean useOn(Widget destinationWidget) {
        Widget w = raw();
        if(w == null) return false;
        return ctx.getInteractionManager().interact(w, destinationWidget);
    }

    /**
     * Uses a widget on an NPC (i.e. Crumble Undead Spell on the Undead Spawn from Vorkath)
     * @param npc NPC to use the widget on.
     * @return True if the action was successful and false otherwise.
     */
    public boolean useOn(NPC npc) {
        Widget w = raw();
        if(w == null) return false;
        return ctx.getInteractionManager().interact(w, npc);
    }

    /**
     * Uses a widget on a Game Object (i.e. Bones on the Chaos Altar)
     * @param gameObject The Game Object to use the widget on.
     * @return True if the action was successful and false otherwise.
     */
    public boolean useOn(GameObject gameObject) {
        Widget w = raw();
        if(w == null) return false;
        return ctx.getInteractionManager().interact(w, gameObject);
    }
}