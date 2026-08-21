package com.extendedclip.deluxemenus.menu;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

final class MenuRegistry {

    private final Map<String, RegisteredMenu> menus = new ConcurrentHashMap<>();
    private final Map<UUID, MenuHolder> holders = new ConcurrentHashMap<>();
    private final Map<UUID, Menu> lastOpenedMenus = new ConcurrentHashMap<>();

    void registerMenu(final @NotNull String name, final @NotNull Menu menu) {
        menus.put(normalize(name), new RegisteredMenu(name, menu));
    }

    Optional<Menu> getMenu(final @NotNull String name) {
        return Optional.ofNullable(menus.get(normalize(name))).map(RegisteredMenu::menu);
    }

    boolean isCurrentMenu(final @NotNull String name, final @NotNull Menu menu) {
        final RegisteredMenu registered = menus.get(normalize(name));
        return registered != null && registered.menu() == menu;
    }

    Optional<Menu> removeMenu(final @NotNull String name) {
        return Optional.ofNullable(menus.remove(normalize(name))).map(RegisteredMenu::menu);
    }

    boolean removeMenu(final @NotNull String name, final @NotNull Menu menu) {
        final RegisteredMenu registered = menus.get(normalize(name));
        return registered != null && registered.menu() == menu
                && menus.remove(normalize(name), registered);
    }

    int menuCount() {
        return menus.size();
    }

    Set<String> menuNamesSnapshot() {
        return Set.copyOf(menus.values().stream()
                .map(RegisteredMenu::name)
                .collect(Collectors.toSet()));
    }

    Collection<Menu> menusSnapshot() {
        return List.copyOf(menus.values().stream().map(RegisteredMenu::menu).toList());
    }

    void clearMenus() {
        menus.clear();
    }

    Optional<MenuHolder> getHolder(final @NotNull UUID playerId) {
        return Optional.ofNullable(holders.get(playerId));
    }

    void setHolder(final @NotNull UUID playerId, final @NotNull MenuHolder holder) {
        holders.put(playerId, holder);
    }

    boolean isCurrentHolder(final @NotNull UUID playerId, final @NotNull MenuHolder holder) {
        return holders.get(playerId) == holder;
    }

    boolean removeHolder(final @NotNull UUID playerId, final @NotNull MenuHolder holder) {
        return holders.remove(playerId, holder);
    }

    Collection<MenuHolder> holdersSnapshot() {
        return List.copyOf(holders.values());
    }

    void rememberLastMenu(final @NotNull UUID playerId, final @NotNull Menu menu) {
        lastOpenedMenus.put(playerId, menu);
    }

    Optional<Menu> getLastMenu(final @NotNull UUID playerId) {
        return Optional.ofNullable(lastOpenedMenus.get(playerId));
    }

    void clearAll() {
        menus.clear();
        holders.clear();
        lastOpenedMenus.clear();
    }

    private static String normalize(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private record RegisteredMenu(String name, Menu menu) {
    }
}
