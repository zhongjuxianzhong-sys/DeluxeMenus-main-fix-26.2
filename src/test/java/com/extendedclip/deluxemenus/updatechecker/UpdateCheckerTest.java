package com.extendedclip.deluxemenus.updatechecker;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.utils.MainThread;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateCheckerTest {

    @Test
    void resultIsPublishedOnlyOnce() {
        final DeluxeMenus plugin = mock(DeluxeMenus.class);
        final PluginDescriptionFile description = mock(PluginDescriptionFile.class);
        when(plugin.getDescription()).thenReturn(description);
        when(description.getVersion()).thenReturn("1.0.0");
        final UpdateChecker checker = new UpdateChecker(plugin, mock(MainThread.class));

        checker.publishResult(new UpdateChecker.CheckResult("2.0.0", true, false));
        checker.publishResult(new UpdateChecker.CheckResult("3.0.0", true, false));

        assertTrue(checker.updateAvailable());
        assertEquals("2.0.0", checker.getLatestVersion());
    }
}
