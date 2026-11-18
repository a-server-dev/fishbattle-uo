package dev.taikin.mcplugins.fishbattle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class TechniqueItem {

    private TechniqueItem() {
    }

    public static ItemStack create(Technique technique) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            var pdc = meta.getPersistentDataContainer();
            pdc.set(FishKeys.TECHNIQUES, PersistentDataType.STRING, technique.id());
            meta.customName(Component.text("技カード: " + technique.name(), technique.color())
                    .decoration(TextDecoration.ITALIC, false));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(line(technique.hand().emoji() + " " + technique.hand().label() + " 用",
                    technique.hand().color()));
            lore.add(line("レアリティ: " + technique.rarity().label(), technique.rarity().color()));
            for (String effect : technique.effectLines()) {
                lore.add(line(effect, technique.color()));
            }
            lore.add(line(technique.description(), NamedTextColor.GRAY));
            lore.add(line("魚を持って右クリックで装備", NamedTextColor.DARK_GRAY));
            meta.lore(lore);
        });
        return item;
    }

    public static Technique read(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(FishKeys.TECHNIQUES, PersistentDataType.STRING);
        return id == null ? null : Technique.byId(id);
    }

    public static ItemStack random() {
        List<Technique> all = Technique.all();
        return create(all.get(ThreadLocalRandom.current().nextInt(all.size())));
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
