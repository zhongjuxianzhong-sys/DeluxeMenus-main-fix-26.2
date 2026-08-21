package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.utils.MainThread;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MenuHolderTest {

    @Test
    void queuedRefreshForClosedHolderDoesNotTouchInventory() {
        final DeluxeMenus plugin = mock(DeluxeMenus.class);
        final Player player = mock(Player.class);
        final Inventory inventory = mock(Inventory.class);
        final List<Runnable> queued = new ArrayList<>();
        final MainThread mainThread = mock(MainThread.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.doAnswer(invocation -> {
            queued.add(invocation.getArgument(0));
            return null;
        }).when(mainThread).run(org.mockito.ArgumentMatchers.any(Runnable.class));

        final MenuHolder holder = new MenuHolder(plugin, player, mainThread);
        holder.setInventory(inventory);
        holder.refreshMenu();

        assertEquals(1, queued.size());
        queued.getFirst().run();
        verifyNoInteractions(inventory);
    }
}
