package com.extendedclip.deluxemenus.updatechecker;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.listener.Listener;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.extendedclip.deluxemenus.utils.MainThread;
import com.extendedclip.deluxemenus.utils.Messages;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class UpdateChecker extends Listener {
    private static final TextReplacementConfig.Builder LATEST_VERSION_REPLACER_BUILDER = TextReplacementConfig.builder().matchLiteral("<latest-version>");
    private static final TextReplacementConfig.Builder CURRENT_VERSION_REPLACER_BUILDER = TextReplacementConfig.builder().matchLiteral("<current-version>");
    private static final int RESOURCE_ID = 11734;

    private final MainThread mainThread;
    private final AtomicBoolean registered = new AtomicBoolean();
    private final AtomicBoolean resultPublished = new AtomicBoolean();
    private volatile String latestVersion;
    private volatile boolean updateAvailable;

    public UpdateChecker(final @NotNull DeluxeMenus plugin) {
        this(plugin, new MainThread(plugin));
    }

    UpdateChecker(final @NotNull DeluxeMenus plugin, final @NotNull MainThread mainThread) {
        super(plugin);
        this.mainThread = mainThread;
        this.latestVersion = plugin.getDescription().getVersion();
    }

    @Override
    public void register() {
        mainThread.run(() -> {
            if (registered.compareAndSet(false, true)) {
                super.register();
            }
        });
    }

    public void start() {
        final String currentVersion = plugin.getDescription().getVersion();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final CheckResult result = check(currentVersion);
            mainThread.run(() -> publishResult(result));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (!player.isOp() || !updateAvailable) {
            return;
        }
        plugin.sms(player, Messages.UPDATE_AVAILABLE.message()
                .replaceText(CURRENT_VERSION_REPLACER_BUILDER.replacement(plugin.getDescription().getVersion()).build())
                .replaceText(LATEST_VERSION_REPLACER_BUILDER.replacement(getLatestVersion()).build()));
    }

    private CheckResult check(final String currentVersion) {
        final String version = getSpigotVersion();
        if (version == null) {
            return CheckResult.failed(currentVersion);
        }
        return new CheckResult(version, checkHigher(currentVersion, version), false);
    }

    private String getSpigotVersion() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.readLine();
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    void publishResult(final @NotNull CheckResult result) {
        if (!resultPublished.compareAndSet(false, true)) return;
        latestVersion = result.version();
        updateAvailable = result.updateAvailable();
        if (result.failed()) {
            plugin.debug(DebugLevel.HIGH, Level.INFO, "Failed to check for update on spigot!");
        } else if (result.updateAvailable()) {
            plugin.debug(DebugLevel.HIGHEST, Level.INFO,
                    "An update for DeluxeMenus (DeluxeMenus v" + result.version() + ")",
                    "is available at https://www.spigotmc.org/resources/deluxemenus.11734/");
        } else {
            plugin.debug(DebugLevel.HIGHEST, Level.INFO, "You are running the latest version of DeluxeMenus!");
        }
    }

    public boolean updateAvailable() { return updateAvailable; }
    public String getLatestVersion() { return latestVersion; }

    private boolean checkHigher(final @NotNull String currentVersion, final @NotNull String newVersion) {
        return toReadable(currentVersion).compareTo(toReadable(newVersion)) < 0;
    }

    private String toReadable(final @NotNull String version) {
        final String[] split = Pattern.compile(".", Pattern.LITERAL).split(version.replace("v", ""));
        final StringBuilder versionBuilder = new StringBuilder();
        for (String part : split) versionBuilder.append(String.format("%4s", part));
        return versionBuilder.toString();
    }

    record CheckResult(String version, boolean updateAvailable, boolean failed) {
        static CheckResult failed(final String currentVersion) { return new CheckResult(currentVersion, false, true); }
    }
}
