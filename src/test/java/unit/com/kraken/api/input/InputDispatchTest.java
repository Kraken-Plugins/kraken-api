package com.kraken.api.input;

import org.junit.jupiter.api.Test;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputDispatchTest {

    @Test
    void anActionRunOnTheEventThreadIsMarkedWhileItRuns() throws Exception {
        AtomicBoolean inside = new AtomicBoolean();
        CountDownLatch done = new CountDownLatch(1);

        InputDispatch.onEventThread(() -> {
            inside.set(InputDispatch.isSynthetic());
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(inside.get());
        AtomicBoolean after = new AtomicBoolean(true);
        EventQueue.invokeAndWait(() -> after.set(InputDispatch.isSynthetic()));
        assertFalse(after.get(), "the mark ends with the action");
    }

    @Test
    void aListenerSeesADispatchedEventAsSynthetic() throws Exception {
        AtomicBoolean synthetic = new AtomicBoolean();
        CountDownLatch pressed = new CountDownLatch(1);
        Component component = new Component() {
        };
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                synthetic.set(InputDispatch.isSynthetic());
                pressed.countDown();
            }
        });

        InputDispatch.dispatch(component, new MouseEvent(component, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 5, 5, 1, false, MouseEvent.BUTTON1));

        assertTrue(pressed.await(5, TimeUnit.SECONDS));
        assertTrue(synthetic.get());
    }

    @Test
    void theCallingThreadIsNotMarked() {
        assertFalse(InputDispatch.isSynthetic());
    }
}
