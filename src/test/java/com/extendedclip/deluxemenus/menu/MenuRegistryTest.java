package com.extendedclip.deluxemenus.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MenuRegistryTest {

    @Test
    void lookupIsCaseInsensitiveAndSnapshotsAreImmutable() {
        final MenuRegistry registry = new MenuRegistry();
        final Menu menu = mock(Menu.class);
        registry.registerMenu("ExampleMenu", menu);

        assertSame(menu, registry.getMenu("examplemenu").orElseThrow());
        assertSame(menu, registry.getMenu("EXAMPLEMENU").orElseThrow());
        assertSame(menu, registry.getMenu("ExampleMenu").orElseThrow());

        final Set<String> names = registry.menuNamesSnapshot();
        final Collection<Menu> menus = registry.menusSnapshot();
        assertEquals(Set.of("ExampleMenu"), names);
        assertThrows(UnsupportedOperationException.class, () -> names.add("other"));
        assertThrows(UnsupportedOperationException.class, menus::clear);

        registry.removeMenu("examplemenu");
        assertEquals(Set.of("ExampleMenu"), names);
        assertEquals(1, menus.size());
    }

    @Test
    void holderReplacementAndConditionalRemovalUseUuidIdentity() {
        final MenuRegistry registry = new MenuRegistry();
        final UUID playerId = UUID.randomUUID();
        final MenuHolder first = mock(MenuHolder.class);
        final MenuHolder second = mock(MenuHolder.class);

        registry.setHolder(playerId, first);
        registry.setHolder(playerId, second);

        assertSame(second, registry.getHolder(playerId).orElseThrow());
        assertFalse(registry.removeHolder(playerId, first));
        assertSame(second, registry.getHolder(playerId).orElseThrow());
        registry.removeHolder(playerId, second);
        assertFalse(registry.getHolder(playerId).isPresent());
    }

    @Test
    void concurrentMutationDoesNotInvalidatePublishedSnapshots() {
        final MenuRegistry registry = new MenuRegistry();
        final Collection<Menu> publishedSnapshots = new ArrayList<>();

        final CompletableFuture<Void> writer = CompletableFuture.runAsync(() ->
                IntStream.range(0, 1_000).forEach(index -> {
                    final String name = "menu-" + index;
                    registry.registerMenu(name, mock(Menu.class));
                    if ((index & 1) == 0) registry.removeMenu(name);
                })
        );
        final CompletableFuture<Void> reader = CompletableFuture.runAsync(() ->
                IntStream.range(0, 1_000).forEach(index -> publishedSnapshots.addAll(registry.menusSnapshot()))
        );

        assertDoesNotThrow(() -> {
            try {
                CompletableFuture.allOf(writer, reader).join();
            } catch (CompletionException exception) {
                throw exception.getCause();
            }
        });
    }
}
