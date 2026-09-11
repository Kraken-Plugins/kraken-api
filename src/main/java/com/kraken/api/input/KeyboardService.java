package com.kraken.api.input;


import com.kraken.api.service.util.SleepService;
import net.runelite.api.Client;
import net.runelite.api.GameState;

import javax.inject.Inject;
import java.awt.Canvas;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.CHAR_UNDEFINED;

public class KeyboardService {

    @Inject
    private Client client;
    
    /**
     * Types a single character.
     * Useful for things like Bank Pins where you want control over the timing between digits.
     * @param c the character to type
     */
    public void typeChar(char c) {
        dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c);
    }

    /**
     * Types a string with a customizable sleep between characters.
     * @param text the string to type
     * @param minSleep The minimum the thread should be slept between key strokes
     * @param maxSleep The max the thread should be slept between key strokes
     */
    public void typeString(String text, int minSleep, int maxSleep) {
        for (char c : text.toCharArray()) {
            dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c);
            SleepService.sleep(minSleep, maxSleep);
        }
    }

    /**
     * Queues a KeyEvent for the canvas on the event dispatch thread.
     *
     * <p>The canvas is made focusable for the duration of the dispatch when it is not already, because
     * AWT's focus manager drops key events targeted at a component that cannot own focus. Both the focus
     * toggle and the dispatch run on the EDT so they cannot interleave with real input handling.</p>
     *
     * @param id       the KeyEvent type (e.g. KEY_TYPED, KEY_PRESSED, etc.)
     * @param keyCode  the key code from {@link KeyEvent}
     * @param keyChar  the character to type, if applicable
     */
    private void dispatchKeyEvent(int id, int keyCode, char keyChar) {
        Canvas canvas = client.getCanvas();
        if (canvas == null) return;
        KeyEvent event = new KeyEvent(canvas, id, System.currentTimeMillis(), 0, keyCode, keyChar);
        InputDispatch.onEventThread(() -> {
            boolean originalFocus = canvas.isFocusable();
            if (!originalFocus) canvas.setFocusable(true);
            try {
                canvas.dispatchEvent(event);
            } finally {
                if (!originalFocus) canvas.setFocusable(false);
            }
        });
    }

    /**
     * Types out a string character-by-character using KEY_TYPED events with a randomized pause between
     * characters.
     *
     * @param word the string to type into the game
     */
    public void typeString(final String word) {
        for (char c : word.toCharArray())
        {
            dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, c);
            SleepService.sleep(100, 200);
        }
    }

    /**
     * Simulates pressing a single character using a KEY_TYPED event.
     *
     * @param key the character to press
     */
    public void keyPress(final char key) {
        dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, key);
    }

    /**
     * Simulates holding the Shift key using a KEY_PRESSED event.
     */
    public void holdShift() {
        dispatchKeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.VK_SHIFT, CHAR_UNDEFINED);
    }

    /**
     * Simulates releasing the Shift key using a KEY_RELEASED event.
     */
    public void releaseShift() {
        dispatchKeyEvent(KeyEvent.KEY_RELEASED, KeyEvent.VK_SHIFT, CHAR_UNDEFINED);
    }

    /**
     * Simulates holding down a key using a KEY_PRESSED event.
     *
     * @param key the key code from {@link KeyEvent}
     */
    public void keyHold(int key) {
        dispatchKeyEvent(KeyEvent.KEY_PRESSED, key, CHAR_UNDEFINED);
    }

    /**
     * Simulates releasing a key using a KEY_RELEASED event.
     *
     * @param key the key code from {@link KeyEvent}
     */
    public void keyRelease(int key) {
        dispatchKeyEvent(KeyEvent.KEY_RELEASED, key, CHAR_UNDEFINED);
    }

    /**
     * Simulates pressing and releasing a key in quick succession.
     *
     * @param key the key code from {@link KeyEvent}
     */
    public void keyPress(int key) {
        keyHold(key);
        keyRelease(key);
    }

    /**
     * Simulates pressing the Enter key.
     * If the player is not logged in, this uses KEY_TYPED to avoid auto-login triggers.
     */
    public void enter() {
        if (!(client.getGameState() == GameState.LOGGED_IN)) {
            dispatchKeyEvent(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, '\n');
            return;
        }

        keyPress(KeyEvent.VK_ENTER);
    }
}
