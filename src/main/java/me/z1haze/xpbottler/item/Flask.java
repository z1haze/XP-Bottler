package me.z1haze.xpbottler.item;

import me.z1haze.commons.util.ChatUtils;
import me.z1haze.xpbottler.XPBottler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable XP holder that players Fill from themselves and Sip back from.
 * Built on a mundane potion so vanilla handles the drinking.
 * Both the XP and the Capacity live in the item's persistent data, so a Flask keeps the
 * Capacity it was made with even if flask.capacity changes later.
 */
public class Flask {
    public static final int UNLIMITED = -1;

    private static final String XP_KEY = "flask_xp";
    private static final String CAPACITY_KEY = "flask_capacity";

    private final XPBottler plugin;
    private final ItemStack stack;
    private final int capacity;
    private int xp;

    public Flask(XPBottler plugin, int xp, int capacity) {
        this.plugin = plugin;
        this.xp = xp;
        this.capacity = capacity;
        this.stack = createPotion();

        setItemMeta();
    }

    private Flask(XPBottler plugin, ItemStack stack, int xp, int capacity) {
        this.plugin = plugin;
        this.stack = stack;
        this.xp = xp;
        this.capacity = capacity;
    }

    /**
     * Reads a Flask made by this plugin, or returns null for anything else.
     * The returned Flask works on a copy, so put {@link #getItem()} back where it came from after changing it.
     */
    public static Flask from(XPBottler plugin, ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer xp = data.get(plugin.getKey(XP_KEY), PersistentDataType.INTEGER);
        Integer capacity = data.get(plugin.getKey(CAPACITY_KEY), PersistentDataType.INTEGER);
        if (xp == null || capacity == null) return null;

        return new Flask(plugin, item.clone(), xp, capacity);
    }

    public ItemStack getItem() {
        return stack;
    }

    public int getXp() {
        return xp;
    }

    public boolean isEmpty() {
        return xp <= 0;
    }

    /**
     * How much more XP fits before the Flask reaches its Capacity.
     */
    public int getRoom() {
        if (capacity == UNLIMITED) return Integer.MAX_VALUE - xp;
        return Math.max(0, capacity - xp);
    }

    /**
     * Changes the stored XP and refreshes the lore. The name is left alone so anvil renames survive.
     */
    public void setXp(int xp) {
        this.xp = xp;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(plugin.getKey(XP_KEY), PersistentDataType.INTEGER, xp);
        meta.setLore(getLore());
        stack.setItemMeta(meta);
    }

    /**
     * Fills {xp} and {capacity}, with the lang file's unlimited symbol when there's no Capacity.
     */
    public String withPlaceholders(String text) {
        return text
                .replace("{xp}", String.valueOf(xp))
                .replace("{capacity}", capacity == UNLIMITED ? getUnlimitedSymbol() : String.valueOf(capacity));
    }

    private void setItemMeta() {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        if (meta instanceof PotionMeta potion) {
            potion.setBasePotionType(PotionType.MUNDANE);
        }

        meta.setMaxStackSize(1);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.setDisplayName(ChatUtils.colorize(getTitle()));
        meta.setLore(getLore());

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(plugin.getKey(XP_KEY), PersistentDataType.INTEGER, xp);
        data.set(plugin.getKey(CAPACITY_KEY), PersistentDataType.INTEGER, capacity);

        plugin.applyItemModel(meta, "flask");

        stack.setItemMeta(meta);
    }

    /**
     * Swaps the potion's 1.6 second drink for flask.drink-seconds. Uses the component string rather
     * than a consumable API, since Spigot and Paper each have their own. Set once at creation, so
     * existing Flasks keep their drink time when the config changes.
     */
    private ItemStack createPotion() {
        return Bukkit.getItemFactory().createItemStack("minecraft:potion[consumable={consume_seconds:"
                + plugin.getFlaskDrinkSeconds()
                + ",animation:'drink',sound:'entity.generic.drink',has_consume_particles:false}]");
    }

    private String getTitle() {
        ConfigurationSection section = plugin.getTranslations().getConfigurationSection("flask");
        String title = section != null ? section.getString("title") : null;

        return withPlaceholders(title != null ? title : "&aExperience Flask");
    }

    private List<String> getLore() {
        List<String> lore = new ArrayList<>();

        ConfigurationSection section = plugin.getTranslations().getConfigurationSection("flask");
        if (section == null) return lore;

        for (String line : section.getStringList("lore")) {
            lore.add(ChatUtils.colorize(withPlaceholders(line)));
        }

        return lore;
    }

    private String getUnlimitedSymbol() {
        return plugin.getTranslations().getString("flask.unlimited", "∞");
    }
}
