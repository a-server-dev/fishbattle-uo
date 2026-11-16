package dev.taikin.mcplugins.fishbattle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class BattleHandMenu implements Listener {

    private static final int[] SLOTS = {2, 4, 6};

    private final Set<UUID> open = new HashSet<>();

    public void open(Player player, List<Rps> hands, Consumer<Rps> onPick, Runnable onCancel) {
        Holder holder = new Holder(hands, onPick, onCancel);
        Inventory inventory = Bukkit.createInventory(holder, 9,
                Component.text("手を選べ（クリックで決定）", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        for (int i = 0; i < hands.size() && i < SLOTS.length; i++) {
            inventory.setItem(SLOTS[i], item(hands.get(i)));
        }
        open.add(player.getUniqueId());
        player.openInventory(inventory);
    }

    public void close(Player player) {
        if (player == null) {
            return;
        }
        if (open.remove(player.getUniqueId())) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        Rps hand = holder.handAt(slot);
        if (hand == null) {
            return;
        }
        Consumer<Rps> callback = holder.onPick;
        open.remove(player.getUniqueId());
        player.closeInventory();
        if (callback != null) {
            callback.accept(hand);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!open.remove(player.getUniqueId())) {
            return;
        }
        Runnable cancel = holder.onCancel;
        if (cancel != null) {
            cancel.run();
        }
    }

    private static ItemStack item(Rps hand) {
        Material material = switch (hand) {
            case GU -> Material.BLUE_DYE;
            case CHOKI -> Material.RED_DYE;
            case PA -> Material.YELLOW_DYE;
        };
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.customName(Component.text(hand.emoji() + " " + hand.label(), hand.color())
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("クリックでこの手に決定", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        });
        return item;
    }

    private static final class Holder implements org.bukkit.inventory.InventoryHolder {
        private final List<Rps> hands;
        private final Consumer<Rps> onPick;
        private final Runnable onCancel;
        private Inventory inventory;

        private Holder(List<Rps> hands, Consumer<Rps> onPick, Runnable onCancel) {
            this.hands = hands;
            this.onPick = onPick;
            this.onCancel = onCancel;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private Rps handAt(int slot) {
            for (int i = 0; i < hands.size() && i < SLOTS.length; i++) {
                if (SLOTS[i] == slot) {
                    return hands.get(i);
                }
            }
            return null;
        }
    }
}
