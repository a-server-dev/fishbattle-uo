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
import org.bukkit.entity.PufferFish;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class PetMenu {

    public static final int SLOT_PUFF = 3;
    public static final int SLOT_INFO = 4;
    public static final int SLOT_RENAME = 5;
    public static final int SLOT_FOLLOW = 6;
    public static final int SLOT_TYPE = 7;
    public static final int SLOT_STATS = 8;
    public static final int SLOT_COLLECT = 13;
    public static final int SLOT_RELEASE = 26;

    private static final int[] DIVIDER = {2, 11, 20};

    private static final int SLOT_POWER_LABEL = 0;
    private static final int SLOT_POWER_BAR = 1;
    private static final int SLOT_HP_LABEL = 9;
    private static final int SLOT_HP_BAR = 10;
    private static final int SLOT_TECH_LABEL = 18;
    private static final int SLOT_TECH_BAR = 19;
    private static final int SLOT_EXP_LABEL = 27;
    private static final int SLOT_EXP_BAR = 28;
    private static final int SLOT_BACK = 40;
    private static final int STAT_BARS = 8;
    private static final int EXP_BARS = 8;
    private static final int[] HAND_ICON = {0, 9, 18};
    private static final int[] SLOT_TECH = {1, 10, 19};

    private static final double RANK_TOP = Rank.DIAMOND.multiplier();
    private static final int MAX_POWER = (int) Math.ceil(RANK_TOP * (FishSpecies.allKeys().stream()
            .mapToInt(key -> FishSpecies.byKey(key).power()).max().orElse(100) + Fish.MAX_LEVEL));
    private static final int MAX_HP = (int) Math.ceil(RANK_TOP * (FishSpecies.allKeys().stream()
            .mapToInt(key -> FishSpecies.byKey(key).hp()).max().orElse(100)
            + (Fish.MAX_LEVEL - 1) * 2));
    private static final int MAX_TECH = (int) Math.ceil(RANK_TOP * (FishSpecies.allKeys().stream()
            .mapToInt(key -> FishSpecies.byKey(key).technique()).max().orElse(100) + Fish.MAX_LEVEL / 2));

    private static final int SLOTS = 27;
    private static final int STATS_SLOTS = 45;
    private final org.bukkit.plugin.Plugin plugin;
    private final FishMotion motion;
    private BattleManager battles;
    private final RenameDialog renameDialog;
    private final Map<UUID, UUID> lastPet = new HashMap<>();
    private final Map<UUID, UUID> openMenus = new HashMap<>();
    private final Map<UUID, Long> releaseConfirm = new HashMap<>();
    private static final long RELEASE_CONFIRM_MILLIS = 5000L;

    public void setBattles(BattleManager battles) {
        this.battles = battles;
    }

    public PetMenu(org.bukkit.plugin.Plugin plugin, FishMotion motion) {
        this.plugin = plugin;
        this.motion = motion;
        this.renameDialog = new RenameDialog(motion);
    }

    public void open(Player player, Entity entity) {
        if (!FishMotion.hasData(entity)) {
            return;
        }
        lastPet.put(player.getUniqueId(), entity.getUniqueId());
        Holder holder = new Holder(Page.MAIN, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, SLOTS,
                Component.text("魚の設定", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        render(inventory, entity, holder);
        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), entity.getUniqueId());
    }

    private void openStats(Player player, Entity entity) {
        lastPet.put(player.getUniqueId(), entity.getUniqueId());
        Holder holder = new Holder(Page.STATS, player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, STATS_SLOTS,
                Component.text("パラメータ", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        render(inventory, entity, holder);
        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), entity.getUniqueId());
    }

    public void refreshIfOpen(Player player) {
        UUID entityId = openMenus.get(player.getUniqueId());
        if (entityId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(entityId);
        if (entity == null || !entity.isValid()) {
            openMenus.remove(player.getUniqueId());
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof Holder holder) {
            render(top, entity, holder);
            player.updateInventory();
        }
    }

    private void render(Inventory inventory, Entity entity, Holder holder) {
        Fish fish = FishData.read(entity);
        if (fish == null) {
            return;
        }
        if (holder != null && holder.page == Page.STATS) {
            renderStats(inventory, fish);
            return;
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }
        for (int slot : DIVIDER) {
            inventory.setItem(slot, filler());
        }
        inventory.setItem(SLOT_INFO, infoItem(fish));
        inventory.setItem(SLOT_TYPE, typeItem(fish));
        inventory.setItem(SLOT_RENAME, actionItem(Material.NAME_TAG, "名前を変える",
                NamedTextColor.GREEN, List.of("チャットに新しい名前を入力")));
        boolean follow = follows(entity);
        inventory.setItem(SLOT_FOLLOW, actionItem(Material.LEAD,
                follow ? "追従: ON" : "追従: OFF",
                follow ? NamedTextColor.YELLOW : NamedTextColor.GRAY,
                List.of(follow ? "クリックでその場に留める" : "クリックで追従させる")));
        inventory.setItem(SLOT_COLLECT, actionItem(Material.BUCKET, "回収する",
                NamedTextColor.AQUA, List.of("バケツに戻してインベントリへ")));
        UUID viewer = holder == null ? null : holder.viewer;
        if (isConfirmingRelease(viewer)) {
            inventory.setItem(SLOT_RELEASE, actionItem(Material.BARRIER, "本当に逃がす？（もう一度クリック）",
                    NamedTextColor.DARK_RED, List.of("5秒以内にもう一度クリックで確定",
                            "戻すには別の場所をクリック")));
        } else {
            inventory.setItem(SLOT_RELEASE, actionItem(Material.LEAD, "逃がす",
                    NamedTextColor.YELLOW, List.of("魚を野生に戻します（元に戻せません）",
                            "クリック後にもう一度確認します")));
        }

        inventory.setItem(SLOT_STATS, actionItem(Material.BOOK, "パラメータを見る",
                NamedTextColor.LIGHT_PURPLE, List.of("強さ・体力・テクニック・EXPのバー")));

        if (entity instanceof PufferFish puffer) {
            inventory.setItem(SLOT_PUFF, puffItem(puffer.getPuffState()));
        }

        List<Rps> hands = List.of(Rps.GU, Rps.CHOKI, Rps.PA);
        for (int i = 0; i < hands.size(); i++) {
            Rps hand = hands.get(i);
            Technique technique = fish.techniqueFor(hand);
            inventory.setItem(HAND_ICON[i], handItem(hand, technique));
            if (technique != null) {
                inventory.setItem(SLOT_TECH[i], techniqueItem(technique, hand));
            } else {
                inventory.setItem(SLOT_TECH[i], emptyTechniqueItem(hand));
            }
        }
    }

    static void applyPuff(PufferFish puffer, int state) {
        puffer.setPuffState(state == 0 ? 1 : 0);
        puffer.setPuffState(state);
    }

    private Entity respawnPuffer(Entity old, int puff) {
        Fish fish = FishData.read(old);
        if (fish == null) {
            return null;
        }
        var oldData = old.getPersistentDataContainer();
        String owner = oldData.get(FishKeys.OWNER, PersistentDataType.STRING);
        Double ax = oldData.get(FishKeys.ANCHOR_X, PersistentDataType.DOUBLE);
        Double ay = oldData.get(FishKeys.ANCHOR_Y, PersistentDataType.DOUBLE);
        Double az = oldData.get(FishKeys.ANCHOR_Z, PersistentDataType.DOUBLE);
        Byte follow = oldData.get(FishKeys.FOLLOW, PersistentDataType.BYTE);
        var location = old.getLocation();

        Entity fresh = CaptureListener.spawnFor(fish, location);
        if (!(fresh instanceof PufferFish puffer)) {
            if (fresh != null) {
                fresh.remove();
            }
            return null;
        }
        CaptureListener.applyData(fresh, fish);
        var data = fresh.getPersistentDataContainer();
        if (owner != null) {
            data.set(FishKeys.OWNER, PersistentDataType.STRING, owner);
        }
        data.set(FishKeys.PUFF, PersistentDataType.INTEGER, puff);
        if (follow != null) {
            data.set(FishKeys.FOLLOW, PersistentDataType.BYTE, follow);
        }
        if (ax != null && ay != null && az != null) {
            data.set(FishKeys.ANCHOR_X, PersistentDataType.DOUBLE, ax);
            data.set(FishKeys.ANCHOR_Y, PersistentDataType.DOUBLE, ay);
            data.set(FishKeys.ANCHOR_Z, PersistentDataType.DOUBLE, az);
        }
        fresh.setPersistent(true);
        puffer.setPuffState(puff);

        motion.untrack(old);
        old.remove();
        motion.track(fresh);
        return fresh;
    }

    private ItemStack puffItem(int state) {
        String[] labels = {"ふつう", "ふくらみ", "最大"};
        ItemStack item = new ItemStack(Material.PUFFERFISH);
        item.editMeta(meta -> {
            meta.customName(Component.text("ふくらませる: " + labels[Math.max(0, Math.min(2, state))],
                    NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line("クリックで ふつう → ふくらみ → 最大 と切り替え",
                    NamedTextColor.GRAY), line("攻撃判定はありません", NamedTextColor.DARK_GRAY)));
        });
        return item;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        item.editMeta(meta -> meta.customName(Component.text(" ")));
        return item;
    }

    private ItemStack handItem(Rps hand, Technique technique) {
        ItemStack item = new ItemStack(hand.icon());
        item.editMeta(meta -> {
            meta.customName(Component.text(hand.emoji() + " " + hand.label() + "の技",
                    hand.color()).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line(technique != null
                    ? "装備: " + technique.name()
                    : "この手で勝つと右の技が発動", NamedTextColor.GRAY)));
        });
        return item;
    }

    private void renderStats(Inventory inventory, Fish fish) {
        statRow(inventory, SLOT_POWER_LABEL, SLOT_POWER_BAR, "強さ", fish.power(),
                fish.powerRank(), MAX_POWER, NamedTextColor.RED, "与えるダメージの大きさ");
        statRow(inventory, SLOT_HP_LABEL, SLOT_HP_BAR, "体力", fish.maxHp(),
                fish.hpRank(), MAX_HP, NamedTextColor.GREEN,
                "HPの高さ（現在 " + fish.hp() + "/" + fish.maxHp() + "）");
        statRow(inventory, SLOT_TECH_LABEL, SLOT_TECH_BAR, "テクニック", fish.technique(),
                fish.techRank(), MAX_TECH, NamedTextColor.AQUA, "気合の溜まりやすさ");
        expRow(inventory, fish);
        inventory.setItem(SLOT_BACK, actionItem(Material.ARROW, "戻る",
                NamedTextColor.WHITE, List.of("設定画面に戻る")));
    }

    private void statRow(Inventory inventory, int labelSlot, int barSlot, String name, int value,
            Rank rank, int max, NamedTextColor color, String hint) {
        int clamped = Math.max(0, Math.min(value, max));
        ItemStack label = new ItemStack(rank.icon());
        label.editMeta(meta -> {
            meta.customName(Component.text(rank.label() + " " + name + ": " + value, rank.color())
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line(hint, NamedTextColor.GRAY),
                    line("個体値: " + rank.label() + "（x" + String.format("%.2f", rank.multiplier())
                            + "）", rank.color()),
                    line("最大 " + max + " 中の " + clamped, NamedTextColor.DARK_GRAY)));
        });
        inventory.setItem(labelSlot, label);
        int filled = max <= 0 ? 0 : (int) Math.round(STAT_BARS * (double) clamped / max);
        for (int i = 0; i < STAT_BARS; i++) {
            inventory.setItem(barSlot + i, barPiece(i < filled, color, name, value, max));
        }
    }

    private void expRow(Inventory inventory, Fish fish) {
        int need = Math.max(1, fish.nextExp());
        boolean capped = fish.level() >= Fish.MAX_LEVEL;
        int have = capped ? need : Math.max(0, Math.min(fish.exp(), need));
        ItemStack label = new ItemStack(Material.EXPERIENCE_BOTTLE);
        label.editMeta(meta -> {
            meta.customName(Component.text("EXP: " + have + " / " + need
                    + "（Lv" + fish.level() + (capped ? " MAX" : "") + "）",
                    NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line(capped ? "最大レベルです" : "つぎのレベルまで " + (need - have) + " EXP",
                    NamedTextColor.GRAY)));
        });
        inventory.setItem(SLOT_EXP_LABEL, label);
        int filled = capped ? EXP_BARS : (int) Math.round(EXP_BARS * (double) have / need);
        for (int i = 0; i < EXP_BARS; i++) {
            inventory.setItem(SLOT_EXP_BAR + i,
                    barPiece(i < filled, NamedTextColor.AQUA, "EXP", have, need));
        }
    }

    private ItemStack barPiece(boolean filled, NamedTextColor color, String name, int value, int max) {
        Material material = filled ? glassFor(color) : Material.LIGHT_GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.customName(Component.text(name + " " + value + " / " + max,
                    filled ? color : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line(filled ? "■■■■■" : "□□□□□", filled ? color : NamedTextColor.GRAY)));
        });
        return item;
    }

    private static Material glassFor(NamedTextColor color) {
        if (color == NamedTextColor.RED) {
            return Material.RED_STAINED_GLASS_PANE;
        }
        if (color == NamedTextColor.GREEN) {
            return Material.LIME_STAINED_GLASS_PANE;
        }
        if (color == NamedTextColor.AQUA) {
            return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        }
        if (color == NamedTextColor.YELLOW) {
            return Material.YELLOW_STAINED_GLASS_PANE;
        }
        return Material.WHITE_STAINED_GLASS_PANE;
    }

    private ItemStack infoItem(Fish fish) {
        Material material = switch (fish.type()) {
            case BLUE -> Material.COD_SPAWN_EGG;
            case RED -> Material.SALMON_SPAWN_EGG;
            case YELLOW -> Material.PUFFERFISH_SPAWN_EGG;
        };
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.customName(FishItems.name(fish));
            List<Component> lore = new ArrayList<>();
            lore.add(line("属性: " + fish.type().label(), fish.type().color()));
            lore.add(line("個体値: " + fish.powerRank().label() + " / "
                    + fish.hpRank().label() + " / " + fish.techRank().label(), NamedTextColor.GOLD));
            lore.add(line("HP " + fish.hp() + " / " + fish.maxHp(), NamedTextColor.RED));
            lore.add(line("Lv" + fish.level() + " / つぎまで "
                    + Math.max(0, fish.nextExp() - fish.exp()) + " EXP", NamedTextColor.AQUA));
            meta.lore(lore);
        });
        return item;
    }

    private ItemStack typeItem(Fish fish) {
        ItemStack item = new ItemStack(fish.type().hand().icon());
        item.editMeta(meta -> {
            meta.customName(Component.text("属性: " + fish.type().label(), fish.type().color())
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line("捕獲時に決まる属性（変更できません）", NamedTextColor.DARK_GRAY)));
        });
        return item;
    }

    private ItemStack techniqueItem(Technique technique, Rps hand) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> {
            String name = technique != null ? technique.name() : "不明な技";
            NamedTextColor color = technique != null ? technique.color() : NamedTextColor.GRAY;
            meta.customName(Component.text(hand.emoji() + " " + name, color)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            if (technique != null) {
                lore.add(line("威力 x" + String.format("%.2f", technique.bonus()), color));
                lore.add(line(technique.description(), NamedTextColor.GRAY));
            }
            lore.add(line("クリックで外す", NamedTextColor.RED));
            meta.lore(lore);
        });
        return item;
    }

    private ItemStack emptyTechniqueItem(Rps hand) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        item.editMeta(meta -> {
            meta.customName(Component.text(hand.emoji() + " " + hand.label() + "（技なし）",
                    NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(line("技カードを持って魚を右クリックで装備", NamedTextColor.GRAY)));
        });
        return item;
    }

    private ItemStack actionItem(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.customName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            List<Component> lines = new ArrayList<>();
            for (String text : lore) {
                lines.add(line(text, NamedTextColor.GRAY));
            }
            meta.lore(lines);
        });
        return item;
    }

    public void handleClick(Player player, int slot) {
        Entity entity = entityOf(player);
        if (entity == null) {
            player.closeInventory();
            return;
        }
        if (slot != SLOT_RELEASE) {
            releaseConfirm.remove(player.getUniqueId());
        }
        switch (slot) {
            case SLOT_STATS -> {
                openStats(player, entity);
                return;
            }
            case SLOT_BACK -> {
                open(player, entity);
                return;
            }
            case SLOT_PUFF -> {
                if (entity instanceof PufferFish puffer) {
                    int next = (puffer.getPuffState() + 1) % 3;
                    player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, 0.7f,
                            next == 0 ? 0.8f : 1.0f + next * 0.2f);
                    if (battles != null && battles.inBattle(player.getUniqueId())) {
                        entity.getPersistentDataContainer().set(FishKeys.PUFF,
                                PersistentDataType.INTEGER, next);
                        applyPuff(puffer, next);
                        refreshIfOpen(player);
                        return;
                    }
                    Entity replacement = respawnPuffer(entity, next);
                    if (replacement != null) {
                        open(player, replacement);
                    }
                }
                return;
            }
            case SLOT_TYPE -> player.sendActionBar(
                    net.kyori.adventure.text.Component.text("属性は捕獲時に決まり変更できません",
                            NamedTextColor.GRAY));
            case SLOT_RENAME -> {
                player.closeInventory();
                renameDialog.open(player, entity);
            }
            case SLOT_FOLLOW -> {
                boolean follow = motion.toggleFollow(entity);
                player.sendMessage(Component.text(follow ? "追従するようにしました。" : "その場に留まるようにしました。",
                        NamedTextColor.YELLOW));
                refreshIfOpen(player);
            }
            case SLOT_COLLECT -> {
                CaptureListener.collect(player, entity, motion);
                close(player);
                player.closeInventory();
            }
            case SLOT_RELEASE -> {
                if (!isConfirmingRelease(player.getUniqueId())) {
                    releaseConfirm.put(player.getUniqueId(),
                            System.currentTimeMillis() + RELEASE_CONFIRM_MILLIS);
                    player.sendMessage(Component.text("もう一度「逃がす」をクリックすると確定します（5秒以内）。",
                            NamedTextColor.YELLOW));
                    refreshIfOpen(player);
                    return;
                }
                releaseConfirm.remove(player.getUniqueId());
                motion.untrack(entity);
                entity.remove();
                player.sendMessage(Component.text("魚を逃がしました。", NamedTextColor.GRAY));
                close(player);
                player.closeInventory();
            }
            default -> {
                if (!isMainPage(player)) {
                    return;
                }
                Rps hand = handAtSlot(slot);
                if (hand != null) {
                    untrain(player, entity, hand);
                    refreshIfOpen(player);
                }
            }
        }
    }

    private boolean isMainPage(Player player) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof Holder holder
                && holder.page == Page.MAIN;
    }

    private static Rps handAtSlot(int slot) {
        for (int i = 0; i < SLOT_TECH.length; i++) {
            if (SLOT_TECH[i] == slot) {
                return List.of(Rps.GU, Rps.CHOKI, Rps.PA).get(i);
            }
        }
        return null;
    }

    public void rename(Player player, String name) {
        Entity entity = petOf(player);
        if (entity == null) {
            player.sendMessage(Component.text("先に魚を右クリックして選んでください。", NamedTextColor.RED));
            return;
        }
        renameDialog.apply(player, entity, name);
    }

    public void close(Player player) {
        openMenus.remove(player.getUniqueId());
        releaseConfirm.remove(player.getUniqueId());
    }

    public void closeMenuOnly(Player player) {
        openMenus.remove(player.getUniqueId());
        releaseConfirm.remove(player.getUniqueId());
    }

    private boolean isConfirmingRelease(UUID viewer) {
        if (viewer == null) {
            return false;
        }
        Long until = releaseConfirm.get(viewer);
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            releaseConfirm.remove(viewer);
            return false;
        }
        return true;
    }

    public boolean ownsMenu(Inventory inventory) {
        return inventory.getHolder() instanceof Holder;
    }

    private void untrain(Player player, Entity entity, Rps hand) {
        Fish fish = FishData.read(entity);
        if (fish == null) {
            return;
        }
        Technique technique = fish.techniqueFor(hand);
        if (technique == null) {
            return;
        }
        Fish updated = fish.withoutTechnique(hand);
        FishData.write(entity, updated);
        player.sendMessage(Component.text(hand.emoji() + technique.name() + " を外しました。",
                NamedTextColor.GRAY));
    }

    public void listTechniques(Player player) {
        Entity entity = petOf(player);
        if (entity == null) {
            player.sendMessage(Component.text("先に魚を右クリックして選んでください。", NamedTextColor.RED));
            return;
        }
        Fish fish = FishData.read(entity);
        if (fish == null) {
            return;
        }
        player.sendMessage(Component.text("── 装備中の技 ──", NamedTextColor.AQUA));
        if (fish.techniques().isEmpty()) {
            player.sendMessage(Component.text("なし（技カードを持って魚を右クリックで装備）", NamedTextColor.GRAY));
            return;
        }
        for (var entry : fish.moves().entrySet()) {
            Technique technique = entry.getValue();
            player.sendMessage(Component.text()
                    .append(Component.text(entry.getKey().emoji() + " ", NamedTextColor.WHITE))
                    .append(Component.text(technique.name(), technique.color()))
                    .append(Component.text(" x" + String.format("%.2f", technique.bonus()),
                            NamedTextColor.DARK_GRAY))
                    .build());
        }
    }

    public boolean equipTechnique(Player player, Entity entity, Technique technique) {
        Fish fish = FishData.read(entity);
        if (fish == null) {
            return false;
        }
        if (fish.hasTechnique(technique.id())) {
            player.sendMessage(Component.text("その技はすでに装備しています。", NamedTextColor.YELLOW));
            return false;
        }
        Rps hand = technique.hand();
        Technique equipped = fish.techniqueFor(hand);
        if (equipped != null) {
            player.sendMessage(Component.text(hand.emoji() + " にはすでに「" + equipped.name()
                    + "」があります。先に外してください。", NamedTextColor.RED));
            return false;
        }
        entity.getPersistentDataContainer().set(FishKeys.TECHNIQUES, PersistentDataType.STRING,
                FishItems.serializeTechniques(fish.withTechnique(hand, technique.id())));
        entity.customName(FishItems.name(fish.withTechnique(hand, technique.id())));
        entity.setCustomNameVisible(true);
        player.sendMessage(Component.text("「" + technique.name() + "」を " + hand.emoji() + " に覚えさせました。",
                technique.color()));
        refreshIfOpen(player);
        return true;
    }

    public void untrain(Player player, Rps hand) {
        Entity entity = petOf(player);
        if (entity == null) {
            return;
        }
        untrain(player, entity, hand);
    }

    public void untrain(Player player, String handName, Runnable onFound) {
        Rps hand = Rps.parse(handName);
        if (hand == null) {
            return;
        }
        untrain(player, hand);
        if (onFound != null) {
            onFound.run();
        }
    }

    public void toggleFollow(Player player) {
        Entity entity = petOf(player);
        if (entity == null) {
            player.sendMessage(Component.text("先に魚を右クリックして選んでください。", NamedTextColor.RED));
            return;
        }
        boolean follow = motion.toggleFollow(entity);
        player.sendMessage(Component.text(follow ? "追従するようにしました。" : "その場に留まるようにしました。",
                NamedTextColor.YELLOW));
    }

    public void collect(Player player) {
        Entity entity = petOf(player);
        if (entity == null) {
            player.sendMessage(Component.text("先に魚を右クリックして選んでください。", NamedTextColor.RED));
            return;
        }
        CaptureListener.collect(player, entity, motion);
        close(player);
    }

    public void remove(Player player) {
        Entity entity = petOf(player);
        if (entity == null) {
            player.sendMessage(Component.text("先に魚を右クリックして選んでください。", NamedTextColor.RED));
            return;
        }
        motion.untrack(entity);
        entity.remove();
        close(player);
        player.sendMessage(Component.text("魚を消しました。", NamedTextColor.GRAY));
    }

    private Entity entityOf(Player player) {
        UUID entityId = openMenus.get(player.getUniqueId());
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                return entity;
            }
        }
        return petOf(player);
    }

    private Entity petOf(Player player) {
        UUID id = lastPet.get(player.getUniqueId());
        if (id == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity == null || !entity.isValid()) {
            lastPet.remove(player.getUniqueId());
            return null;
        }
        return entity;
    }

    private static boolean follows(Entity entity) {
        Byte value = entity.getPersistentDataContainer().get(FishKeys.FOLLOW, PersistentDataType.BYTE);
        return value == null || value != 0;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private enum Page {
        MAIN,
        STATS
    }

    private static final class Holder implements org.bukkit.inventory.InventoryHolder {
        private final Page page;
        private final UUID viewer;
        private Inventory inventory;

        private Holder(Page page, UUID viewer) {
            this.page = page;
            this.viewer = viewer;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
