package dev.taikin.mcplugins.fishbattle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;

public record Technique(String id, String name, double bonus, Rps hand, double mashSeconds, Rarity rarity,
        double chargeStart, double chargeGain, double drain, double pierce, double heal, boolean ignoreType,
        double tiePower, int flatDamage, String description) {

    private static final Map<String, Technique> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, Technique> REGISTRY = new LinkedHashMap<>();

    static {
        add("tackle", "たいあたり", 1.10, Rps.GU, 0.0, Rarity.COMMON, "基本の体当たり");
        addEffect("dash", "とっしん", 1.30, Rps.GU, 0.0, Rarity.UNCOMMON, 0.15, 1.0, 0, 0, 0, false, 1.0, 0,
                "開始気合+15%で飛び出す");
        addEffect("spin", "スピンアタック", 1.30, Rps.GU, 0.0, Rarity.UNCOMMON, 0, 1.0, 0, 0, 0, true, 1.0, 0,
                "属性不利を打ち消す");
        addEffect("thunder", "かみなり", 1.65, Rps.GU, 0.0, Rarity.EPIC, 0, 1.0, 0, 0.5, 0, false, 1.0, 0,
                "相手の気合防御を50%貫通");
        addEffect("blaze", "ごうかブレイズ", 1.80, Rps.GU, 0.0, Rarity.EPIC, 0, 1.0, 0.15, 0, 0, false, 1.0, 0,
                "相手の気合を15%吸い取る");
        addEffect("finish", "とどめの一撃", 1.90, Rps.GU, 0.0, Rarity.LEGENDARY, 0, 1.0, 0, 0.35, 0, false, 1.0, 5,
                "防御貫通35%と固定+5ダメージ");

        addEffect("bite", "かみつき", 1.25, Rps.CHOKI, 0.0, Rarity.UNCOMMON, 0, 1.0, 0, 0, 0.20, false, 1.0, 0,
                "与ダメージの20%を回復");
        addEffect("tail", "しっぽビンタ", 1.20, Rps.CHOKI, 0.0, Rarity.COMMON, 0, 1.0, 0.08, 0, 0, false, 1.0, 0,
                "相手の気合を8%吸い取る");
        addEffect("splash", "はねあげ", 1.15, Rps.CHOKI, 0.0, Rarity.COMMON, 0, 1.5, 0, 0, 0, false, 1.0, 0,
                "連打の伸びが1.5倍");
        addEffect("frost", "こおりづめ", 1.50, Rps.CHOKI, 0.0, Rarity.RARE, 0, 1.0, 0.18, 0, 0, false, 1.0, 0,
                "相手の気合を18%凍らせる");
        addEffect("poison", "もうどくばり", 1.45, Rps.CHOKI, 0.0, Rarity.RARE, 0, 1.0, 0, 0, 0.15, false, 1.0, 2,
                "固定+2ダメージと15%回復");

        addEffect("bubble", "バブルビーム", 1.40, Rps.PA, 0.0, Rarity.RARE, 0, 1.0, 0, 0, 0, true, 1.0, 0,
                "属性不利を打ち消す");
        addEffect("wave", "ビッグウェーブ", 1.55, Rps.PA, 0.0, Rarity.RARE, 0, 1.0, 0.12, 0, 0, false, 1.0, 0,
                "相手の気合を12%押し流す");
        addEffect("guard", "てっぺき", 1.00, Rps.PA, 1.0, Rarity.UNCOMMON, 0.20, 1.0, 0, 0, 0, false, 1.0, 0,
                "開始気合+20%と連打時間+1秒");
        addEffect("heal", "いやしのしずく", 1.05, Rps.PA, 1.0, Rarity.UNCOMMON, 0, 1.0, 0, 0, 0.30, false, 1.0, 0,
                "与ダメージの30%回復と連打時間+1秒");
        addEffect("lucky", "ラッキーヒット", 1.40, Rps.PA, 0.5, Rarity.RARE, 0, 1.0, 0, 0, 0, false, 1.6, 0,
                "あいこの押し込みが1.6倍、連打+0.5秒");
    }

    private static void add(String id, String name, double bonus, Rps hand, double mashSeconds,
            Rarity rarity, String description) {
        addEffect(id, name, bonus, hand, mashSeconds, rarity, 0, 1.0, 0, 0, 0, false, 1.0, 0, description);
    }

    private static void addEffect(String id, String name, double bonus, Rps hand, double mashSeconds,
            Rarity rarity, double chargeStart, double chargeGain, double drain, double pierce, double heal,
            boolean ignoreType, double tiePower, int flatDamage, String description) {
        Technique technique = new Technique(id, name, bonus, hand, mashSeconds, rarity, chargeStart, chargeGain,
                drain, pierce, heal, ignoreType, tiePower, flatDamage, description);
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
                    clamp(sub.getDouble("charge-start", def.chargeStart())),
                    Math.max(0.1, sub.getDouble("charge-gain", def.chargeGain())),
                    clamp(sub.getDouble("drain", def.drain())),
                    clamp(sub.getDouble("pierce", def.pierce())),
                    clamp(sub.getDouble("heal", def.heal())),
                    sub.getBoolean("ignore-type", def.ignoreType()),
                    Math.max(0.1, sub.getDouble("tie-power", def.tiePower())),
                    Math.max(0, sub.getInt("flat-damage", def.flatDamage())),
                    sub.getString("description", def.description())));
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
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

    public List<String> effectLines() {
        List<String> lines = new ArrayList<>();
        lines.add("威力 x" + String.format("%.2f", bonus));
        if (mashSeconds > 0) {
            lines.add("連打時間 +" + String.format("%.1f", mashSeconds) + "秒");
        }
        if (chargeStart > 0) {
            lines.add("開始気合 +" + Math.round(chargeStart * 100) + "%");
        }
        if (chargeGain > 1.0) {
            lines.add("気合の伸び x" + String.format("%.1f", chargeGain));
        }
        if (drain > 0) {
            lines.add("相手の気合 -" + Math.round(drain * 100) + "%");
        }
        if (pierce > 0) {
            lines.add("気合防御を" + Math.round(pierce * 100) + "%貫通");
        }
        if (heal > 0) {
            lines.add("与ダメージの" + Math.round(heal * 100) + "%回復");
        }
        if (ignoreType) {
            lines.add("属性不利を無効化");
        }
        if (tiePower > 1.0) {
            lines.add("あいこの押し込み x" + String.format("%.1f", tiePower));
        }
        if (flatDamage > 0) {
            lines.add("固定 +" + flatDamage + " ダメージ");
        }
        return lines;
    }
}
