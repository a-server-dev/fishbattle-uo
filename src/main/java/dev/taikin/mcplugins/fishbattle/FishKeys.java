package dev.taikin.mcplugins.fishbattle;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class FishKeys {

    public static NamespacedKey CAPTURE;
    public static NamespacedKey ID;
    public static NamespacedKey SPECIES;
    public static NamespacedKey TYPE;
    public static NamespacedKey LEVEL;
    public static NamespacedKey EXP;
    public static NamespacedKey HP;
    public static NamespacedKey ANCHOR_X;
    public static NamespacedKey ANCHOR_Y;
    public static NamespacedKey ANCHOR_Z;
    public static NamespacedKey OWNER;
    public static NamespacedKey NICK;
    public static NamespacedKey FOLLOW;
    public static NamespacedKey TECHNIQUES;
    public static NamespacedKey DEMO;
    public static NamespacedKey CAUGHT;
    public static NamespacedKey PUFF;
    public static NamespacedKey RANK_POWER;
    public static NamespacedKey RANK_HP;
    public static NamespacedKey RANK_TECH;
    public static NamespacedKey DEMO_VILLAGER;
    public static NamespacedKey DEMO_SPECIES;
    public static NamespacedKey DEMO_LEVEL;
    public static NamespacedKey DEMO_TYPE;
    public static NamespacedKey DEMO_RANK;
    public static NamespacedKey DEMO_RANK_HP;
    public static NamespacedKey DEMO_RANK_TECH;

    private FishKeys() {
    }

    public static void init(Plugin plugin) {
        CAPTURE = new NamespacedKey(plugin, "capture_bucket");
        ID = new NamespacedKey(plugin, "fish_id");
        SPECIES = new NamespacedKey(plugin, "species");
        TYPE = new NamespacedKey(plugin, "type");
        LEVEL = new NamespacedKey(plugin, "level");
        EXP = new NamespacedKey(plugin, "exp");
        HP = new NamespacedKey(plugin, "hp");
        ANCHOR_X = new NamespacedKey(plugin, "anchor_x");
        ANCHOR_Y = new NamespacedKey(plugin, "anchor_y");
        ANCHOR_Z = new NamespacedKey(plugin, "anchor_z");
        OWNER = new NamespacedKey(plugin, "owner");
        NICK = new NamespacedKey(plugin, "nick");
        FOLLOW = new NamespacedKey(plugin, "follow");
        TECHNIQUES = new NamespacedKey(plugin, "techniques");
        DEMO = new NamespacedKey(plugin, "demo");
        CAUGHT = new NamespacedKey(plugin, "caught");
        PUFF = new NamespacedKey(plugin, "puff");
        RANK_POWER = new NamespacedKey(plugin, "rank_power");
        RANK_HP = new NamespacedKey(plugin, "rank_hp");
        RANK_TECH = new NamespacedKey(plugin, "rank_tech");
        DEMO_VILLAGER = new NamespacedKey(plugin, "demo_villager");
        DEMO_SPECIES = new NamespacedKey(plugin, "demo_species");
        DEMO_LEVEL = new NamespacedKey(plugin, "demo_level");
        DEMO_TYPE = new NamespacedKey(plugin, "demo_type");
        DEMO_RANK = new NamespacedKey(plugin, "demo_rank");
        DEMO_RANK_HP = new NamespacedKey(plugin, "demo_rank_hp");
        DEMO_RANK_TECH = new NamespacedKey(plugin, "demo_rank_tech");
    }
}
