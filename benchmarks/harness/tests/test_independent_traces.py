"""Generated physical fork timings and an oracle that reassigns labels to raw slots."""

import itertools
import math
import random
import unittest

from harness.analysis import ALPHA, SHIFTS, log_effect, minimum_detectable_effects, percent, sign_p


def oracle_p(groups, observed):
    # Compute geometric mean ratios directly from the four physical slots.
    choices = [(math.sqrt((b*c)/(a*d)), math.sqrt((a*d)/(b*c))) for a, b, c, d in groups]

    def magnitude(ratios):
        return abs(math.log(math.prod(ratios)))

    threshold = magnitude(pair[side] for pair, side in zip(choices, observed))
    extreme = sum(magnitude(assignment) >= threshold - 1e-12 for assignment in itertools.product(*choices))
    return extreme / 2 ** len(groups)


def simulate(noise, budgets, draws):
    # Bootstrap whole A/A block magnitudes. Independent signs reflect randomized assignment.
    rng = random.Random(405811)
    report = []
    for n in budgets:
        detected = [0] * len(SHIFTS)
        for _ in range(draws):
            sample = [rng.choice(noise) * rng.choice((-1, 1)) for _ in range(n)]
            for i, shift in enumerate(SHIFTS):
                delta = math.log1p(shift / 100)
                ds = [v + delta for v in sample]
                detected[i] += sign_p(ds) <= ALPHA
        report.append(dict(blocks=n, draws=draws, detection_probability={
            str(shift): count / draws for shift, count in zip(SHIFTS, detected)}))
    return report


def independent_mde_check():
    def traces(rng, n, shift):
        groups, orientations = [], []
        for _ in range(n):
            side = rng.randrange(2)
            order = ["ABBA", "BAAB"][side]
            groups.append([200 * rng.lognormvariate(0, .01) * (1 + shift if s == "B" else 1)
                           for s in order])
            orientations.append(side)
        return groups, orientations

    groups, sides = traces(random.Random(912), 256, 0)
    pilot = [abs(log_effect(xs, ["ABBA", "BAAB"][s])) for xs, s in zip(groups, sides)]
    shift = minimum_detectable_effects(pilot, [8], 1500)[8]["mde_slowdown_pct"] / 100
    rng = random.Random(800721)
    hits = sum(oracle_p(*traces(rng, 8, shift)) <= .05 for _ in range(3000))
    return dict(calibration_blocks=256, validation_experiments=3000, blocks_per_experiment=8,
                per_fork_log_noise_sd=.01, target_power=.8, mde_pct=100 * shift,
                independent_oracle_detections=hits, observed_power=hits / 3000)


class IndependentTracesTest(unittest.TestCase):
    def test_known_effect_with_random_block_speeds_and_local_log_drift(self):
        rng = random.Random(421)
        for true_ratio in (0.8, 0.98, 1.0, 1.002, 1.02, 1.4):
            for _ in range(100):
                base, drift = rng.uniform(20, 1500), rng.uniform(0.9, 1.1)
                order = rng.choice(["ABBA", "BAAB"])
                times = [base * drift**i * (true_ratio if side == "B" else 1)
                         for i, side in enumerate(order)]
                self.assertAlmostEqual(percent(log_effect(times, order)), 100 * (true_ratio-1), places=9)

    def test_exact_test_agrees_with_raw_slot_reassignment_even_with_spikes(self):
        rng = random.Random(7291)
        for n in (4, 6, 8, 10):
            for _ in range(25):
                groups, orientations, effects = [], [], []
                for _ in range(n):
                    base = rng.lognormvariate(4, .25)
                    slots = [base * rng.lognormvariate(0, .015) for _ in range(4)]
                    if rng.random() < .3:
                        slots[rng.randrange(4)] *= 1.3
                    side = rng.randrange(2)
                    groups.append(slots)
                    orientations.append(side)
                    effects.append(log_effect(slots, ["ABBA", "BAAB"][side]))
                self.assertEqual(sign_p(effects), oracle_p(groups, orientations))

    def test_single_slot_spike_survives_balancing_and_reverses_with_assignment(self):
        timings = [100, 100, 144, 100]
        self.assertAlmostEqual(percent(log_effect(timings, "ABBA")), 20)
        self.assertAlmostEqual(percent(log_effect(timings, "BAAB")), 100 * (1 / 1.2 - 1))

    def test_wall_time_drift_cancels_only_when_slot_centers_balance(self):
        for times, expected in [([0, 1, 2, 3], 0), ([0, 1, 2, 6], -1.5 * math.log(1.01))]:
            timings = [100 * 1.01**t for t in times]
            self.assertAlmostEqual(log_effect(timings, "ABBA"), expected)

    def test_mde_grid_matches_direct_repeated_testing(self):
        noise = [.002, .005, .015, .045, .1]
        budgets = [4, 6, 8]
        mde = minimum_detectable_effects(noise, budgets, 300)
        direct = simulate(noise, budgets, 300)
        self.assertIsNone(mde[4]["mde_slowdown_pct"])
        for forecast in direct:
            row = mde[forecast["blocks"]]
            self.assertEqual(row["power_at_grid_points"], forecast["detection_probability"])
        self.assertGreater(mde[8]["mde_slowdown_pct"], 0)

    def test_calibrated_mde_on_new_draws_from_known_noise_model(self):
        actual = independent_mde_check()["observed_power"]
        self.assertGreater(actual, 0.74)
        self.assertLess(actual, 0.91)


if __name__ == "__main__":
    unittest.main()
