package me.z1haze.xpbottler.command;

import me.z1haze.commons.event.ConfigChangeEvent;
import me.z1haze.commons.util.ChatUtils;
import me.z1haze.xpbottler.XPBottler;
import me.z1haze.xpbottler.item.XPBottle;
import me.z1haze.xpbottler.util.XPMath;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
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
        int total = XPMath.totalPoints(player.getLevel(), player.getExp(), player.getExpToLevel());
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
        setPlayerXp(player, total - bottledXp);

        int dropped = giveBottles(player, xpPerBottle, bottles);
        if (dropped > 0) {
            sendColorized(player, withBottles(plugin.getMessage("dropped"), dropped));
        }

        playSound(player);
        sendColorized(player, withBottles(plugin.getMessage("bottled"), bottles)
                .replace("{xp}", String.valueOf(bottledXp)));
    }

    // /xpb give <player> [amount] [xp]
    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xpbottler.give")) {
            sendColorized(sender, plugin.getMessage("no-permission"));
            return;
        }

        if (args.length < 2 || args.length > 4) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sendColorized(sender, plugin.getMessage("player-not-found").replace("{player}", args[1]));
            return;
        }

        int amount = args.length > 2 ? parsePositiveInt(args[2]) : 1;
        int xp = args.length > 3 ? parsePositiveInt(args[3]) : plugin.getXpPerBottle();

        if (amount <= 0 || xp <= 0) {
            sendColorized(sender, plugin.getMessage("usage"));
            return;
        }

        giveBottles(target, xp, amount);

        sendColorized(sender, withBottles(plugin.getMessage("gave"), amount)
                .replace("{xp}", String.valueOf(xp))
                .replace("{player}", target.getName()));
        sendColorized(target, withBottles(plugin.getMessage("received"), amount));
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
     * Replaces the player's XP with an exact number of points.
     * setTotalExperience is reset too so the score stays in step with what the player sees.
     */
    private void setPlayerXp(Player player, int points) {
        player.setLevel(0);
        player.setExp(0f);
        player.setTotalExperience(0);

        if (points > 0) {
            player.giveExp(points);
        }
    }

    /**
     * Adds bottles to the player's inventory, dropping whatever doesn't fit at their feet.
     * Returns how many bottles were dropped.
     */
    private int giveBottles(Player player, int xp, int amount) {
        ItemStack bottles = new XPBottle(plugin, xp, amount).getItem();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(bottles);

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

    @SuppressWarnings("deprecation") // Sound.valueOf still resolves both BUKKIT_NAMES and vanilla keys
    private void playSound(Player player) {
        ConfigurationSection sound = plugin.getConfig().getConfigurationSection("sound");
        if (sound == null || !sound.getBoolean("enabled", true)) return;

        String name = sound.getString("name", "BLOCK_BREWING_STAND_BREW");

        Sound resolved;
        try {
            resolved = Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            ChatUtils.logColorized("&cUnknown sound '" + name + "' in config.yml");
            return;
        }

        player.playSound(player.getLocation(), resolved,
                (float) sound.getDouble("volume", 1.0),
                (float) sound.getDouble("pitch", 1.0));
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

    private List<String> startingWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
