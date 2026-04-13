package dev.jones.doorlock.util;

import dev.jones.doorlock.Doorlock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WorldGuardSupport {
    public enum Mode {
        OFF,
        AUTO,
        REQUIRED
    }

    private static volatile Mode mode = Mode.AUTO;
    private static final AtomicBoolean broken = new AtomicBoolean(false);
    private static final AtomicBoolean loggedUnavailable = new AtomicBoolean(false);
    private static final AtomicBoolean loggedFailure = new AtomicBoolean(false);

    private WorldGuardSupport() {
    }

    public static void reload() {
        String configured = Doorlock.getInstance().getConfig().getString("worldguard.mode", "AUTO");
        mode = parseMode(configured);
        broken.set(false);
        loggedUnavailable.set(false);
        loggedFailure.set(false);
    }

    public static Mode getMode() {
        return mode;
    }

    public static boolean isEnabled() {
        return mode != Mode.OFF;
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public static boolean isBroken() {
        return broken.get();
    }

    public static boolean shouldUseChecks() {
        return isEnabled() && isAvailable() && !isBroken();
    }

    public static boolean shouldFailClosed() {
        return mode == Mode.REQUIRED && (!isAvailable() || isBroken());
    }

    public static boolean shouldSkipChecks() {
        return !isEnabled() || (!shouldFailClosed() && (!isAvailable() || isBroken()));
    }

    public static void logStartupState() {
        if (mode == Mode.OFF) {
            Doorlock.getInstance().getLogger().info("WorldGuard integration is disabled in config.");
            return;
        }

        if (!isAvailable()) {
            if (mode == Mode.REQUIRED) {
                Doorlock.getInstance().getLogger().severe("WorldGuard is required by config, but the plugin is missing. Protection-related actions will be denied.");
            } else if (loggedUnavailable.compareAndSet(false, true)) {
                Doorlock.getInstance().getLogger().warning("WorldGuard is not installed. WorldGuard-backed checks will be skipped because config mode is AUTO.");
            }
            return;
        }

        Doorlock.getInstance().getLogger().info("WorldGuard integration is enabled in " + mode.name().toLowerCase(Locale.ROOT) + " mode.");
    }

    public static boolean denyIfStrictUnavailable(Player player, String action) {
        if (!shouldFailClosed()) {
            return false;
        }

        String reason = isAvailable()
                ? "WorldGuard integration failed during " + action + "."
                : "WorldGuard is required for " + action + ", but the plugin is missing.";
        if (loggedFailure.compareAndSet(false, true)) {
            logFailure(reason, null);
        }
        if (player != null) {
            player.sendMessage(Messages.get("worldguard.strict_blocked"));
        }
        return true;
    }

    public static boolean handleFailure(Player player, String action, Throwable throwable) {
        broken.set(true);
        String reason = "WorldGuard error during " + action + ".";

        if (mode == Mode.REQUIRED) {
            logFailure(reason, throwable);
            if (player != null) {
                player.sendMessage(Messages.get("worldguard.strict_blocked"));
            }
            return true;
        }

        if (loggedFailure.compareAndSet(false, true)) {
            logFailure(reason + " Falling back to non-WorldGuard behavior because config mode is AUTO.", throwable);
        }
        return false;
    }

    private static void logFailure(String message, Throwable throwable) {
        Doorlock.getInstance().getLogger().severe(message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    private static Mode parseMode(String value) {
        if (value == null) {
            return Mode.AUTO;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("FALSE") || normalized.equals("NO") || normalized.equals("0")) {
            return Mode.OFF;
        }
        if (normalized.equals("TRUE") || normalized.equals("YES") || normalized.equals("1")) {
            return Mode.REQUIRED;
        }
        try {
            return Mode.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Mode.AUTO;
        }
    }
}
