package me.z1haze.xpbottler;

import me.z1haze.commons.lang.LanguageLoader;
import me.z1haze.commons.model.ModelLoader;
import me.z1haze.commons.util.ChatUtils;
import me.z1haze.xpbottler.command.XPBottlerCommand;
import me.z1haze.xpbottler.listener.ExpBottleListener;
import me.z1haze.xpbottler.listener.PluginReloadListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class XPBottler extends JavaPlugin {

    private LanguageLoader languageLoader;
    private ModelLoader modelLoader;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ChatUtils.init(this, "&f&l[&3&lXPBottler&f&l]&r");

        languageLoader = new LanguageLoader(this);
        modelLoader = new ModelLoader(this);

        registerEvents();
        registerCommands();

        new Metrics(this, 34783);
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new ExpBottleListener(this), this);
        pm.registerEvents(new PluginReloadListener(this), this);
    }

    private void registerCommands() {
        XPBottlerCommand command = new XPBottlerCommand(this);

        PluginCommand xpb = getCommand("xpb");
        if (xpb == null) {
            ChatUtils.logColorized("&cCommand 'xpb' is missing from plugin.yml");
            return;
        }

        xpb.setExecutor(command);
        xpb.setTabCompleter(command);
    }

    public FileConfiguration getTranslations() {
        return languageLoader.getTranslations();
    }

    public void reloadLanguages() {
        languageLoader = new LanguageLoader(this);
    }

    public FileConfiguration getModelData() {
        return modelLoader.getModelData();
    }

    public void reloadModelData() {
        modelLoader = new ModelLoader(this);
    }

    public NamespacedKey getKey(String str) {
        return new NamespacedKey(this, str);
    }

    /**
     * A message from config.yml, uncolored and with placeholders still in it.
     */
    public String getMessage(String key) {
        return getConfig().getString("messages." + key, "&cMissing message: " + key);
    }

    public int getXpPerBottle() {
        return Math.max(1, getConfig().getInt("xp-per-bottle", 50));
    }
}
