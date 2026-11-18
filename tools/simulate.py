#!/usr/bin/env python3
"""uo battle simulator: replays the plugin's damage/charge formulas in Python."""

import argparse
import itertools
import math
import random
import sys

try:
    import yaml
except ImportError:
    yaml = None

RANKS = {"S": 1.10, "A": 1.05, "B": 1.00, "C": 0.95}

MASH_SECONDS = 3.0
DECAY_PER_SECOND = 0.06
TIME_STEP = 0.002

TUNING = {
    "tech_div": 700.0,
    "tech_min": 0.03,
    "tech_max": 0.25,
    "power_div": 3.0,
    "level_power": 1.0,
    "damage_scale": 1.0,
    "attack_charge": 2.0,
    "defense_charge": 0.6,
    "hp_per_level": 4,
    "power_per_level": 2,
    "tech_per_level": 1,
}


def load_config(path):
    if yaml is None:
        raise SystemExit("pyyaml is required")
    with open(path) as handle:
        return yaml.safe_load(handle)


def exp_multiplier(total, exp):
    return max(exp.get("min-multiplier", 0.45),
               exp.get("base-multiplier", 0.5) + exp.get("per-point", 0.08)
               * (total - exp.get("reference-total", 150)))


class Fish:
    def __init__(self, name, hp, power, tech, rank_hp=1.0, rank_power=1.0, rank_tech=1.0,
                 level=10):
        self.name = name
        self.hp = max(1, round((hp + (level - 1) * TUNING["hp_per_level"]) * rank_hp))
        self.max_hp = self.hp
        self.power = max(1, round((power + level * TUNING["power_per_level"]) * rank_power))
        self.tech = max(1, round((tech + level * TUNING["tech_per_level"]) * rank_tech))
        self.level = level

    def gain_factor(self):
        return max(TUNING["tech_min"],
                   min(TUNING["tech_max"], self.tech / TUNING["tech_div"]))

    def with_hp(self, hp):
        self.hp = max(0, min(hp, self.max_hp))


def simulate_mash(fish, seconds, cps, charge_start=0.0, gain_mult=1.0):
    charge = charge_start
    t = 0.0
    next_click = 0.03
    interval = 1.0 / cps
    factor = fish.gain_factor() * gain_mult
    while t < seconds:
        charge = max(0.0, charge - DECAY_PER_SECOND * TIME_STEP)
        if t >= next_click:
            charge = min(1.0, charge + factor * (1.0 - charge))
            next_click += interval
        t += TIME_STEP
    return charge


def damage(attacker, defender, attacker_charge, defender_charge, multiplier, rng):
    attack_factor = 0.5 + attacker_charge * TUNING["attack_charge"]
    defense_factor = 1.0 / (1.0 + defender_charge * TUNING["defense_charge"])
    base = attacker.power / TUNING["power_div"] + attacker.level * TUNING["level_power"]
    variance = 0.85 + rng.random() * 0.15
    return max(1, round(base * attack_factor * defense_factor * multiplier * variance
                        * TUNING["damage_scale"]))


RPS = ("GU", "CHOKI", "PA")
BEATS = {"GU": "CHOKI", "CHOKI": "PA", "PA": "GU"}
TYPE_MULT = {True: 1.3, False: 0.75, None: 1.0}


def type_multiplier(attacker_hand, defender_hand):
    if attacker_hand == defender_hand:
        return None
    return TYPE_MULT[BEATS[attacker_hand] == defender_hand]


def battle(a, b, cps, rng, with_types=True):
    rounds = 0
    while a.hp > 0 and b.hp > 0 and rounds < 100:
        rounds += 1
        charge_a = simulate_mash(a, MASH_SECONDS, cps)
        charge_b = simulate_mash(b, MASH_SECONDS, cps)
        hand_a = rng.choice(RPS)
        hand_b = rng.choice(RPS)

        if hand_a == hand_b:
            if charge_a == charge_b:
                continue
            winner_is_a = charge_a > charge_b
            scale = max(0.15, min(1.0, abs(charge_a - charge_b) / 0.15))
        else:
            winner_is_a = BEATS[hand_a] == hand_b
            scale = 1.0

        # only the winner mashes; the loser keeps zero charge
        if winner_is_a:
            attacker, defender, ac = a, b, charge_a
            hand_atk, hand_def = hand_a, hand_b
        else:
            attacker, defender, ac = b, a, charge_b
            hand_atk, hand_def = hand_b, hand_a
        dc = 0.0

        mult = type_multiplier(hand_atk, hand_def) if with_types else 1.0
        if mult is None:
            mult = 1.0
        dmg = round(damage(attacker, defender, ac, dc, mult, rng) * scale)
        defender.with_hp(defender.hp - max(1, dmg))
    if a.hp <= 0 and b.hp <= 0:
        return 0.5
    return 1.0 if b.hp <= 0 else 0.0


