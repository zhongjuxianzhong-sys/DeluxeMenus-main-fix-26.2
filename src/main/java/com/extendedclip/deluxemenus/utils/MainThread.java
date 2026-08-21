package com.extendedclip.deluxemenus.utils;

import com.extendedclip.deluxemenus.DeluxeMenus;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Small boundary for Bukkit's single-threaded API.
 */
public final class MainThread {

    private final BooleanSupplier enabled;
    private final BooleanSupplier primaryThread;
    private final Consumer<Runnable> scheduler;

    public MainThread(final @NotNull DeluxeMenus plugin) {
        this(
                plugin::isEnabled,
                Bukkit::isPrimaryThread,
                task -> Bukkit.getScheduler().runTask(plugin, task)
        );
    }

    MainThread(
            final @NotNull BooleanSupplier enabled,
            final @NotNull BooleanSupplier primaryThread,
            final @NotNull Consumer<Runnable> scheduler
    ) {
        this.enabled = enabled;
        this.primaryThread = primaryThread;
        this.scheduler = scheduler;
    }

    public void run(final @NotNull Runnable task) {
        if (!enabled.getAsBoolean()) {
            return;
        }

        if (primaryThread.getAsBoolean()) {
            task.run();
            return;
        }

        scheduler.accept(() -> {
            if (enabled.getAsBoolean()) {
                task.run();
            }
        });
    }

    public static void run(final @NotNull DeluxeMenus plugin, final @NotNull Runnable task) {
        new MainThread(plugin).run(task);
    }
}
