package dev.taikin.mcplugins.fishbattle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.bukkit.persistence.PersistentDataType;

public final class OtherFishMenu implements Listener {

    private static final int SLOT_INFO = 4;
    private static final int SLOT_HP_LABEL = 9;
    private static final int SLOT_HP_BAR = 10;
    private static final int SLOT_POWER_LABEL = 18;
    private static final int SLOT_POWER_BAR = 19;
    private static final int SLOT_TECH_LABEL = 27;
    private static final int SLOT_TECH_BAR = 28;
    private static final int SLOT_MOVES = 36;
    private static final int SLOT_CHALLENGE = 44;
    private static final double DEFAULT_STAKE = 100.0;

    private final org.bukkit.plugin.Plugin plugin;
    private final BattleManager battles;
    private final Map<UUID, UUID> openMenus = new HashMap<>();

    public OtherFishMenu(org.bukkit.plugin.Plugin plugin, BattleManager battles) {
        this.plugin = plugin;
        this.battles = battles;
    }

    public void open(Player player, Entity entity) {
        Fish fish = FishData.read(entity);
        if (fish == null) {
            return;
        }
        String ownerId = entity.getPersistentDataContainer().get(FishKeys.OWNER, PersistentDataType.STRING);
        openMenus.put(player.getUniqueId(), entity.getUniqueId());
        Holder holder = new Holder(entity.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 45,
                Component.text(CaptureListener.ownerName(ownerId) + " の魚", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        render(inventory, fish, ownerId);
        player.openInventory(inventory);
    }

    private void render(Inventory inventory, Fish fish, String ownerId) {
        inventory.setItem(SLOT_INFO, item(infoIcon(fish), FishItems.name(fish),
                List.of(line("飼い主: " + CaptureListener.ownerName(ownerId), NamedTextColor.GRAY),
                        line("属性: " + fish.type().label(), fish.type().color()),
                        line("Lv" + fish.level(), NamedTextColor.AQUA))));

        statRow(inventory, SLOT_HP_LABEL, SLOT_HP_BAR, fish.hpRank(), "体力", fish.maxHp(),
                PetMenu.maxHp(), NamedTextColor.GREEN);
        statRow(inventory, SLOT_POWER_LABEL, SLOT_POWER_BAR, fish.powerRank(), "強さ", fish.power(),
                PetMenu.maxPower(), NamedTextColor.RED);
        statRow(inventory, SLOT_TECH_LABEL, SLOT_TECH_BAR, fish.techRank(), "テクニック",
                fish.technique(), PetMenu.maxTech(), NamedTextColor.AQUA);

        List<Component> moves = new ArrayList<>();
        if (fish.moves().isEmpty()) {
            moves.add(line("技: なし", NamedTextColor.DARK_GRAY));
        } else {
            for (var entry : fish.moves().entrySet()) {
                moves.add(line(entry.getKey().emoji() + " " + entry.getValue().name()
                        + " x" + String.format("%.2f", entry.getValue().bonus()),
                        entry.getValue().color()));
            }
        }
        inventory.setItem(SLOT_MOVES, item(Material.PAPER,
                Component.text("技", NamedTextColor.AQUA), moves));

        inventory.setItem(SLOT_CHALLENGE, item(Material.DIAMOND_SWORD,
                Component.text("対戦を申し込む", NamedTextColor.GOLD),
                List.of(line("賭け金 " + (int) DEFAULT_STAKE, NamedTextColor.GRAY))));
    }

    private static void statRow(Inventory inventory, int labelSlot, int barSlot, Rank rank,
            String name, int value, int max, NamedTextColor color) {
        int clamped = Math.max(0, Math.min(value, max));
        ItemStack label = new ItemStack(rank.icon());
        label.editMeta(meta -> {
            meta.customName(Component.text(rank.display() + " " + name + ": " + value, rank.color())
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line("最大 " + max + " 中の " + clamped, NamedTextColor.DARK_GRAY)));
        });
        inventory.setItem(labelSlot, label);
        int filled = max <= 0 ? 0 : (int) Math.round(PetMenu.STAT_BARS * (double) clamped / max);
        for (int i = 0; i < PetMenu.STAT_BARS; i++) {
            inventory.setItem(barSlot + i, PetMenu.barPiece(i < filled, color, name, value, max));
        }
    }

    private static Material infoIcon(Fish fish) {
        return switch (fish.speciesKey()) {
            case "cod" -> Material.COD_SPAWN_EGG;
            case "salmon" -> Material.SALMON_SPAWN_EGG;
            case "pufferfish" -> Material.PUFFERFISH_SPAWN_EGG;
            case "tadpole" -> Material.TADPOLE_SPAWN_EGG;
            default -> fish.speciesKey().startsWith("axolotl:")
                    ? Material.AXOLOTL_SPAWN_EGG : Material.TROPICAL_FISH_SPAWN_EGG;
        };
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
        if (event.getRawSlot() != SLOT_CHALLENGE) {
            return;
        }
        Entity entity = Bukkit.getEntity(holder.entity);
        if (entity == null || !entity.isValid()) {
            player.closeInventory();
            return;
        }
        String ownerId = entity.getPersistentDataContainer().get(FishKeys.OWNER, PersistentDataType.STRING);
        if (ownerId == null) {
            return;
        }
        Player owner;
        try {
            owner = Bukkit.getPlayer(UUID.fromString(ownerId));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (owner == null) {
            player.sendMessage(Component.text("飼い主はオンラインではありません。", NamedTextColor.RED));
            return;
        }
        player.closeInventory();
        battles.challenge(player, owner, DEFAULT_STAKE);
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

    static boolean owns(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    private static final class Holder implements org.bukkit.inventory.InventoryHolder {
        private final UUID entity;
        private Inventory inventory;

        private Holder(UUID entity) {
            this.entity = entity;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
