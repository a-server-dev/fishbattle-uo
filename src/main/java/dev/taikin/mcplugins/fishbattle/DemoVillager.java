package dev.taikin.mcplugins.fishbattle;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;

public final class DemoVillager {

    private DemoVillager() {
    }

    public record Settings(String speciesKey, int level, FishType type, Rank powerRank,
            Rank hpRank, Rank techRank) {

        public Fish toFish() {
            FishSpecies species = FishSpecies.byKey(speciesKey);
            return new Fish(UUID.randomUUID().toString(), type, speciesKey, level, 0,
                    Fish.maxHp(species, level, hpRank), "", java.util.Map.of(),
                    powerRank, hpRank, techRank);
        }

        public Settings withPowerRank(Rank rank) {
            return new Settings(speciesKey, level, type, rank, hpRank, techRank);
        }

        public Settings withHpRank(Rank rank) {
            return new Settings(speciesKey, level, type, powerRank, rank, techRank);
        }

        public Settings withTechRank(Rank rank) {
            return new Settings(speciesKey, level, type, powerRank, hpRank, rank);
        }
    }

    public static boolean isDemoVillager(Entity entity) {
        return entity instanceof Villager
                && entity.getPersistentDataContainer().has(FishKeys.DEMO_VILLAGER, PersistentDataType.BYTE);
    }

    public static void mark(Villager villager, Settings settings) {
        villager.getPersistentDataContainer().set(FishKeys.DEMO_VILLAGER, PersistentDataType.BYTE, (byte) 1);
        write(villager, settings);
        villager.customName(Component.text("デモの村人", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
    }

    public static void write(Entity villager, Settings settings) {
        var pdc = villager.getPersistentDataContainer();
        pdc.set(FishKeys.DEMO_SPECIES, PersistentDataType.STRING, settings.speciesKey());
        pdc.set(FishKeys.DEMO_LEVEL, PersistentDataType.INTEGER, settings.level());
        pdc.set(FishKeys.DEMO_TYPE, PersistentDataType.STRING, settings.type().name());
        pdc.set(FishKeys.DEMO_RANK, PersistentDataType.STRING, settings.powerRank().name());
        pdc.set(FishKeys.DEMO_RANK_HP, PersistentDataType.STRING, settings.hpRank().name());
        pdc.set(FishKeys.DEMO_RANK_TECH, PersistentDataType.STRING, settings.techRank().name());
    }

    public static Settings read(Entity villager) {
        var pdc = villager.getPersistentDataContainer();
        String species = pdc.get(FishKeys.DEMO_SPECIES, PersistentDataType.STRING);
        Integer level = pdc.get(FishKeys.DEMO_LEVEL, PersistentDataType.INTEGER);
        String typeName = pdc.get(FishKeys.DEMO_TYPE, PersistentDataType.STRING);
        String rankName = pdc.get(FishKeys.DEMO_RANK, PersistentDataType.STRING);
        FishType type;
        try {
            type = typeName == null ? FishType.BLUE : FishType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            type = FishType.BLUE;
        }
        String hpRankName = pdc.get(FishKeys.DEMO_RANK_HP, PersistentDataType.STRING);
        String techRankName = pdc.get(FishKeys.DEMO_RANK_TECH, PersistentDataType.STRING);
        Rank rank = Rank.parse(rankName);
        Rank hpRank = hpRankName == null ? rank : Rank.parse(hpRankName);
        Rank techRank = techRankName == null ? rank : Rank.parse(techRankName);
        String key = species == null ? "cod" : species;
        int lv = level == null ? 5 : Math.max(1, Math.min(Fish.MAX_LEVEL, level));
        return new Settings(key, lv, type, rank, hpRank, techRank);
    }

    public static Settings nextSpecies(Settings settings, int direction) {
        List<String> keys = FishSpecies.allKeys();
        int index = keys.indexOf(settings.speciesKey());
        if (index < 0) {
            index = 0;
        }
        index = Math.floorMod(index + direction, keys.size());
        return new Settings(keys.get(index), settings.level(), settings.type(),
                settings.powerRank(), settings.hpRank(), settings.techRank());
    }

    public static Settings shiftLevel(Settings settings, int direction) {
        int level = Math.max(1, Math.min(Fish.MAX_LEVEL, settings.level() + direction));
        return new Settings(settings.speciesKey(), level, settings.type(),
                settings.powerRank(), settings.hpRank(), settings.techRank());
    }

    public static Settings cycleType(Settings settings, int direction) {
        FishType[] values = FishType.values();
        int index = Math.floorMod(settings.type().ordinal() + direction, values.length);
        return new Settings(settings.speciesKey(), settings.level(), values[index],
                settings.powerRank(), settings.hpRank(), settings.techRank());
    }

    public static Settings cyclePowerRank(Settings settings, int direction) {
        return settings.withPowerRank(nextRank(settings.powerRank(), direction));
    }

    public static Settings cycleHpRank(Settings settings, int direction) {
        return settings.withHpRank(nextRank(settings.hpRank(), direction));
    }

    public static Settings cycleTechRank(Settings settings, int direction) {
        return settings.withTechRank(nextRank(settings.techRank(), direction));
    }

    private static Rank nextRank(Rank current, int direction) {
        Rank[] values = Rank.values();
        int index = Math.floorMod(current.ordinal() + direction, values.length);
        return values[index];
    }
}
