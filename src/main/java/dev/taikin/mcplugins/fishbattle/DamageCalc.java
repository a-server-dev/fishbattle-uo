package dev.taikin.mcplugins.fishbattle;

import java.util.concurrent.ThreadLocalRandom;

final class DamageCalc {

    private static double scale = 1.0;

    static void setScale(double value) {
        scale = Math.max(0.05, value);
    }

    static int damage(Fish attacker, double attackerCharge, double typeMultiplier) {
        double chargeFactor = 0.5 + attackerCharge * 2.0;
        double base = attacker.power() / 3.0 + attacker.level();
        double variance = 0.85 + ThreadLocalRandom.current().nextDouble() * 0.15;
        return Math.max(1, (int) Math.round(base * chargeFactor * typeMultiplier * variance * scale));
    }
}
