package dev.taikin.mcplugins.fishbattle;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.TropicalFish;

public record FishSpecies(String key, String name, int hp, int power, int technique) {

    private static final Map<String, FishSpecies> REGISTRY = new LinkedHashMap<>();

    static {
        REGISTRY.put("cod", new FishSpecies("cod", "タラ", 55, 50, 44));
        REGISTRY.put("salmon", new FishSpecies("salmon", "サケ", 55, 55, 52));
        REGISTRY.put("pufferfish", new FishSpecies("pufferfish", "フグ", 60, 55, 40));
        REGISTRY.put("tadpole", new FishSpecies("tadpole", "オタマジャクシ", 50, 45, 58));

        pattern(TropicalFish.Pattern.KOB, "コブダイ", 55, 50, 44);
        pattern(TropicalFish.Pattern.SUNSTREAK, "スジモヨウ", 50, 55, 54);
        pattern(TropicalFish.Pattern.SNOOPER, "ハナビレ", 50, 50, 48);
        pattern(TropicalFish.Pattern.DASHER, "セイブン", 45, 55, 60);
        pattern(TropicalFish.Pattern.BRINELY, "ブリナ", 55, 50, 44);
        pattern(TropicalFish.Pattern.SPOTTY, "スポッティ", 50, 50, 50);
        pattern(TropicalFish.Pattern.FLOPPER, "フロッパー", 60, 45, 42);
        pattern(TropicalFish.Pattern.STRIPEY, "シマシマ", 50, 55, 52);
        pattern(TropicalFish.Pattern.GLITTER, "キラキラ", 45, 50, 62);
        pattern(TropicalFish.Pattern.BLOCKFISH, "ブロック", 60, 55, 38);
        pattern(TropicalFish.Pattern.BETTY, "ベティ", 50, 60, 50);
        pattern(TropicalFish.Pattern.CLAYFISH, "クレイ", 55, 55, 40);

        axolotl(Axolotl.Variant.LUCY, "ルーシー", 55, 50, 50);
        axolotl(Axolotl.Variant.WILD, "ワイルド", 50, 55, 48);
        axolotl(Axolotl.Variant.GOLD, "ゴールド", 55, 55, 48);
        axolotl(Axolotl.Variant.CYAN, "シアン", 50, 55, 54);
        axolotl(Axolotl.Variant.BLUE, "ブルー", 50, 50, 60);
    }

    private static void pattern(TropicalFish.Pattern pattern, String name, int hp, int power, int technique) {
        REGISTRY.put("tropical:" + pattern.name(),
                new FishSpecies("tropical:" + pattern.name(), name, hp, power, technique));
    }

    private static void axolotl(Axolotl.Variant variant, String name, int hp, int power, int technique) {
        REGISTRY.put("axolotl:" + variant.name(),
                new FishSpecies("axolotl:" + variant.name(), name, hp, power, technique));
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
