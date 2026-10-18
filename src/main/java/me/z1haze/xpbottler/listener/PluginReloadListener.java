package me.z1haze.xpbottler.listener;

import me.z1haze.commons.event.ConfigChangeEvent;
import me.z1haze.xpbottler.XPBottler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PluginReloadListener implements Listener {

    private final XPBottler plugin;

    public PluginReloadListener(XPBottler plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConfigReload(ConfigChangeEvent event) {
        if (event.plugin != plugin) return;

        // Reload lang files, lore, etc
        plugin.reloadLanguages();

        // Reload the item model
        plugin.reloadModelData();

        // Rebuild recipes last so the crafted Flask picks up the new text, model and Capacity
        plugin.unloadRecipes();
        plugin.loadRecipes();
    }
}