RANK_WEIGHTS = [("S", 5), ("A", 20), ("B", 35), ("C", 40)]


def roll_rank(rng):
    total = sum(weight for _, weight in RANK_WEIGHTS)
    pick = rng.uniform(0, total)
    for name, weight in RANK_WEIGHTS:
        pick -= weight
        if pick <= 0:
            return RANKS[name]
    return 1.0


def roll_ranks(mode, rng):
    if mode == "none":
        return (1.0, 1.0, 1.0), (1.0, 1.0, 1.0)
    if mode == "fixed":
        return (RANKS["B"],) * 3, (RANKS["B"],) * 3
    return (tuple(roll_rank(rng) for _ in range(3)),
            tuple(roll_rank(rng) for _ in range(3)))


def pearson(xs, ys):
    n = len(xs)
    if n < 2:
        return 0.0
    mx = sum(xs) / n
    my = sum(ys) / n
    num = sum((x - mx) * (y - my) for x, y in zip(xs, ys))
    dx = math.sqrt(sum((x - mx) ** 2 for x in xs))
    dy = math.sqrt(sum((y - my) ** 2 for y in ys))
    if dx == 0 or dy == 0:
        return 0.0
    return num / (dx * dy)


def spearman(xs, ys):
    def ranks(values):
        order = sorted(range(len(values)), key=lambda i: values[i])
        result = [0.0] * len(values)
        i = 0
        while i < len(order):
            j = i
            while j + 1 < len(order) and values[order[j + 1]] == values[order[i]]:
                j += 1
            avg = (i + j) / 2.0 + 1
            for k in range(i, j + 1):
                result[order[k]] = avg
            i = j + 1
        return result
    return pearson(ranks(xs), ranks(ys))


def print_correlations(rows):
    print("\n相関（勝率との関係）")
    print(f"{'要素':<10}{'Pearson':>10}{'Spearman':>10}")
    rates = [rate for rate, _, _ in rows]
    for label, index in (("体力", 0), ("強さ", 1), ("テクニック", 2), ("合計", 3)):
        values = []
        for _, key, entry in rows:
            values.append(sum([entry["hp"], entry["power"], entry["technique"]][:index + 1])
                          if index == 3 else [entry["hp"], entry["power"], entry["technique"]][index])
        print(f"{label:<10}{pearson(values, rates):>10.2f}{spearman(values, rates):>10.2f}")


def print_matrix(rows):
    print("\n勝率マトリクス（行が列に勝つ確率, %）")
    labels = [entry["name"] for _, _, entry in rows]
    width = max(len(label) for label in labels) + 1
    print(" " * width + "".join(f"{i:>4}" for i in range(1, len(rows) + 1)))
    for i, (_, _, entry) in enumerate(rows):
        print(f"{i + 1:>2} {entry['name']:<{width - 3}}"
              + "".join(f"{MATRIX[i][j]:>4.0f}" for j in range(len(rows))))


