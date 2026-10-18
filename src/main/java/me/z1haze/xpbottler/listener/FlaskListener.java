package me.z1haze.xpbottler.listener;

import me.z1haze.xpbottler.XPBottler;
import me.z1haze.xpbottler.item.Flask;
import me.z1haze.xpbottler.util.PlayerXp;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.z1haze.commons.util.ChatUtils.sendColorized;

public class FlaskListener implements Listener {

    // A cancelled right-click on a block makes the client send a second right-click on air straight after.
    // Holding right-click repeats every 4 ticks, so anything closer than this is that echo, not a new Fill.
    private static final long FILL_ECHO_TICKS = 3;

    private final XPBottler plugin;
    private final Map<UUID, Long> lastFill = new HashMap<>();

    public FlaskListener(XPBottler plugin) {
        this.plugin = plugin;
    }

    /**
     * Shift + right-click with a Flask in the main hand Fills it, and nothing else happens:
     * no drinking, no chest opening, no block use.
     * A plain right-click with an empty Flask is stopped before the drink animation starts.
     */
    // not ignoreCancelled: Bukkit fires right-clicks on air already marked as cancelled
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Flask flask = Flask.from(plugin, event.getItem());
        if (flask == null) return;

        Player player = event.getPlayer();

        if (player.isSneaking() && event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);

            long now = player.getWorld().getGameTime();
            Long last = lastFill.put(player.getUniqueId(), now);
            if (last == null || now - last >= FILL_ECHO_TICKS) {
                fill(player, flask);
            }
            return;
        }

        if (flask.isEmpty()) {
            event.setUseItemInHand(Event.Result.DENY);

            // don't nag players who are just opening a chest with an empty Flask in hand
            Block block = event.getClickedBlock();
            if (block == null || !block.getType().isInteractable()) {
                sendColorized(player, plugin.getMessage("flask-empty"));
            }
        }
    }

    /**
     * Finishing a drink is a Sip. The drink is allowed to finish, with the drained Flask as its use remainder,
     * so it takes the place of the glass bottle a potion normally leaves behind. Cancelling instead would leave
     * the player stuck mid-drink while they hold right-click.
     */
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Flask flask = Flask.from(plugin, event.getItem());
        if (flask == null) return;

        Player player = event.getPlayer();
        if (flask.isEmpty()) {
            event.setCancelled(true);
            sendColorized(player, plugin.getMessage("flask-empty"));
            return;
        }

        int amount = Math.min(plugin.getXpPerSip(), flask.getXp());
        flask.setXp(flask.getXp() - amount);

        ItemStack drinking = event.getItem();
        ItemMeta meta = drinking.getItemMeta();
        if (meta == null) return;

        meta.setUseRemainder(flask.getItem());
        drinking.setItemMeta(meta);
        event.setItem(drinking);

        // straight onto the bar, so unlike a thrown Bottle it never repairs Mending gear
        player.giveExp(amount);

        plugin.playSound(player, "flask.sip-sound");
        plugin.sendActionBar(player, flask.withPlaceholders(plugin.getMessage("flask-status")));
    }

    /**
     * Mundane potions can be brewed, so refuse any brew with a Flask in the stand rather than ruin it.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        BrewerInventory stand = event.getContents();

        for (int slot = 0; slot < 3; slot++) {
            if (Flask.from(plugin, stand.getItem(slot)) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(plugin.getRecipeKeys());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastFill.remove(event.getPlayer().getUniqueId());
    }

    private void fill(Player player, Flask flask) {
        int total = PlayerXp.getTotal(player);
        if (total == 0) {
            sendColorized(player, plugin.getMessage("flask-no-xp"));
            return;
        }

        int room = flask.getRoom();
        if (room == 0) {
            sendColorized(player, plugin.getMessage("flask-full"));
            return;
        }

        int amount = Math.min(plugin.getXpPerSip(), Math.min(total, room));
        PlayerXp.set(player, total - amount);

        // Identical Flasks can end up stacked (e.g. by potion stacking plugins). Only one of them gets the XP,
        // otherwise a single Fill would be copied onto every Flask in the stack.
        ItemStack rest = player.getInventory().getItemInMainHand().clone();
        rest.setAmount(rest.getAmount() - 1);

        flask.setXp(flask.getXp() + amount);
        ItemStack filled = flask.getItem();
        filled.setAmount(1);
        player.getInventory().setItemInMainHand(filled);

        if (rest.getAmount() > 0) {
            player.getInventory().addItem(rest).values()
                    .forEach(overflow -> player.getWorld().dropItem(player.getLocation(), overflow));
        }

        plugin.playSound(player, "flask.fill-sound");
        plugin.sendActionBar(player, flask.withPlaceholders(plugin.getMessage("flask-status")));

        if (flask.getRoom() == 0) {
            sendColorized(player, plugin.getMessage("flask-now-full"));
        }
    }
}
