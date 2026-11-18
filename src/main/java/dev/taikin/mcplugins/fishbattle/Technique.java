package dev.taikin.mcplugins.fishbattle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;

public record Technique(String id, String name, double bonus, Rps hand, double mashSeconds, Rarity rarity,
        String description) {

    private static final Map<String, Technique> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, Technique> REGISTRY = new LinkedHashMap<>();

    static {
        add("tackle", "たいあたり", 1.10, Rps.GU, 0.0, Rarity.COMMON, "基本の体当たり");
        add("bite", "かみつき", 1.25, Rps.CHOKI, 0.0, Rarity.UNCOMMON, "鋭い牙で噛みつく");
        add("tail", "しっぽビンタ", 1.20, Rps.CHOKI, 0.0, Rarity.COMMON, "しっぽで張り倒す");
        add("splash", "はねあげ", 1.15, Rps.CHOKI, 0.0, Rarity.COMMON, "水しぶきを上げる");
        add("dash", "とっしん", 1.35, Rps.GU, 0.0, Rarity.UNCOMMON, "勢いよく突進する");
        add("spin", "スピンアタック", 1.30, Rps.GU, 0.0, Rarity.UNCOMMON, "回転しながら体当たり");
        add("bubble", "バブルビーム", 1.45, Rps.PA, 0.0, Rarity.RARE, "泡を撃ち出す");
        add("wave", "ビッグウェーブ", 1.60, Rps.PA, 0.0, Rarity.RARE, "大波で押し流す");
        add("thunder", "かみなり", 1.70, Rps.GU, 0.0, Rarity.EPIC, "雷を落とす");
        add("blaze", "ごうかブレイズ", 1.85, Rps.GU, 0.0, Rarity.EPIC, "炎をまとって突撃");
        add("frost", "こおりづめ", 1.55, Rps.CHOKI, 0.0, Rarity.RARE, "冷気で凍らせる");
        add("poison", "もうどくばり", 1.50, Rps.CHOKI, 0.0, Rarity.RARE, "毒針を突き刺す");
        add("guard", "てっぺき", 1.00, Rps.PA, 1.0, Rarity.UNCOMMON, "防御を固め、連打時間が1秒伸びる");
        add("heal", "いやしのしずく", 1.05, Rps.PA, 1.0, Rarity.UNCOMMON, "連打時間が1秒伸びる");
        add("lucky", "ラッキーヒット", 1.40, Rps.PA, 0.5, Rarity.RARE, "運任せ。連打時間が0.5秒伸びる");
        add("finish", "とどめの一撃", 2.00, Rps.GU, 0.0, Rarity.LEGENDARY, "勝負を決める大技");
    }

    private static void add(String id, String name, double bonus, Rps hand, double mashSeconds,
            Rarity rarity, String description) {
        Technique technique = new Technique(id, name, bonus, hand, mashSeconds, rarity, description);
        DEFAULTS.put(id, technique);
        REGISTRY.put(id, technique);
    }

    public static void loadConfig(ConfigurationSection section) {
        REGISTRY.clear();
        if (section == null) {
            REGISTRY.putAll(DEFAULTS);
            return;
        }
        for (Map.Entry<String, Technique> entry : DEFAULTS.entrySet()) {
            Technique def = entry.getValue();
            ConfigurationSection sub = section.getConfigurationSection(entry.getKey());
            if (sub == null) {
                REGISTRY.put(entry.getKey(), def);
                continue;
            }
            Rps hand = Rps.parse(sub.getString("hand"));
            REGISTRY.put(entry.getKey(), new Technique(entry.getKey(),
                    sub.getString("name", def.name()),
                    sub.getDouble("power", def.bonus()),
                    hand == null ? def.hand() : hand,
                    Math.max(0.0, sub.getDouble("mash-seconds", def.mashSeconds())),
                    Rarity.parse(sub.getString("rarity", def.rarity().name())),
                    sub.getString("description", def.description())));
        }
    }

    public static List<Technique> all() {
        return new ArrayList<>(REGISTRY.values());
    }

    public static List<Technique> forHand(Rps hand) {
        List<Technique> result = new ArrayList<>();
        for (Technique technique : REGISTRY.values()) {
            if (technique.hand == hand) {
                result.add(technique);
            }
        }
        return result;
    }

    public static Technique byId(String id) {
        if (id == null) {
            return null;
        }
        return REGISTRY.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public NamedTextColor color() {
        return rarity.color();
    }
}
