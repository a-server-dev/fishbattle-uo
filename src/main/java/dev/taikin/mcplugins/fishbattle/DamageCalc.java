package dev.taikin.mcplugins.fishbattle;

import java.util.concurrent.ThreadLocalRandom;

final class DamageCalc {

    static final double DEFENSE_CHARGE = 0.6;
    private static double scale = 1.0;

    static void setScale(double value) {
        scale = Math.max(0.05, value);
    }

    private DamageCalc() {
    }

    static int damage(Fish attacker, Fish defender, double attackerCharge, double defenderCharge,
            double typeMultiplier) {
        double attackFactor = 0.5 + attackerCharge * 2.0;
        double defenseFactor = 1.0 / (1.0 + defenderCharge * DEFENSE_CHARGE);
        double base = attacker.power() / 3.0 + attacker.level();
        double variance = 0.85 + ThreadLocalRandom.current().nextDouble() * 0.15;
        return Math.max(1,
                (int) Math.round(base * attackFactor * defenseFactor * typeMultiplier * variance * scale));
    }

    static Technique bestTechnique(Fish fish, Rps hand) {
        return fish.techniqueFor(hand);
    }
}
