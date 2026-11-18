package dev.taikin.mcplugins.fishbattle;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;

public record Technique(String id, String name, double bonus, Rps hand, double mashSeconds,
        String description) {

    private static final List<Technique> REGISTRY = new ArrayList<>();

    static {
        add("tackle", "たいあたり", 1.10, Rps.GU, "基本の体当たり");
        add("bite", "かみつき", 1.25, Rps.CHOKI, "鋭い牙で噛みつく");
        add("tail", "しっぽビンタ", 1.20, Rps.CHOKI, "しっぽで張り倒す");
        add("splash", "はねあげ", 1.15, Rps.CHOKI, "水しぶきを上げる");
        add("dash", "とっしん", 1.35, Rps.GU, "勢いよく突進する");
        add("spin", "スピンアタック", 1.30, Rps.GU, "回転しながら体当たり");
        add("bubble", "バブルビーム", 1.45, Rps.PA, "泡を撃ち出す");
        add("wave", "ビッグウェーブ", 1.60, Rps.PA, "大波で押し流す");
        add("thunder", "かみなり", 1.70, Rps.GU, "雷を落とす");
        add("blaze", "ごうかブレイズ", 1.85, Rps.GU, "炎をまとって突撃");
        add("frost", "こおりづめ", 1.55, Rps.CHOKI, "冷気で凍らせる");
        add("poison", "もうどくばり", 1.50, Rps.CHOKI, "毒針を突き刺す");
        addMash("guard", "てっぺき", 1.00, Rps.PA, 1.0, "防御を固め、連打時間が1秒伸びる");
        addMash("heal", "いやしのしずく", 1.05, Rps.PA, 1.0, "連打時間が1秒伸びる");
        addMash("lucky", "ラッキーヒット", 1.40, Rps.PA, 0.5, "運任せ。連打時間が0.5秒伸びる");
        add("finish", "とどめの一撃", 2.00, Rps.GU, "勝負を決める大技");
    }

    private static void add(String id, String name, double bonus, Rps hand, String description) {
        REGISTRY.add(new Technique(id, name, bonus, hand, 0.0, description));
    }

    private static void addMash(String id, String name, double bonus, Rps hand, double mashSeconds,
            String description) {
        REGISTRY.add(new Technique(id, name, bonus, hand, mashSeconds, description));
    }

    public static List<Technique> all() {
        return REGISTRY;
    }

    public static List<Technique> forHand(Rps hand) {
        List<Technique> result = new ArrayList<>();
        for (Technique technique : REGISTRY) {
            if (technique.hand == hand) {
                result.add(technique);
            }
        }
        return result;
    }

    public static Technique byId(String id) {
        for (Technique technique : REGISTRY) {
            if (technique.id().equalsIgnoreCase(id)) {
                return technique;
            }
        }
        return null;
    }

    public NamedTextColor color() {
        if (bonus >= 1.8) {
            return NamedTextColor.DARK_PURPLE;
        }
        if (bonus >= 1.45) {
            return NamedTextColor.LIGHT_PURPLE;
        }
        if (bonus >= 1.25) {
            return NamedTextColor.AQUA;
        }
        return NamedTextColor.GRAY;
    }
}
