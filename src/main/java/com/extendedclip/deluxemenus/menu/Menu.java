package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.events.DeluxeMenusOpenMenuEvent;
import com.extendedclip.deluxemenus.events.DeluxeMenusPreOpenMenuEvent;
import com.extendedclip.deluxemenus.menu.command.RegistrableMenuCommand;
import com.extendedclip.deluxemenus.menu.options.MenuOptions;
import com.extendedclip.deluxemenus.requirement.RequirementList;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.extendedclip.deluxemenus.utils.MainThread;
import com.extendedclip.deluxemenus.utils.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Menu {

    private static final MenuRegistry REGISTRY = new MenuRegistry();

    private final DeluxeMenus plugin;
    private final MenuOptions options;
    private final Map<Integer, TreeMap<Integer, MenuItem>> items;
    // menu path starting from the plugin directory
    private final String path;

    private RegistrableMenuCommand command = null;

    public Menu(
            final @NotNull DeluxeMenus plugin,
            final @NotNull MenuOptions options,
            final @NotNull Map<Integer, TreeMap<Integer, MenuItem>> items,
            final @NotNull String path
    ) {
        this.plugin = plugin;
        this.options = options;
        this.items = items;
        this.path = path;

        if (this.options.registerCommands()) {
            this.command = new RegistrableMenuCommand(plugin, this);
            this.command.register();
        }

        REGISTRY.registerMenu(this.options.name(), this);
    }

    public static void unload(final @NotNull DeluxeMenus plugin, final @NotNull String name) {
        if (!Bukkit.isPrimaryThread()) {
            MainThread.run(plugin, () -> unload(plugin, name));
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInMenu(p, name)) {
                closeMenu(plugin, p, true);
            }
        }

        Optional<Menu> optionalMenu = Menu.getMenuByName(name);
        if (optionalMenu.isEmpty()) {
            return;
        }

        final Menu loadedMenu = optionalMenu.get();
        loadedMenu.unregisterCommand();
        REGISTRY.removeMenu(loadedMenu.options().name(), loadedMenu);
    }

    public static void unload(final @NotNull DeluxeMenus plugin) {
        if (!Bukkit.isPrimaryThread()) {
            MainThread.run(plugin, () -> unload(plugin));
            return;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isInMenu(p)) {
                closeMenu(plugin, p, true);
            }
        }
        for (Menu menu : Menu.getAllMenus()) {
            menu.unregisterCommand();
        }
        REGISTRY.clearAll();
    }

    private void unregisterCommand() {
        if (this.command != null) {
            this.command.unregister();
        }

        // WARNING! A reference to the command is stored by CraftBukkit for their `/help` command. There is currently
        // no way to remove this reference!
        this.command = null;
    }

    public static void unloadForShutdown(final @NotNull DeluxeMenus plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInMenu(player)) {
                closeMenuForShutdown(plugin, player);
            }
        }
        REGISTRY.clearAll();
    }

    public static int getLoadedMenuSize() {
        return REGISTRY.menuCount();
    }

    public static @NotNull Set<String> getAllMenuNames() {
        return REGISTRY.menuNamesSnapshot();
    }

    public static @NotNull Collection<Menu> getAllMenus() {
        return REGISTRY.menusSnapshot();
    }

    // Menus need to be stored in a list because config.yml can contain multiple menus.
    // This can be changed once we remove support for menus inside the config file.
    public static @NotNull TreeMap<String, List<Menu>> getPathSortedMenus() {
        return REGISTRY.menusSnapshot().stream().map(m -> Map.entry(m.path(), m)).collect(
                TreeMap::new, (tree, entry) -> {
                    final List<Menu> list = tree.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                    list.add(entry.getValue());
                    tree.put(entry.getKey(), list);
                },
                (tree1, tree2) -> {
                    for (Entry<String, List<Menu>> entry : tree2.entrySet()) {
                        final List<Menu> list = tree1.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
                        list.addAll(entry.getValue());
                        tree1.put(entry.getKey(), list);
                    }
                }
        );
    }

    public static @NotNull Optional<Menu> getMenuByName(final @NotNull String name) {
        return REGISTRY.getMenu(name);
    }

    public static @NotNull Optional<Menu> getMenuByCommand(final @NotNull String command) {
        return REGISTRY.menusSnapshot().stream().filter(m -> m.getMenuCommandUsed(command).isPresent()).findFirst();
    }

    public static boolean isMenuCommand(final @NotNull String command) {
        return getMenuByCommand(command).isPresent();
    }

    public static boolean isInMenu(final @NotNull Player player) {
        return REGISTRY.getHolder(player.getUniqueId()).isPresent();
    }

    public static boolean isInMenu(final @NotNull Player player, final @NotNull String menu) {
        return REGISTRY.getHolder(player.getUniqueId())
                .map(MenuHolder::getMenuName)
                .map(name -> name.equalsIgnoreCase(menu))
                .orElse(false);
    }

    public static Optional<MenuHolder> getMenuHolder(final @NotNull Player player) {
        return REGISTRY.getHolder(player.getUniqueId());
    }

    static boolean isCurrentHolder(final @NotNull MenuHolder holder) {
        return REGISTRY.isCurrentHolder(holder.getViewer().getUniqueId(), holder);
    }

    public static Optional<Menu> getOpenMenu(final @NotNull Player player) {
        return getMenuHolder(player).flatMap(MenuHolder::getMenu);
    }

    public static Optional<Menu> getLastMenu(final @NotNull Player player) {
        return REGISTRY.getLastMenu(player.getUniqueId());
    }

    public static void cleanInventory(final @NotNull DeluxeMenus plugin, final @NotNull Player player) {
        if (!Bukkit.isPrimaryThread()) {
            MainThread.run(plugin, () -> {
                if (player.isOnline()) {
                    cleanInventory(plugin, player);
                }
            });
            return;
        }

        for (final ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null) continue;
            if (!plugin.getMenuItemMarker().isMarked(itemStack)) continue;

            plugin.debug(
                    DebugLevel.LOWEST,
                    Level.INFO,
                    "Found a DeluxeMenus item in a player's inventory. Removing it."
            );
            player.getInventory().remove(itemStack);
        }
        player.updateInventory();
    }

    public static void closeMenu(final @NotNull DeluxeMenus plugin, final @NotNull Player player, final boolean close, final boolean executeCloseActions) {
        if (!Bukkit.isPrimaryThread()) {
            final Optional<MenuHolder> expectedHolder = getMenuHolder(player);
            if (expectedHolder.isEmpty()) {
                return;
            }

            MainThread.run(plugin, () -> {
                if (isCurrentHolder(expectedHolder.get())) {
                    closeMenuNow(plugin, player, close, executeCloseActions);
                }
            });
            return;
        }

        closeMenuNow(plugin, player, close, executeCloseActions);
    }

    private static void closeMenuNow(final @NotNull DeluxeMenus plugin, final @NotNull Player player, final boolean close, final boolean executeCloseActions) {
        Optional<MenuHolder> optionalHolder = getMenuHolder(player);
        if (optionalHolder.isEmpty()) {
            return;
        }

        MenuHolder holder = optionalHolder.get();

        holder.stopPlaceholderUpdate();
        holder.stopRefreshTask();

        if (executeCloseActions) {
            holder.getMenu().map(Menu::options).map(MenuOptions::closeHandler).flatMap(h -> h).ifPresent(h -> h.onClick(holder));
        }

        REGISTRY.removeHolder(player.getUniqueId(), holder);
        holder.getMenu().ifPresent(menu -> REGISTRY.rememberLastMenu(player.getUniqueId(), menu));

        if (close && player.isOnline()) {
            player.closeInventory();
            cleanInventory(plugin, player);
        }
    }

    public static void closeMenuForShutdown(final @NotNull DeluxeMenus plugin, final @NotNull Player player) {
        if (!Bukkit.isPrimaryThread()) {
            MainThread.run(plugin, () -> closeMenuForShutdown(plugin, player));
            return;
        }

        getMenuHolder(player).ifPresent(holder -> {
            holder.stopPlaceholderUpdate();
            holder.stopRefreshTask();
            REGISTRY.removeHolder(player.getUniqueId(), holder);
        });

        player.closeInventory();
        cleanInventory(plugin, player);
    }

    public static void closeMenu(final @NotNull DeluxeMenus plugin, final @NotNull Player player, final boolean close) {
        closeMenu(plugin, player, close, false);
    }

    private boolean hasOpenBypassPerm(final @NotNull Player viewer) {
        return viewer.hasPermission("deluxemenus.openrequirement.bypass." + this.options.name())
                || viewer.hasPermission("deluxemenus.openrequirement.bypass.*");
    }

    private boolean handleOpenRequirements(final @NotNull MenuHolder holder) {
        if (this.options.openRequirements().isEmpty()) {
            return true;
        }

        final RequirementList openRequirements = this.options.openRequirements().get();
        if (openRequirements.getRequirements() == null) {
            return true;
        }

        if (holder.getViewer() != null && (this.options.enableBypassPerm() && this.hasOpenBypassPerm(holder.getViewer()))) {
            return true;
        }

        if (!openRequirements.evaluate(holder)) {
            if (openRequirements.getDenyHandler() != null) {
                openRequirements.getDenyHandler().onClick(holder);
            }
            return false;
        }
        return true;
    }

    private boolean handleArgRequirements(final @NotNull MenuHolder holder) {
        for (RequirementList rl : this.options.argumentRequirements()) {
            if (rl.getRequirements() == null) {
                continue;
            }

            if (!rl.evaluate(holder)) {
                if (rl.getDenyHandler() != null) {
                    rl.getDenyHandler().onClick(holder);
                }
                return false;
            }
        }

        return true;
    }

    public void openMenu(final @NotNull Player viewer) {
        openMenu(viewer, null, null);
    }

    public void openMenu(final @NotNull Player viewer, final @Nullable Map<String, String> args, final @Nullable Player placeholderPlayer) {
        if (!Bukkit.isPrimaryThread()) {
            final Map<String, String> copiedArgs = args == null ? null : new HashMap<>(args);
            MainThread.run(plugin, () -> openMenu(viewer, copiedArgs, placeholderPlayer));
            return;
        }

        openMenuNow(viewer, args, placeholderPlayer);
    }

    private void openMenuNow(final @NotNull Player viewer, final @Nullable Map<String, String> args, final @Nullable Player placeholderPlayer) {
        if (!viewer.isOnline() || !REGISTRY.isCurrentMenu(options.name(), this) || items == null || items.isEmpty()) {
            return;
        }

        final DeluxeMenusPreOpenMenuEvent preOpenEvent = new DeluxeMenusPreOpenMenuEvent(viewer);
        Bukkit.getPluginManager().callEvent(preOpenEvent);
        if (preOpenEvent.isCancelled()) {
            return;
        }

        final MenuHolder holder = new MenuHolder(plugin, viewer);
        if (placeholderPlayer != null) {
            holder.setPlaceholderPlayer(placeholderPlayer);
        }
        holder.setTypedArgs(args);
        holder.parsePlaceholdersInArguments(this.options.parsePlaceholdersInArguments());
        holder.parsePlaceholdersAfterArguments(this.options.parsePlaceholdersAfterArguments());

        if (!handleArgRequirements(holder) || !handleOpenRequirements(holder)) {
            return;
        }

        final Set<MenuItem> activeItems = new HashSet<>();
        for (Entry<Integer, TreeMap<Integer, MenuItem>> entry : items.entrySet()) {
            for (MenuItem item : entry.getValue().values()) {
                final int slot = item.options().slot();
                if (slot >= options.size()) {
                    plugin.debug(DebugLevel.HIGHEST, Level.WARNING,
                            "Item set to slot " + slot + " for menu: " + options.name() + " exceeds the inventory size!",
                            "This item will not be added to the menu!");
                    continue;
                }

                if (item.options().viewRequirements().isEmpty()
                        || item.options().viewRequirements().get().evaluate(holder)) {
                    activeItems.add(item);
                    break;
                }
            }
        }

        if (activeItems.isEmpty()) {
            return;
        }

        holder.setMenuName(options.name());
        holder.setActiveItems(activeItems);
        options.openHandler().ifPresent(handler -> handler.onClick(holder));

        final String title = StringUtils.color(holder.setPlaceholdersAndArguments(options.title()));
        final Inventory inventory = options.type() == InventoryType.CHEST
                ? Bukkit.createInventory(holder, options.size(), title)
                : Bukkit.createInventory(holder, options.type(), title);
        holder.setInventory(inventory);

        boolean updatePlaceholders = false;
        for (MenuItem item : activeItems) {
            final int slot = item.options().slot();
            if (slot >= options.size()) {
                continue;
            }

            ItemStack itemStack = item.getItemStack(holder);
            if (itemStack == null) {
                continue;
            }

            inventory.setItem(slot, plugin.getMenuItemMarker().mark(itemStack));
            updatePlaceholders |= item.options().updatePlaceholders();
        }

        if (isInMenu(viewer)) {
            closeMenuNow(plugin, viewer, false, false);
        }

        viewer.openInventory(inventory);
        if (!viewer.isOnline() || viewer.getOpenInventory().getTopInventory() != inventory) {
            return;
        }

        REGISTRY.setHolder(viewer.getUniqueId(), holder);
        if (options.refresh()) {
            holder.startRefreshTask();
        }
        if (updatePlaceholders) {
            holder.startUpdatePlaceholdersTask();
        }

        Bukkit.getPluginManager().callEvent(new DeluxeMenusOpenMenuEvent(viewer, holder));
    }

    public void refreshForAll() {
        if (!Bukkit.isPrimaryThread()) {
            MainThread.run(plugin, this::refreshForAll);
            return;
        }

        if (!REGISTRY.isCurrentMenu(options.name(), this)) {
            return;
        }

        final List<MenuHolder> holders = REGISTRY.holdersSnapshot().stream()
                .filter(menuHolder -> options.name().equalsIgnoreCase(menuHolder.getMenuName()))
                .toList();
        holders.forEach(MenuHolder::refreshMenu);
    }

    public @NotNull Map<Integer, TreeMap<Integer, MenuItem>> getMenuItems() {
        return this.items;
    }

    public @NotNull Optional<String> getMenuCommandUsed(final @NotNull String command) {
        return this.options.commands().stream().filter(c -> c.equalsIgnoreCase(command)).findFirst();
    }

    public @NotNull MenuOptions options() {
        return this.options;
    }

    public @NotNull String path() {
        return this.path;
    }

    public int activeViewers() {
        return (int) REGISTRY.holdersSnapshot().stream()
                .filter(holder -> options.name().equalsIgnoreCase(holder.getMenuName()))
                .count();
    }

}
