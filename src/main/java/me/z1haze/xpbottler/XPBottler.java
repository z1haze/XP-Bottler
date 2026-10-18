package me.z1haze.xpbottler;

import me.z1haze.commons.lang.LanguageLoader;
import me.z1haze.commons.model.ModelLoader;
import me.z1haze.commons.recipe.RecipeLoader;
import me.z1haze.commons.util.ChatUtils;
import me.z1haze.commons.util.FileUtils;
import me.z1haze.xpbottler.command.XPBottlerCommand;
import me.z1haze.xpbottler.item.Flask;
import me.z1haze.xpbottler.listener.ExpBottleListener;
import me.z1haze.xpbottler.listener.FlaskListener;
import me.z1haze.xpbottler.listener.PluginReloadListener;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class XPBottler extends JavaPlugin {

    private LanguageLoader languageLoader;
    private ModelLoader modelLoader;
    private List<NamespacedKey> recipeKeys = List.of();

    @Override
    public void onEnable() {
        FileUtils.saveAndMergeDefaultConfig(this);

        ChatUtils.init(this, "&f&l[&3&lXPBottler&f&l]&r");

        languageLoader = new LanguageLoader(this);
        modelLoader = new ModelLoader(this);

        loadRecipes();
        registerEvents();
        registerCommands();

        new Metrics(this, 34783);
    }

    @Override
    public void onDisable() {
        unloadRecipes();
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new ExpBottleListener(this), this);
        pm.registerEvents(new FlaskListener(this), this);
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

    /**
     * Registers the Flask recipes and puts them in every online player's recipe book.
     * The crafted Flask gets the Capacity configured at the time of loading.
     */
    public void loadRecipes() {
        ItemStack flask = new Flask(this, 0, getFlaskCapacity()).getItem();
        recipeKeys = new RecipeLoader(this).loadRecipes("recipes.flask", "flask", flask);

        getServer().getOnlinePlayers().forEach(player -> player.discoverRecipes(recipeKeys));
    }

    /**
     * Removes exactly the recipes that were loaded, even if their ids have since changed in config.
     */
    public void unloadRecipes() {
        new RecipeLoader(this).unloadRecipes(recipeKeys);
        recipeKeys = List.of();
    }

    public List<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    /**
     * Points the item at the model set under {@code <path>.item_model} in modelData.yml, if any.
     */
    public void applyItemModel(ItemMeta meta, String path) {
        String model = getModelData().getString(path + ".item_model", "");
        if (model == null || model.isBlank()) return;

        NamespacedKey key = NamespacedKey.fromString(model);
        if (key != null) {
            meta.setItemModel(key);
        } else {
            ChatUtils.logColorized("&cInvalid item_model '" + model + "' in modelData.yml, using the vanilla look");
        }
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

    /**
     * How much XP one Sip or Fill moves.
     */
    public int getXpPerSip() {
        return Math.max(1, getConfig().getInt("flask.xp-per-sip", 250));
    }

    public float getFlaskDrinkSeconds() {
        return (float) Math.max(0.05, getConfig().getDouble("flask.drink-seconds", 0.8));
    }

    /**
     * The Capacity new Flasks are made with. Anything below 1 means no limit.
     */
    public int getFlaskCapacity() {
        int capacity = getConfig().getInt("flask.capacity", Flask.UNLIMITED);
        return capacity > 0 ? capacity : Flask.UNLIMITED;
    }

    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(ChatUtils.colorize(message)));
    }

    /**
     * Plays the sound configured at {@code path} (enabled, name, volume, pitch) to the player.
     */
    @SuppressWarnings("deprecation") // Sound.valueOf still resolves both BUKKIT_NAMES and vanilla keys
    public void playSound(Player player, String path) {
        ConfigurationSection sound = getConfig().getConfigurationSection(path);
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
}
