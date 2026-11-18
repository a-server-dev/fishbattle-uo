package dev.taikin.mcplugins.fishbattle;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.TropicalFish;

public record FishSpecies(String key, String name, int hp, int power, int technique) {

    private static final Map<String, FishSpecies> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, FishSpecies> REGISTRY = new LinkedHashMap<>();
    private static int referenceTotal = 150;
    private static double baseExpMultiplier = 0.5;
    private static double expPerPoint = 0.08;
    private static double minExpMultiplier = 0.45;

    static {
        register("cod", "タラ", 60, 45, 45);
        register("salmon", "サケ", 45, 60, 45);
        register("pufferfish", "フグ", 65, 55, 30);
        register("tadpole", "オタマジャクシ", 35, 40, 70);

        pattern(TropicalFish.Pattern.KOB, "コブダイ", 55, 55, 50);
        pattern(TropicalFish.Pattern.SUNSTREAK, "スジモヨウ", 50, 55, 55);
        pattern(TropicalFish.Pattern.SNOOPER, "ハナビレ", 55, 45, 60);
        pattern(TropicalFish.Pattern.DASHER, "セイブン", 40, 60, 60);
        pattern(TropicalFish.Pattern.BRINELY, "ブリナ", 65, 45, 50);
        pattern(TropicalFish.Pattern.SPOTTY, "スポッティ", 60, 50, 50);
        pattern(TropicalFish.Pattern.FLOPPER, "フロッパー", 70, 50, 40);
        pattern(TropicalFish.Pattern.STRIPEY, "シマシマ", 45, 60, 55);
        pattern(TropicalFish.Pattern.GLITTER, "キラキラ", 50, 50, 60);
        pattern(TropicalFish.Pattern.BLOCKFISH, "ブロック", 75, 55, 30);
        pattern(TropicalFish.Pattern.BETTY, "ベティ", 55, 65, 40);
        pattern(TropicalFish.Pattern.CLAYFISH, "クレイ", 65, 55, 40);

        axolotl(Axolotl.Variant.LUCY, "ルーシー", 55, 50, 50);
        axolotl(Axolotl.Variant.WILD, "ワイルド", 55, 55, 45);
        axolotl(Axolotl.Variant.GOLD, "ゴールド", 55, 60, 40);
        axolotl(Axolotl.Variant.CYAN, "シアン", 45, 55, 55);
        axolotl(Axolotl.Variant.BLUE, "ブルー", 50, 45, 60);
    }

    private static void pattern(TropicalFish.Pattern pattern, String name, int hp, int power, int technique) {
        register("tropical:" + pattern.name(), name, hp, power, technique);
    }

    private static void register(String key, String name, int hp, int power, int technique) {
        DEFAULTS.put(key, new FishSpecies(key, name, hp, power, technique));
        REGISTRY.put(key, DEFAULTS.get(key));
    }

    public static void loadConfig(ConfigurationSection section) {
        REGISTRY.clear();
        if (section == null) {
            REGISTRY.putAll(DEFAULTS);
            return;
        }
        for (Map.Entry<String, FishSpecies> entry : DEFAULTS.entrySet()) {
            FishSpecies def = entry.getValue();
            ConfigurationSection sub = section.getConfigurationSection(shortId(entry.getKey()));
            if (sub == null) {
                REGISTRY.put(entry.getKey(), def);
                continue;
            }
            REGISTRY.put(entry.getKey(), new FishSpecies(entry.getKey(), def.name(),
                    Math.max(1, sub.getInt("hp", def.hp())),
                    Math.max(1, sub.getInt("power", def.power())),
                    Math.max(1, sub.getInt("technique", def.technique()))));
        }
    }

    public static void setExpSettings(int reference, double base, double perPoint, double min) {
        referenceTotal = reference;
        baseExpMultiplier = Math.max(0.01, base);
        expPerPoint = perPoint;
        minExpMultiplier = Math.max(0.01, min);
    }

    private static void axolotl(Axolotl.Variant variant, String name, int hp, int power, int technique) {
        register("axolotl:" + variant.name(), name, hp, power, technique);
    }

    public int total() {
        return hp + power + technique;
    }

    public double expMultiplier() {
        return Math.max(minExpMultiplier,
                baseExpMultiplier + expPerPoint * (total() - referenceTotal));
    }

    public static java.util.List<String> allKeys() {
        return new java.util.ArrayList<>(REGISTRY.keySet());
    }

    public static java.util.List<String> allIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (String key : REGISTRY.keySet()) {
            ids.add(shortId(key));
        }
        return ids;
    }

    public static String shortId(String key) {
        int colon = key.indexOf(':');
        return colon >= 0 ? key.substring(colon + 1).toLowerCase(java.util.Locale.ROOT)
                : key.toLowerCase(java.util.Locale.ROOT);
    }

    public static String resolve(String name) {
        String wanted = name.toLowerCase(java.util.Locale.ROOT);
        for (String key : REGISTRY.keySet()) {
            if (key.equalsIgnoreCase(wanted) || shortId(key).equals(wanted)) {
                return key;
            }
            if (REGISTRY.get(key).name().equals(name)) {
                return key;
            }
        }
        return null;
    }

    public static FishSpecies byKey(String key) {
        FishSpecies species = REGISTRY.get(key);
        return species != null ? species : REGISTRY.get("tropical:KOB");
    }

    public static String keyOf(TropicalFish.Pattern pattern) {
        return "tropical:" + pattern.name();
    }

    public static String keyOf(Axolotl.Variant variant) {
        return "axolotl:" + variant.name();
    }

    public static FishType typeOf(Axolotl.Variant variant) {
        return switch (variant) {
            case LUCY, WILD -> FishType.RED;
            case GOLD -> FishType.YELLOW;
            case CYAN, BLUE -> FishType.BLUE;
        };
    }

    public static FishType typeOfKey(String key) {
        return switch (key) {
            case "cod" -> FishType.BLUE;
            case "salmon" -> FishType.RED;
            case "pufferfish" -> FishType.YELLOW;
            case "tadpole" -> FishType.BLUE;
            default -> FishType.BLUE;
        };
    }
}
