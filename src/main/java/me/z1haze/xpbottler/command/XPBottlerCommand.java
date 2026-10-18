package me.z1haze.xpbottler.command;

import me.z1haze.commons.event.ConfigChangeEvent;
import me.z1haze.commons.util.ChatUtils;
import me.z1haze.xpbottler.XPBottler;
import me.z1haze.xpbottler.item.Flask;
import me.z1haze.xpbottler.item.XPBottle;
import me.z1haze.xpbottler.util.PlayerXp;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static me.z1haze.commons.util.ChatUtils.sendColorized;

public class XPBottlerCommand implements TabExecutor {

    private final XPBottler plugin;

    public XPBottlerCommand(XPBottler plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            bottle(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "reload" -> reload(sender);
            default -> sendColorized(sender, plugin.getMessage("usage"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("xpbottler.give")) subcommands.add("give");
            if (sender.hasPermission("xpbottler.reload")) subcommands.add("reload");

            return startingWith(subcommands, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("xpbottler.give")) {
            List<String> names = plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            return startingWith(names, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("xpbottler.give")) {
            return startingWith(List.of("flask"), args[2]);
        }

        return List.of();
    }

    // /xpb
    private void bottle(CommandSender sender) {
        if (!sender.hasPermission("xpbottler.use")) {
            sendColorized(sender, plugin.getMessage("no-permission"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sendColorized(sender, plugin.getMessage("player-only"));
            return;
        }

        int xpPerBottle = plugin.getXpPerBottle();
        int total = PlayerXp.getTotal(player);
        int bottles = total / xpPerBottle;

        if (bottles == 0) {
            sendColorized(player, plugin.getMessage("not-enough-xp").replace("{xp}", String.valueOf(xpPerBottle)));
            return;
        }

        if (plugin.getConfig().getBoolean("glass-bottle-cost", false)) {
            int glass = countGlassBottles(player.getInventory());

            if (glass == 0) {
                sendColorized(player, plugin.getMessage("no-glass"));
                return;
            }

            bottles = Math.min(bottles, glass);
            player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, bottles));
        }

        int bottledXp = bottles * xpPerBottle;
        PlayerXp.set(player, total - bottledXp);

        int dropped = giveItems(player, new XPBottle(plugin, xpPerBottle, bottles).getItem());
        if (dropped > 0) {
            sendColorized(player, withBottles(plugin.getMessage("dropped"), dropped));
        }

        plugin.playSound(player, "sound");
        sendColorized(player, withBottles(plugin.getMessage("bottled"), bottles)
                .replace("{xp}", String.valueOf(bottledXp)));
    }

    // /xpb give <player> [amount] [xp]
    // /xpb give <player> flask [xp] [capacity]
    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xpbottler.give")) {
            sendColorized(sender, plugin.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sendColorized(sender, plugin.getMessage("player-not-found").replace("{player}", args[1]));
            return;
        }

        if (args.length > 2 && args[2].equalsIgnoreCase("flask")) {
            giveFlask(sender, target, args);
            return;
        }

        if (args.length > 4) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        int amount = args.length > 2 ? parsePositiveInt(args[2]) : 1;
        int xp = args.length > 3 ? parsePositiveInt(args[3]) : plugin.getXpPerBottle();

        if (amount <= 0 || xp <= 0) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        giveItems(target, new XPBottle(plugin, xp, amount).getItem());

        sendColorized(sender, withBottles(plugin.getMessage("gave"), amount)
                .replace("{xp}", String.valueOf(xp))
                .replace("{player}", target.getName()));
        sendColorized(target, withBottles(plugin.getMessage("received"), amount));
    }

    private void giveFlask(CommandSender sender, Player target, String[] args) {
        if (args.length > 5) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        int xp = args.length > 3 ? parsePositiveInt(args[3]) : 0;
        int capacity = args.length > 4 ? parseCapacity(args[4]) : plugin.getFlaskCapacity();

        if (xp < 0 || capacity == 0) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        if (capacity != Flask.UNLIMITED) {
            xp = Math.min(xp, capacity);
        }

        Flask flask = new Flask(plugin, xp, capacity);
        giveItems(target, flask.getItem());

        sendColorized(sender, flask.withPlaceholders(plugin.getMessage("gave-flask"))
                .replace("{player}", target.getName()));
        sendColorized(target, flask.withPlaceholders(plugin.getMessage("received-flask")));
    }

    // /xpb reload
    private void reload(CommandSender sender) {
        if (!sender.hasPermission("xpbottler.reload")) {
            sendColorized(sender, plugin.getMessage("no-permission"));
            return;
        }

        plugin.reloadConfig();
        plugin.getServer().getPluginManager().callEvent(new ConfigChangeEvent(plugin));

        sendColorized(sender, plugin.getMessage("reloaded"));
    }

    /**
     * Adds items to the player's inventory, dropping whatever doesn't fit at their feet.
     * Returns how many items were dropped.
     */
    private int giveItems(Player player, ItemStack items) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(items);

        int dropped = 0;

        for (ItemStack rest : leftover.values()) {
            int remaining = rest.getAmount();
            dropped += remaining;

            // leftovers come back as a single oversized stack, so drop them in normal stack sizes
            while (remaining > 0) {
                ItemStack part = rest.clone();
                part.setAmount(Math.min(remaining, rest.getMaxStackSize()));
                player.getWorld().dropItem(player.getLocation(), part);
                remaining -= part.getAmount();
            }
        }

        return dropped;
    }

    private int countGlassBottles(Inventory inventory) {
        ItemStack plainGlassBottle = new ItemStack(Material.GLASS_BOTTLE);
        int count = 0;

        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.isSimilar(plainGlassBottle)) {
                count += item.getAmount();
            }
        }

        return count;
    }

    /**
     * Fills {bottles} with the count and {s} with the plural suffix that goes with it.
     */
    private String withBottles(String message, int bottles) {
        return message
                .replace("{bottles}", String.valueOf(bottles))
                .replace("{s}", bottles == 1 ? "" : "s");
    }

    private int parsePositiveInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * A Capacity argument: a positive number, or -1 for no limit. Returns 0 when it's neither.
     */
    private int parseCapacity(String text) {
        if (text.equals(String.valueOf(Flask.UNLIMITED))) return Flask.UNLIMITED;
        return Math.max(0, parsePositiveInt(text));
    }

    private List<String> startingWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
