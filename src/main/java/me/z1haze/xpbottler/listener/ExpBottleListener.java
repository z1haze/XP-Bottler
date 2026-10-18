package me.z1haze.xpbottler.listener;

import me.z1haze.xpbottler.XPBottler;
import me.z1haze.xpbottler.item.XPBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;

public class ExpBottleListener implements Listener {

    private final XPBottler plugin;

    public ExpBottleListener(XPBottler plugin) {
        this.plugin = plugin;
    }

    /**
     * Bottles made by this plugin give back exactly what they hold.
     * Vanilla bottles (trades, loot, creative) are left alone and stay random.
     */
    @EventHandler
    public void onExpBottle(ExpBottleEvent event) {
        int stored = XPBottle.getStoredXp(plugin, event.getEntity().getItem());
        if (stored < 0) return;

        event.setExperience(stored);
    }
}
