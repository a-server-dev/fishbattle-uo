package dev.taikin.mcplugins.fishbattle;

import java.util.LinkedHashMap;
import java.util.Map;

public record Fish(String id, FishType type, String speciesKey, int level, int exp, int hp, String nick,
        Map<Rps, String> techniques, Rank powerRank, Rank hpRank, Rank techRank) {

    public Fish {
        if (techniques == null || techniques.isEmpty()) {
            techniques = Map.of();
        } else {
            techniques = Map.copyOf(techniques);
        }
        if (powerRank == null) {
            powerRank = Rank.COPPER;
        }
        if (hpRank == null) {
            hpRank = Rank.COPPER;
        }
        if (techRank == null) {
            techRank = Rank.COPPER;
        }
    }

    public String techniqueId(Rps hand) {
        return hand == null ? null : techniques.get(hand);
    }

    public Technique techniqueFor(Rps hand) {
        String id = techniqueId(hand);
        return id == null ? null : Technique.byId(id);
    }

    public boolean hasTechnique(String id) {
        return id != null && techniques.containsValue(id);
    }

    public Map<Rps, Technique> moves() {
        Map<Rps, Technique> result = new LinkedHashMap<>();
        for (Rps hand : Rps.values()) {
            Technique technique = techniqueFor(hand);
            if (technique != null) {
                result.put(hand, technique);
            }
        }
        return result;
    }

    public Fish withTechnique(Rps hand, String id) {
        Map<Rps, String> next = new LinkedHashMap<>(techniques);
        if (id == null) {
            next.remove(hand);
        } else {
            next.put(hand, id);
        }
        return new Fish(this.id, this.type, this.speciesKey, this.level, this.exp, this.hp, this.nick, next,
                this.powerRank, this.hpRank, this.techRank);
    }

    public Fish withoutTechnique(Rps hand) {
        return withTechnique(hand, null);
    }

    public Fish withTechniques(Map<Rps, String> next) {
        return new Fish(this.id, this.type, this.speciesKey, this.level, this.exp, this.hp, this.nick, next,
                this.powerRank, this.hpRank, this.techRank);
    }

    public String displayName() {
        return nick == null || nick.isBlank() ? species().name() : nick;
    }

    public FishSpecies species() {
        return FishSpecies.byKey(speciesKey);
    }

    public int maxHp() {
        return scaled(maxHp(species(), level), hpRank);
    }

    public int maxHpAt(int newLevel) {
        return scaled(maxHp(species(), newLevel), hpRank);
    }

    public int power() {
        return scaled(species().power() + level * POWER_PER_LEVEL, powerRank);
    }

    public int technique() {
        return scaled(species().technique() + level * TECH_PER_LEVEL, techRank);
    }

    private static int scaled(int value, Rank rank) {
        return Math.max(1, (int) Math.round(value * (rank == null ? 1.0 : rank.multiplier())));
    }

    public int nextExp() {
        return expNeeded(level, species());
    }

    public static int expNeeded(int level, FishSpecies species) {
        return (int) Math.round(level * 20 * species.expMultiplier());
    }

    public Fish withHp(int newHp) {
        return new Fish(this.id, this.type, this.speciesKey, this.level, this.exp,
                Math.max(0, Math.min(newHp, maxHp())), this.nick, this.techniques,
                this.powerRank, this.hpRank, this.techRank);
    }

    public static final int MAX_LEVEL = 10;

    public Fish gainExp(int amount) {
        FishSpecies species = species();
        int newLevel = level;
        int newExp = exp + amount;
        while (newLevel < MAX_LEVEL && newExp >= expNeeded(newLevel, species)) {
            newExp -= expNeeded(newLevel, species);
            newLevel++;
        }
        if (newLevel >= MAX_LEVEL) {
            newExp = 0;
        }
        int newHp = newLevel > level ? maxHpAt(newLevel) : hp;
        return new Fish(this.id, this.type, this.speciesKey, newLevel, newExp, newHp, this.nick,
                this.techniques, this.powerRank, this.hpRank, this.techRank);
    }

    public static final int POWER_PER_LEVEL = 2;
    public static final int HP_PER_LEVEL = 4;
    public static final int TECH_PER_LEVEL = 1;

    public static int maxHp(FishSpecies species, int level) {
        return species.hp() + (level - 1) * HP_PER_LEVEL;
    }

    public static int maxHp(FishSpecies species, int level, Rank hpRank) {
        return scaled(species.hp() + (level - 1) * HP_PER_LEVEL, hpRank);
    }
}
