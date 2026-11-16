package dev.taikin.mcplugins.fishbattle;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum Rps {
    GU("グー", "✊", FishType.BLUE),
    CHOKI("チョキ", "✌", FishType.RED),
    PA("パー", "✋", FishType.YELLOW);

    private final String label;
    private final String emoji;
    private final FishType type;

    Rps(String label, String emoji, FishType type) {
        this.label = label;
        this.emoji = emoji;
        this.type = type;
    }

    public FishType type() {
        return type;
    }

    public String emoji() {
        return emoji;
    }

    public String label() {
        return label;
    }

    public NamedTextColor color() {
        return type.color();
    }

    public Material icon() {
        return switch (this) {
            case GU -> Material.RAW_IRON;
            case CHOKI -> Material.SHEARS;
            case PA -> Material.PAPER;
        };
    }

    public boolean beats(Rps other) {
        return (this == GU && other == CHOKI)
                || (this == CHOKI && other == PA)
                || (this == PA && other == GU);
    }

    public static Rps parse(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "gu", "g", "rock" -> GU;
            case "choki", "c", "scissors" -> CHOKI;
            case "pa", "p", "paper" -> PA;
            default -> null;
        };
    }
}
