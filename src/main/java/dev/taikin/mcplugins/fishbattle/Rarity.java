package dev.taikin.mcplugins.fishbattle;

import net.kyori.adventure.text.format.NamedTextColor;

public enum Rarity {
    COMMON("コモン", NamedTextColor.GRAY),
    UNCOMMON("アンコモン", NamedTextColor.GREEN),
    RARE("レア", NamedTextColor.AQUA),
    EPIC("エピック", NamedTextColor.LIGHT_PURPLE),
    LEGENDARY("レジェンド", NamedTextColor.GOLD);

    private final String label;
    private final NamedTextColor color;

    Rarity(String label, NamedTextColor color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public NamedTextColor color() {
        return color;
    }

    public static Rarity parse(String raw) {
        if (raw == null) {
            return COMMON;
        }
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
