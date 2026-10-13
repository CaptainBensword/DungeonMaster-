package dev.dungeonmaster;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class DungeonMaster extends JavaPlugin {

    private boolean joinWindowOpen = false;
    private boolean runActive = false;

    private final Set<UUID> participants = new LinkedHashSet<>();

    private BukkitTask joinTask;
    private BukkitTask eventTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("DungeonMaster V0.2 enabled!");
        getLogger().info("Automatic dungeon events are being scheduled.");

        scheduleNextEvent();
    }

    @Override
    public void onDisable() {
        if (joinTask != null) {
            joinTask.cancel();
        }

        if (eventTask != null) {
            eventTask.cancel();
        }

        getLogger().info("DungeonMaster has been disabled.");
    }

    private void scheduleNextEvent() {
        int minMinutes = Math.max(1,
                getConfig().getInt("events.minimum-interval-minutes", 30));

        int maxMinutes = Math.max(minMinutes,
                getConfig().getInt("events.maximum-interval-minutes", 60));

        int delayMinutes = ThreadLocalRandom.current()
                .nextInt(minMinutes, maxMinutes + 1);

        long delayTicks = delayMinutes * 60L * 20L;

        eventTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                getLogger().info(
                        "No players online. Skipping this dungeon event."
                );
                scheduleNextEvent();
                return;
            }

            openEvent(null);
        }, delayTicks);

        getLogger().info(
                "Next dungeon event scheduled in " + delayMinutes + " minutes."
        );
    }

    private void openEvent(CommandSender starter) {
        if (joinWindowOpen || runActive) {
            if (starter != null) {
                sendMessage(starter, "&cA dungeon event or run is already active.");
            }
            return;
        }

        participants.clear();
        joinWindowOpen = true;

        int joinMinutes = Math.max(1,
                getConfig().getInt("events.join-window-minutes", 10));

        Bukkit.broadcastMessage(color(
                "&8[&cDungeonMaster&8] &6&lA DUNGEON HAS OPENED!"
        ));
        Bukkit.broadcastMessage(color(
                "&eUse &f/dungeon join &eto join the adventure!"
        ));
        Bukkit.broadcastMessage(color(
                "&7You have &e" + joinMinutes
                        + " minutes &7to join."
        ));

        if (starter != null) {
            getLogger().info(
                    "A dungeon event was manually opened by "
                            + starter.getName() + "."
            );
        } else {
            getLogger().info("A scheduled dungeon event has opened.");
        }

        joinTask = Bukkit.getScheduler().runTaskLater(
                this,
                this::beginRun,
                joinMinutes * 60L * 20L
        );
    }

    private void beginRun() {
        joinWindowOpen = false;

        if (participants.isEmpty()) {
            Bukkit.broadcastMessage(color(
                    "&8[&cDungeonMaster&8] &7The dungeon closed because nobody joined."
            ));

            finishRun();
            return;
        }

        String worldName = getConfig().getString("dungeon.world", "dungeon");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            getLogger().severe(
                    "Dungeon world '" + worldName
                            + "' is not loaded. The run cannot begin."
            );

            for (UUID id : participants) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    sendMessage(player,
                            "&cThe dungeon world is unavailable. Please contact an admin.");
                }
            }

            finishRun();
            return;
        }

        double x = getConfig().getDouble("dungeon.spawn.x", 0.5);
        double y = getConfig().getDouble("dungeon.spawn.y", 65.0);
        double z = getConfig().getDouble("dungeon.spawn.z", 0.5);
        float yaw = (float) getConfig().getDouble("dungeon.spawn.yaw", 0.0);
        float pitch = (float) getConfig().getDouble("dungeon.spawn.pitch", 0.0);

        Location start = new Location(world, x, y, z, yaw, pitch);
        int teleported = 0;

        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);

            if (player != null && player.isOnline()) {
                if (player.teleport(start)) {
                    sendMessage(player, "&aWelcome to the dungeon!");
                    teleported++;
                } else {
                    sendMessage(player, "&cTeleport failed. Please contact an admin.");
                }
            }
        }

        if (teleported == 0) {
            Bukkit.broadcastMessage(color(
                    "&8[&cDungeonMaster&8] &7The dungeon run was cancelled."
            ));
            finishRun();
            return;
        }

        runActive = true;

        Bukkit.broadcastMessage(color(
                "&8[&cDungeonMaster&8] &c&lTHE DUNGEON RUN HAS BEGUN!"
        ));

        getLogger().info(
                "Dungeon run started with " + teleported + " participant(s)."
        );

        // Enemy waves and completion logic will be added in a later update.
    }

    private void finishRun() {
        joinWindowOpen = false;
        runActive = false;
        participants.clear();

        if (joinTask != null) {
            joinTask.cancel();
            joinTask = null;
        }

        scheduleNextEvent();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("dungeon")) {
            return false;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendMessage(sender, "&6&lDungeonMaster &7V0.2");
            sendMessage(sender, "&e/dungeon status &7- Check event status");
            sendMessage(sender, "&e/dungeon join &7- Join an open dungeon");
            sendMessage(sender, "&e/dungeon start &7- Open an event (admin)");
            sendMessage(sender, "&e/dungeon stop &7- Stop the event (admin)");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("status")) {
            if (joinWindowOpen) {
                sendMessage(sender, "&eA dungeon is open for joining!");
                sendMessage(sender, "&7Participants: &f" + participants.size());
            } else if (runActive) {
                sendMessage(sender, "&cA dungeon run is in progress!");
                sendMessage(sender, "&7Participants: &f" + participants.size());
            } else {
                sendMessage(sender, "&7No dungeon event is currently active.");
            }
            return true;
        }

        if (subcommand.equals("join")) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cOnly players can join a dungeon.");
                return true;
            }

            Player player = (Player) sender;

            if (!joinWindowOpen) {
                sendMessage(player, "&cThere is no dungeon currently open for joining.");
                return true;
            }

            if (!participants.add(player.getUniqueId())) {
                sendMessage(player, "&eYou've already joined this dungeon event.");
                return true;
            }

            sendMessage(player, "&aYou've joined the dungeon!");
            sendMessage(player, "&7You'll be teleported when the joining period ends.");
            return true;
        }

        if (subcommand.equals("start")) {
            if (!sender.hasPermission("dungeonmaster.admin")) {
                sendMessage(sender, "&cYou don't have permission to do that.");
                return true;
            }

            openEvent(sender);
            return true;
        }

        if (subcommand.equals("stop")) {
            if (!sender.hasPermission("dungeonmaster.admin")) {
                sendMessage(sender, "&cYou don't have permission to do that.");
                return true;
            }

            if (!joinWindowOpen && !runActive) {
                sendMessage(sender, "&cThere is no dungeon event or run to stop.");
                return true;
            }

            Bukkit.broadcastMessage(color(
                    "&8[&cDungeonMaster&8] &cThe dungeon event has been stopped."
            ));

            finishRun();
            sendMessage(sender, "&aDungeon event stopped.");
            return true;
        }

        sendMessage(sender, "&cUnknown subcommand. Use /dungeon help.");
        return true;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(color("&8[&cDungeonMaster&8] &r" + message));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
