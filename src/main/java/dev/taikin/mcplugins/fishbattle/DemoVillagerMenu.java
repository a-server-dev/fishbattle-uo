package dev.taikin.mcplugins.fishbattle;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public final class DemoVillagerMenu implements Listener {

    private static final int SLOT_SPECIES = 11;
    private static final int SLOT_LEVEL_DOWN = 12;
    private static final int SLOT_LEVEL = 13;
    private static final int SLOT_LEVEL_UP = 14;
    private static final int SLOT_TYPE = 15;
    private static final int SLOT_RANK = 16;
    private static final int SLOT_INFO = 4;
    private static final int SLOT_START = 22;

    private final Plugin plugin;
    private final BattleManager battles;
    private final java.util.Map<UUID, UUID> openMenus = new java.util.HashMap<>();

    public DemoVillagerMenu(Plugin plugin, BattleManager battles) {
        this.plugin = plugin;
        this.battles = battles;
    }

    public void open(Player player, Entity villager) {
        if (!DemoVillager.isDemoVillager(villager)) {
            return;
        }
        openMenus.put(player.getUniqueId(), villager.getUniqueId());
        Holder holder = new Holder(villager.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("デモの村人", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        render(inventory, villager);
        player.openInventory(inventory);
    }

    private void render(Inventory inventory, Entity villager) {
        DemoVillager.Settings settings = DemoVillager.read(villager);
        FishSpecies species = FishSpecies.byKey(settings.speciesKey());
        Fish preview = settings.toFish();

        inventory.setItem(SLOT_INFO, item(Material.VILLAGER_SPAWN_EGG,
                Component.text("相手: " + species.name(), NamedTextColor.GOLD),
                List.of(line("レベル " + settings.level(), NamedTextColor.WHITE),
                        line("属性 " + settings.type().label(), settings.type().color()),
                        line("個体値 " + settings.rank().display(), settings.rank().color()),
                        line("HP " + preview.maxHp() + " / 強さ " + preview.power()
                                + " / テク " + preview.technique(), NamedTextColor.GRAY))));

        inventory.setItem(SLOT_SPECIES, item(spawnIcon(settings.speciesKey()),
                Component.text("魚を変える: " + species.name(), NamedTextColor.AQUA),
                List.of(line("左クリックで次、右クリックで前", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_LEVEL_DOWN, item(Material.RED_DYE,
                Component.text("レベル -1", NamedTextColor.RED), List.of()));
        inventory.setItem(SLOT_LEVEL, item(Material.EXPERIENCE_BOTTLE,
                Component.text("レベル " + settings.level(), NamedTextColor.AQUA),
                List.of(line("最大 " + Fish.MAX_LEVEL, NamedTextColor.GRAY))));
        inventory.setItem(SLOT_LEVEL_UP, item(Material.LIME_DYE,
                Component.text("レベル +1", NamedTextColor.GREEN), List.of()));

        inventory.setItem(SLOT_TYPE, item(settings.type().hand().icon(),
                Component.text("属性: " + settings.type().label(), settings.type().color()),
                List.of(line("左クリックで次、右クリックで前", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_RANK, item(settings.rank().icon(),
                Component.text("個体値: " + settings.rank().display(), settings.rank().color()),
                List.of(line("左クリックで次、右クリックで前", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_START, item(Material.DIAMOND_SWORD,
                Component.text("バトル開始", NamedTextColor.GOLD),
                List.of(line("この設定の相手と戦う", NamedTextColor.GRAY))));
    }

    private static Material spawnIcon(String speciesKey) {
        if (speciesKey.equals("cod")) {
            return Material.COD_SPAWN_EGG;
        }
        if (speciesKey.equals("salmon")) {
            return Material.SALMON_SPAWN_EGG;
        }
        if (speciesKey.equals("pufferfish")) {
            return Material.PUFFERFISH_SPAWN_EGG;
        }
        if (speciesKey.equals("tadpole")) {
            return Material.TADPOLE_SPAWN_EGG;
        }
        if (speciesKey.startsWith("axolotl:")) {
            return Material.AXOLOTL_SPAWN_EGG;
        }
        return Material.TROPICAL_FISH_SPAWN_EGG;
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.customName(name.decoration(TextDecoration.ITALIC, false));
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
        });
        return item;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !owns(event.getClickedInventory())) {
            return;
        }
        Entity villager = Bukkit.getEntity(holder.villager);
        if (villager == null || !villager.isValid()) {
            player.closeInventory();
            return;
        }
        DemoVillager.Settings settings = DemoVillager.read(villager);
        int direction = event.isRightClick() ? -1 : 1;
        switch (event.getRawSlot()) {
            case SLOT_SPECIES -> DemoVillager.write(villager,
                    DemoVillager.nextSpecies(settings, direction));
            case SLOT_LEVEL_DOWN -> DemoVillager.write(villager,
                    DemoVillager.shiftLevel(settings, -1));
            case SLOT_LEVEL_UP -> DemoVillager.write(villager,
                    DemoVillager.shiftLevel(settings, 1));
            case SLOT_TYPE -> DemoVillager.write(villager,
                    DemoVillager.cycleType(settings, direction));
            case SLOT_RANK -> DemoVillager.write(villager,
                    DemoVillager.cycleRank(settings, direction));
            case SLOT_START -> {
                player.closeInventory();
                battles.startDemo(player, settings);
                return;
            }
            default -> {
                return;
            }
        }
        render(event.getView().getTopInventory(), villager);
        player.updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (owns(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (owns(event.getView().getTopInventory()) && event.getPlayer() instanceof Player player) {
            openMenus.remove(player.getUniqueId());
        }
    }

    private static boolean owns(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    private static final class Holder implements org.bukkit.inventory.InventoryHolder {
        private final UUID villager;
        private Inventory inventory;

        private Holder(UUID villager) {
            this.villager = villager;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
