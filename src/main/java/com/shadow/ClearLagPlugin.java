package com.shadow;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        if (args.length == 1 && args[0].equalsIgnoreCase("run")) {
            if (!sender.hasPermission("clearlag.run")) {
                sender.sendMessage(colorize(getConfig().getString(
                        "messages.no-permission",
                        "%prefix%&cJe hebt geen permissie voor dit commando."
                ).replace("%prefix%", getConfig().getString("messages.prefix", "&8[&aClearLag&8] "))));
                return true;
            }

            CleanupResult cleanupResult = runCleanup();
            sender.sendMessage(colorize(formatCleanupMessage(
                    getConfig().getString(
                            "messages.manual-cleanup-complete",
                            "%prefix%&fCleanup uitgevoerd. Verwijderd: &b%amount%&f."
                    ),
                    cleanupResult
            )));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("clearlag.reload")) {
                sender.sendMessage(colorize(getConfig().getString(
                        "messages.no-permission",
                        "%prefix%&cJe hebt geen permissie voor dit commando."
                ).replace("%prefix%", getConfig().getString("messages.prefix", "&8[&aClearLag&8] "))));
                return true;
            }

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
                "%prefix%&fGebruik: &e/clearlag reload &7of &e/clearlag run"
        ).replace("%prefix%", getConfig().getString("messages.prefix", "&8[&aClearLag&8] "))));
        return true;
    }

    private void scheduleCleanupCycle(long delaySeconds) {
        long safeDelaySeconds = Math.max(1L, delaySeconds);

        scheduleWarnings(safeDelaySeconds);

        BukkitTask cleanupTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            CleanupResult cleanupResult = runCleanup();
            broadcastMessage(formatCleanupMessage(
                    getConfig().getString(
                            "messages.cleanup-complete",
                            "%prefix%&fCleanup voltooid. Verwijderd: &b%amount%&f."
                    ),
                    cleanupResult
            ));

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

    private CleanupResult runCleanup() {
        CleanupResult cleanupResult = new CleanupResult();
        Set<String> ignoredWorlds = getIgnoredWorlds();

        for (World world : Bukkit.getWorlds()) {
            if (ignoredWorlds.contains(world.getName().toLowerCase(Locale.ROOT))) {
                continue;
            }

            if (getConfig().getBoolean("cleanup.remove-items", true)) {
                cleanupResult.add("items", removeEntities(world.getEntitiesByClass(Item.class)));
            }

            if (getConfig().getBoolean("cleanup.remove-xp-orbs", true)) {
                cleanupResult.add("xp", removeEntities(world.getEntitiesByClass(ExperienceOrb.class)));
            }

            if (getConfig().getBoolean("cleanup.remove-arrows", true)) {
                cleanupResult.add("arrows", removeEntities(world.getEntitiesByClass(AbstractArrow.class)));
            }

            if (getConfig().getBoolean("cleanup.remove-other-projectiles", false)) {
                cleanupResult.add("projectiles", removeProjectiles(world));
            }

            if (getConfig().getBoolean("cleanup.remove-minecarts", false)) {
                cleanupResult.add("minecarts", removeEntities(world.getEntitiesByClass(Minecart.class)));
            }

            if (getConfig().getBoolean("cleanup.remove-boats", false)) {
                cleanupResult.add("boats", removeEntities(world.getEntitiesByClass(Boat.class)));
            }

            if (getConfig().getBoolean("cleanup.mobs.enabled", false)) {
                cleanupResult.add("mobs", removeFarMobs(world));
            }
        }

        return cleanupResult;
    }

    private int removeFarMobs(World world) {
        int removed = 0;
        double maxDistance = Math.max(1D, getConfig().getDouble("cleanup.mobs.max-distance-from-player", 96D));
        double maxDistanceSquared = maxDistance * maxDistance;
        boolean removeHostile = getConfig().getBoolean("cleanup.mobs.remove-hostile-mobs", true);
        boolean removePassive = getConfig().getBoolean("cleanup.mobs.remove-passive-mobs", false);

        for (Entity worldEntity : world.getEntities()) {
            if (!(worldEntity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity entity = (LivingEntity) worldEntity;
            if (!shouldRemoveMob(entity, removeHostile, removePassive)) {
                continue;
            }

            if (hasNearbyPlayer(entity, maxDistanceSquared)) {
                continue;
            }

            entity.remove();
            removed++;
        }

        return removed;
    }

    private int removeProjectiles(World world) {
        int removed = 0;

        for (Entity entity : world.getEntitiesByClass(Projectile.class)) {
            if (entity instanceof AbstractArrow) {
                continue;
            }

            if (isIgnoredEntityType(entity)) {
                continue;
            }

            entity.remove();
            removed++;
        }

        return removed;
    }

    private int removeEntities(List<? extends Entity> entities) {
        int removed = 0;

        for (Entity entity : entities) {
            if (isIgnoredEntityType(entity)) {
                continue;
            }

            entity.remove();
            removed++;
        }

        return removed;
    }

    private boolean shouldRemoveMob(LivingEntity entity, boolean removeHostile, boolean removePassive) {
        if (isIgnoredEntityType(entity)) {
            return false;
        }

        if (entity instanceof Player) {
            return false;
        }

        if (entity.getCustomName() != null || isLeashed(entity) || !entity.getPassengers().isEmpty() || entity.isInsideVehicle()) {
            return false;
        }

        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed()) {
                return false;
            }
        }

        if (entity instanceof Monster) {
            return removeHostile;
        }

        if (entity instanceof Animals) {
            return removePassive;
        }

        return false;
    }

    private boolean isLeashed(LivingEntity entity) {
        if (!(entity instanceof Mob)) {
            return false;
        }

        return ((Mob) entity).isLeashed();
    }

    private boolean hasNearbyPlayer(Entity entity, double maxDistanceSquared) {
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= maxDistanceSquared) {
                return true;
            }
        }

        return false;
    }

    private Set<String> getIgnoredWorlds() {
        Set<String> ignoredWorlds = new HashSet<>();

        for (String worldName : getConfig().getStringList("cleanup.ignored-worlds")) {
            ignoredWorlds.add(worldName.toLowerCase(Locale.ROOT));
        }

        return ignoredWorlds;
    }

    private boolean isIgnoredEntityType(Entity entity) {
        return getIgnoredEntityTypes().contains(entity.getType());
    }

    private Set<EntityType> getIgnoredEntityTypes() {
        Set<EntityType> ignoredEntityTypes = new HashSet<>();
        List<String> ignoredTypeNames = getConfig().getStringList("cleanup.ignored-entity-types");

        for (String ignoredTypeName : ignoredTypeNames) {
            try {
                ignoredEntityTypes.add(EntityType.valueOf(ignoredTypeName.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Onbekend entity type in cleanup.ignored-entity-types: " + ignoredTypeName);
            }
        }

        return ignoredEntityTypes;
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

    private String formatCleanupMessage(String template, CleanupResult cleanupResult) {
        String prefix = getConfig().getString("messages.prefix", "&8[&aClearLag&8] ");

        return template
                .replace("%prefix%", prefix)
                .replace("%amount%", Integer.toString(cleanupResult.getTotalRemoved()))
                .replace("%details%", cleanupResult.getDetailsSummary());
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

    private static final class CleanupResult {
        private final Map<String, Integer> removedByType = new LinkedHashMap<>();

        private void add(String type, int amount) {
            if (amount <= 0) {
                return;
            }

            removedByType.merge(type, amount, Integer::sum);
        }

        private int getTotalRemoved() {
            int total = 0;

            for (int amount : removedByType.values()) {
                total += amount;
            }

            return total;
        }

        private String getDetailsSummary() {
            if (removedByType.isEmpty()) {
                return "niets";
            }

            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : removedByType.entrySet()) {
                parts.add(entry.getValue() + " " + entry.getKey());
            }

            return String.join(", ", parts);
        }
    }
}
