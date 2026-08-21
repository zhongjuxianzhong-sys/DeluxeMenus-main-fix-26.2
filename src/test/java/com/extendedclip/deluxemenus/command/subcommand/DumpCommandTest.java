package com.extendedclip.deluxemenus.command.subcommand;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.utils.MainThread;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DumpCommandTest {

    @Test
    void completionMessagesAreDispatchedThroughMainThread() {
        final DeluxeMenus plugin = mock(DeluxeMenus.class);
        final CommandSender sender = mock(CommandSender.class);
        final MainThread mainThread = mock(MainThread.class);
        final ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);

        DumpCommand.handleCompletion(plugin, sender, "abc123", null, mainThread);

        verify(mainThread).run(task.capture());
        verifyNoInteractions(plugin);
        task.getValue().run();
        verify(plugin).sms(org.mockito.ArgumentMatchers.eq(sender), any(Component.class));
    }
}
