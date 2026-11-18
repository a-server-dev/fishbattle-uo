package dev.taikin.mcplugins.fishbattle;

import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum Rank {
    DIAMOND("ダイヤ", "S", 1.15, NamedTextColor.AQUA, Material.DIAMOND),
    GOLD("金", "A", 1.08, NamedTextColor.GOLD, Material.GOLD_INGOT),
    COPPER("銅", "B", 1.00, NamedTextColor.YELLOW, Material.COPPER_INGOT),
    IRON("鉄", "C", 0.93, NamedTextColor.GRAY, Material.IRON_INGOT);

    private final String label;
    private final String grade;
    private final double multiplier;
    private final NamedTextColor color;
    private final Material icon;

    Rank(String label, String grade, double multiplier, NamedTextColor color, Material icon) {
        this.label = label;
        this.grade = grade;
        this.multiplier = multiplier;
        this.color = color;
        this.icon = icon;
    }

    public String label() {
        return label;
    }

    public String grade() {
        return grade;
    }

    public String display() {
        return label + "(" + grade + ")";
    }

    public double multiplier() {
        return multiplier;
    }

    public NamedTextColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    public static Rank roll(int diamond, int gold, int copper) {
        int total = diamond + gold + copper + 50;
        int pick = ThreadLocalRandom.current().nextInt(total);
        if (pick < diamond) {
            return DIAMOND;
        }
        if (pick < diamond + gold) {
            return GOLD;
        }
        if (pick < diamond + gold + copper) {
            return COPPER;
        }
        return IRON;
    }

    public static Rank parse(String raw) {
        if (raw == null) {
            return COPPER;
        }
        try {
            return valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return COPPER;
        }
    }
}
