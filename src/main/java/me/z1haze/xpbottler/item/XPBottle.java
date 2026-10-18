package me.z1haze.xpbottler.item;

import me.z1haze.commons.util.ChatUtils;
import me.z1haze.xpbottler.XPBottler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * A vanilla experience bottle that remembers how much XP it holds.
 * The amount lives in the item's persistent data, so bottles keep their value
 * even if xp-per-bottle changes later.
 */
public class XPBottle {
    private static final String XP_KEY = "xp";

    private final XPBottler plugin;
    private final int xp;
    private final ItemStack stack;

    public XPBottle(XPBottler plugin, int xp, int amount) {
        this.plugin = plugin;
        this.xp = xp;
        this.stack = new ItemStack(Material.EXPERIENCE_BOTTLE, amount);

        setItemMeta();
    }

    public ItemStack getItem() {
        return stack;
    }

    /**
     * Reads the XP stored in a bottle made by this plugin.
     * Returns -1 for vanilla bottles and anything that isn't an experience bottle.
     */
    public static int getStoredXp(XPBottler plugin, ItemStack item) {
        if (item == null || item.getType() != Material.EXPERIENCE_BOTTLE) return -1;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;

        Integer stored = meta.getPersistentDataContainer().get(plugin.getKey(XP_KEY), PersistentDataType.INTEGER);
        return stored == null ? -1 : stored;
    }

    private void setItemMeta() {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(ChatUtils.colorize(getTitle()));
        meta.setLore(getLore());
        meta.getPersistentDataContainer().set(plugin.getKey(XP_KEY), PersistentDataType.INTEGER, xp);

        String model = plugin.getModelData().getString("bottle.item_model", "");
        if (model != null && !model.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(model);
            if (key != null) {
                meta.setItemModel(key);
            } else {
                ChatUtils.logColorized("&cInvalid item_model '" + model + "' in modelData.yml, using the vanilla look");
            }
        }

        stack.setItemMeta(meta);
    }

    private String getTitle() {
        ConfigurationSection section = plugin.getTranslations().getConfigurationSection("bottle");
        String title = section != null ? section.getString("title") : null;

        return withPlaceholders(title != null ? title : "&aBottle o' Enchanting");
    }

    private List<String> getLore() {
        List<String> lore = new ArrayList<>();

        ConfigurationSection section = plugin.getTranslations().getConfigurationSection("bottle");
        if (section == null) return lore;

        for (String line : section.getStringList("lore")) {
            lore.add(ChatUtils.colorize(withPlaceholders(line)));
        }

        return lore;
    }

    private String withPlaceholders(String text) {
        return text.replace("{xp}", String.valueOf(xp));
    }
}
