package dev.taikin.mcplugins.fishbattle;

import java.util.LinkedHashMap;
import java.util.Map;

public record Fish(String id, FishType type, String speciesKey, int level, int exp, int hp, String nick,
        Map<Rps, String> techniques) {

    public Fish {
        if (techniques == null || techniques.isEmpty()) {
            techniques = Map.of();
        } else {
            techniques = Map.copyOf(techniques);
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
        return new Fish(this.id, this.type, this.speciesKey, this.level, this.exp, this.hp, this.nick, next);
    }

    public Fish withoutTechnique(Rps hand) {
        return withTechnique(hand, null);
    }

    public Fish withTechniques(Map<Rps, String> next) {
        return new Fish(this.id, this.type, this.speciesKey, this.level, this.exp, this.hp, this.nick, next);
    }

    public String displayName() {
        return nick == null || nick.isBlank() ? species().name() : nick;
    }

    public FishSpecies species() {
        return FishSpecies.byKey(speciesKey);
    }

    public int maxHp() {
        return maxHp(species(), level);
    }

    public int power() {
        return species().power() + level;
    }

    public int technique() {
        return species().technique() + level / 2;
    }

    public int nextExp() {
        return level * 20;
    }

    public Fish withHp(int newHp) {
        return new Fish(this.id, this.type, this.speciesKey, this.level, this.exp,
                Math.max(0, Math.min(newHp, maxHp())), this.nick, this.techniques);
    }

    public static final int MAX_LEVEL = 10;

    public Fish gainExp(int amount) {
        int newLevel = level;
        int newExp = exp + amount;
        while (newLevel < MAX_LEVEL && newExp >= newLevel * 20) {
            newExp -= newLevel * 20;
            newLevel++;
        }
        if (newLevel >= MAX_LEVEL) {
            newExp = 0;
        }
        int newHp = newLevel > level ? maxHp(species(), newLevel) : hp;
        return new Fish(this.id, this.type, this.speciesKey, newLevel, newExp, newHp, this.nick, this.techniques);
    }

    public static int maxHp(FishSpecies species, int level) {
        return species.hp() + (level - 1) * 2;
    }
}
