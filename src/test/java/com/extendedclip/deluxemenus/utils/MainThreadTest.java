package com.extendedclip.deluxemenus.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MainThreadTest {

    @Test
    void primaryThreadRunsImmediately() {
        final AtomicInteger executions = new AtomicInteger();
        final List<Runnable> queued = new ArrayList<>();
        final MainThread mainThread = new MainThread(() -> true, () -> true, queued::add);

        mainThread.run(executions::incrementAndGet);

        assertEquals(1, executions.get());
        assertEquals(0, queued.size());
    }

    @Test
    void asyncThreadQueuesExactlyOnceAndRechecksPluginState() {
        final AtomicBoolean enabled = new AtomicBoolean(true);
        final AtomicInteger executions = new AtomicInteger();
        final List<Runnable> queued = new ArrayList<>();
        final MainThread mainThread = new MainThread(enabled::get, () -> false, queued::add);

        mainThread.run(executions::incrementAndGet);

        assertEquals(0, executions.get());
        assertEquals(1, queued.size());

        enabled.set(false);
        queued.getFirst().run();
        assertFalse(enabled.get());
        assertEquals(0, executions.get());
    }
}
