
package dev.dungeonmaster;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonMaster extends JavaPlugin {

    private boolean eventActive = false;

    @Override
    public void onEnable() {
        getLogger().info("Dungeon Master v0.1 has been enabled!");
        getLogger().info("Ready to manage dungeon events.");

        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("Dungeon Master has been disabled.");
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
            sendMessage(sender, "&6&lDungeon Master &7v0.1");
            sendMessage(sender, "&e/dungeon &7- Show this help menu");
            sendMessage(sender, "&e/dungeon status &7- Check event status");
            sendMessage(sender, "&e/dungeon start &7- Start a test event");
            sendMessage(sender, "&e/dungeon stop &7- Stop the current event");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("status")) {
            if (eventActive) {
                sendMessage(sender, "&aA dungeon event is currently active!");
            } else {
                sendMessage(sender, "&7No dungeon event is currently active.");
            }
            return true;
        }

        if (subcommand.equals("start")) {
            if (!sender.hasPermission("dungeonmaster.admin")) {
                sendMessage(sender, "&cYou don't have permission to do that.");
                return true;
            }

            if (eventActive) {
                sendMessage(sender, "&cA dungeon event is already active.");
                return true;
            }

            eventActive = true;

            getLogger().info("A dungeon test event was started by "
                    + sender.getName() + ".");

            sendMessage(sender, "&a&lDUNGEON EVENT STARTED!");
            sendMessage(sender, "&7This is a test event for V0.1.");
            return true;
        }

        if (subcommand.equals("stop")) {
            if (!sender.hasPermission("dungeonmaster.admin")) {
                sendMessage(sender, "&cYou don't have permission to do that.");
                return true;
            }

            if (!eventActive) {
                sendMessage(sender, "&cThere is no active dungeon event.");
                return true;
            }

            eventActive = false;

            getLogger().info("The dungeon test event was stopped by "
                    + sender.getName() + ".");

            sendMessage(sender, "&c&lDUNGEON EVENT STOPPED!");
            return true;
        }

        sendMessage(sender, "&cUnknown subcommand. Use /dungeon help.");
        return true;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        "&8[&cDungeonMaster&8] &r" + message)
        );
    }
}
