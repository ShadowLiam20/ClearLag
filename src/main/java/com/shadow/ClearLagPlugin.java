package com.shadow;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClearLagPlugin extends JavaPlugin {
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        scheduleCleanupCycle(getStartDelaySeconds());
        getLogger().info("ClearLag plugin is gestart.");
    }

    @Override
    public void onDisable() {
        cancelScheduledTasks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("clearlag")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            restartCleanupScheduler();
            sender.sendMessage(colorize(getConfig().getString(
                    "messages.reload-complete",
                    "%prefix%&aConfig opnieuw geladen."
            ).replace("%prefix%", getConfig().getString("messages.prefix", "&8[&aClearLag&8] "))));
            return true;
        }

        sender.sendMessage(colorize(getConfig().getString(
                "messages.reload-usage",
                "%prefix%&fGebruik: &e/clearlag reload"
        ).replace("%prefix%", getConfig().getString("messages.prefix", "&8[&aClearLag&8] "))));
        return true;
    }

    private void scheduleCleanupCycle(long delaySeconds) {
        long safeDelaySeconds = Math.max(1L, delaySeconds);

        scheduleWarnings(safeDelaySeconds);

        BukkitTask cleanupTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            int removedItems = clearGroundItems();
            broadcastMessage(
                    getConfig().getString(
                            "messages.cleanup-complete",
                            "%prefix%&f%amount% items van de grond verwijderd."
                    ).replace("%amount%", Integer.toString(removedItems))
            );

            scheduleCleanupCycle(getIntervalSeconds());
        }, safeDelaySeconds * 20L);

        scheduledTasks.add(cleanupTask);
    }

    private void scheduleWarnings(long cleanupDelaySeconds) {
        if (!getConfig().getBoolean("warnings.enabled", true)) {
            return;
        }

        List<Integer> warningTimes = new ArrayList<>(getConfig().getIntegerList("warnings.times-seconds"));
        warningTimes.sort(Comparator.reverseOrder());

        for (int warningSeconds : warningTimes) {
            if (warningSeconds <= 0 || warningSeconds >= cleanupDelaySeconds) {
                continue;
            }

            long delayUntilWarning = cleanupDelaySeconds - warningSeconds;
            BukkitTask warningTask = Bukkit.getScheduler().runTaskLater(this, () -> broadcastMessage(
                    getConfig().getString(
                            "messages.warning",
                            "%prefix%&fGrond items worden over &e%time% &fverwijderd."
                    )
                            .replace("%seconds%", Integer.toString(warningSeconds))
                            .replace("%time%", formatTime(warningSeconds))
            ), delayUntilWarning * 20L);

            scheduledTasks.add(warningTask);
        }
    }

    private long getStartDelaySeconds() {
        long intervalSeconds = getIntervalSeconds();
        return Math.max(1L, getConfig().getLong("cleanup.start-delay-seconds", intervalSeconds));
    }

    private long getIntervalSeconds() {
        return Math.max(1L, getConfig().getLong("cleanup.interval-seconds", 900L));
    }

    private int clearGroundItems() {
        int removed = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Item.class)) {
                entity.remove();
                removed++;
            }
        }

        return removed;
    }

    private void broadcastMessage(String rawMessage) {
        if (!getConfig().getBoolean("messages.broadcast", true)) {
            return;
        }

        String prefix = getConfig().getString("messages.prefix", "&8[&aClearLag&8] ");
        Bukkit.broadcastMessage(colorize(rawMessage.replace("%prefix%", prefix)));
    }

    private void cancelScheduledTasks() {
        for (BukkitTask task : scheduledTasks) {
            task.cancel();
        }

        scheduledTasks.clear();
    }

    private void restartCleanupScheduler() {
        cancelScheduledTasks();
        scheduleCleanupCycle(getStartDelaySeconds());
    }

    private String formatTime(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + " seconde" + (totalSeconds == 1 ? "" : "n");
        }

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (seconds == 0) {
            return minutes + " minuut" + (minutes == 1 ? "" : "en");
        }

        return minutes + "m " + seconds + "s";
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }
}