MATRIX = []


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="config.yml")
    parser.add_argument("--trials", type=int, default=300)
    parser.add_argument("--cps", type=float, default=10.0)
    parser.add_argument("--level", type=int, default=10)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--no-types", action="store_true")
    parser.add_argument("--matrix", action="store_true")
    parser.add_argument("--rank-mode", choices=("none", "fixed", "random"), default="none",
                        help="none=ランク無し, fixed=全員B(銅), random=捕獲抽選どおり")
    parser.add_argument("--tech-div", type=float)
    parser.add_argument("--tech-max", type=float)
    parser.add_argument("--power-div", type=float)
    parser.add_argument("--level-power", type=float)
    parser.add_argument("--damage-scale", type=float)
    parser.add_argument("--hp-per-level", type=int)
    parser.add_argument("--power-per-level", type=int)
    parser.add_argument("--tech-per-level", type=float)
    parser.add_argument("--quiet", action="store_true")
    parser.add_argument("--attack-charge", type=float)
    parser.add_argument("--defense-charge", type=float)
    args = parser.parse_args()

    for key, attr in (("tech_div", "tech_div"), ("tech_max", "tech_max"),
                      ("power_div", "power_div"), ("level_power", "level_power"),
                      ("damage_scale", "damage_scale"), ("hp_per_level", "hp_per_level"),
                      ("power_per_level", "power_per_level"),
                      ("tech_per_level", "tech_per_level"),
                      ("attack_charge", "attack_charge"),
                      ("defense_charge", "defense_charge")):
        value = getattr(args, key)
        if value is not None:
            TUNING[attr] = value

    data = load_config(args.config)
    species = data.get("species") or {}
    names = []
    for key, entry in species.items():
        if not isinstance(entry, dict) or "hp" not in entry:
            continue
        names.append((key, entry))
    if not names:
        raise SystemExit("no species found in config")

    global MATRIX
    rng = random.Random(args.seed)
    wins = {key: 0.0 for key, _ in names}
    games = {key: 0 for key, _ in names}
    index = {key: i for i, (key, _) in enumerate(names)}
    head_to_head = [[0.0] * len(names) for _ in names]
    counts = [[0] * len(names) for _ in names]

    for (key_a, a), (key_b, b) in itertools.combinations(names, 2):
        ia, ib = index[key_a], index[key_b]
        for _ in range(args.trials):
            ra, rb = roll_ranks(args.rank_mode, rng)
            fish_a = Fish(a["name"], a["hp"], a["power"], a["technique"],
                          rank_hp=ra[0], rank_power=ra[1], rank_tech=ra[2], level=args.level)
            fish_b = Fish(b["name"], b["hp"], b["power"], b["technique"],
                          rank_hp=rb[0], rank_power=rb[1], rank_tech=rb[2], level=args.level)
            result = battle(fish_a, fish_b, args.cps, rng, not args.no_types)
            wins[key_a] += result
            wins[key_b] += 1.0 - result
            head_to_head[ia][ib] += result
            head_to_head[ib][ia] += 1.0 - result
            counts[ia][ib] += 1
            counts[ib][ia] += 1
        games[key_a] += args.trials
        games[key_b] += args.trials

    rows = []
    for key, entry in names:
        rate = wins[key] / games[key] if games[key] else 0.0
        rows.append((rate, key, entry))
    rows.sort(reverse=True)

    MATRIX = [[(head_to_head[i][j] / counts[i][j] * 100) if counts[i][j] else 50.0
               for j in range(len(names))] for i in range(len(names))]
    order = []
    for _, key, _ in rows:
        order.append(index[key])
    MATRIX = [[MATRIX[i][j] for j in order] for i in order]

    if args.quiet:
        rates = [rate for rate, _, _ in rows]
        print(f"最大 {max(rates) * 100:.1f}% 最小 {min(rates) * 100:.1f}% "
              f"ばらつき {math.sqrt(sum((r - 0.5) ** 2 for r in rates) / len(rates)) * 100:.1f}pt")
        print_correlations(rows)
        return

    print(f"{'種':<12}{'勝率':>7}{'体力':>5}{'強さ':>5}{'テク':>5}{'合計':>5}")
    for rate, key, entry in rows:
        print(f"{entry['name']:<12}{rate * 100:>6.1f}%{entry['hp']:>5}{entry['power']:>5}"
              f"{entry['technique']:>5}{entry['hp'] + entry['power'] + entry['technique']:>5}")

    rates = [rate for rate, _, _ in rows]
    print(f"\n最大 {max(rates) * 100:.1f}% / 最小 {min(rates) * 100:.1f}% / "
          f"ばらつき {math.sqrt(sum((r - 0.5) ** 2 for r in rates) / len(rates)) * 100:.1f}pt")

    print_correlations(rows)
    if args.matrix:
        print_matrix(rows)


if __name__ == "__main__":
    main()
