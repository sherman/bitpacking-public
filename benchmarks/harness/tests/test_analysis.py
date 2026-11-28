import itertools
import math
from pathlib import Path
import random
import sys
import tempfile
import unittest
from unittest.mock import patch

from harness.analysis import ALPHA, log_effect, percent, profile_summary, sign_p, timing_inference, timing_summary
from harness.cpu_policy import run_checked_fork
from harness.runner import make_schedule


class FixedBudgetProtocolTest(unittest.TestCase):
    def test_performance_profile_with_powersave_governor_does_not_start_rerun(self):
        with patch.object(Path, "is_file", return_value=True), patch.object(Path, "read_text", autospec=True,
                side_effect=lambda p: "powersave" if p.name == "scaling_governor" else "performance"), \
                patch("harness.cpu_policy.subprocess.Popen") as launch:
            with self.assertRaisesRegex(RuntimeError, "scaling_governor='powersave'"):
                run_checked_fork(["java"], None, 2)
            launch.assert_not_called()

    def test_mid_fork_governor_change_stops_active_process_group(self):
        governor = iter(["performance", "powersave"])
        def read_setting(path):
            return next(governor) if path.name == "scaling_governor" else "performance"
        with tempfile.TemporaryDirectory() as temp:
            pidfile = Path(temp) / "pids"
            # The JVM and its child are represented by a parent and child sleep process.
            code = ("import os,subprocess,sys,time; "
                    "child=subprocess.Popen(['sleep','15']); "
                    "open(sys.argv[1],'w').write(f'{os.getpid()} {child.pid}'); time.sleep(15)")
            with (Path(temp) / "log").open("w") as log, patch.object(Path, "is_file", return_value=True), \
                    patch.object(Path, "read_text", autospec=True, side_effect=read_setting):
                with self.assertRaisesRegex(RuntimeError, "scaling_governor='powersave'"):
                    run_checked_fork([sys.executable, "-c", code, str(pidfile)], log, 2)
            pids = [int(pid) for pid in pidfile.read_text().split()]
            self.assertEqual(len(pids), 2)
            for pid in pids:
                stat = Path(f"/proc/{pid}/stat")
                if stat.is_file():
                    self.assertEqual(stat.read_text().split(") ")[1][0], "Z")

    def test_post_fork_governor_change_excludes_even_a_short_run(self):
        governor = iter(["performance", "powersave"])
        with tempfile.TemporaryFile(mode="w+") as log, patch.object(Path, "is_file", return_value=True), \
                patch.object(Path, "read_text", autospec=True, side_effect=lambda p:
                             next(governor) if p.name == "scaling_governor" else "performance"):
            with self.assertRaisesRegex(RuntimeError, "scaling_governor='powersave'"):
                run_checked_fork(["true"], log, 2)

    def test_block_ratio_recovers_change_under_multiplicative_drift(self):
        for order in ["ABBA", "BAAB"]:
            scores = [100 * 1.03 ** i * (1.02 if v == "B" else 1) for i, v in enumerate(order)]
            self.assertAlmostEqual(percent(log_effect(scores, order)), 2)
            self.assertAlmostEqual(log_effect(scores, order.translate(str.maketrans("AB", "BA"))),
                                   -log_effect(scores, order))

    def test_exact_p_matches_exhaustive_reference_including_ties(self):
        rng = random.Random(643)
        cases = [[0] * 8, [1, 1, -1, -1], [0, 0, 2, 2]]
        for n in range(1, 13):
            cases.extend([[rng.randrange(-8, 9) for _ in range(n)] for _ in range(5)])
        for ds in cases:
            actual = abs(sum(ds))
            extreme = sum(abs(sum(x * s for x, s in zip(ds, signs))) >= actual
                          for signs in itertools.product((-1, 1), repeat=len(ds)))
            self.assertEqual(sign_p(ds), extreme / 2 ** len(ds), ds)

    def test_smallest_attainable_two_sided_p(self):
        for n in range(1, 17):
            self.assertEqual(sign_p([0.02] * n), 2 / 2 ** n)
        self.assertGreater(sign_p([0.02] * 5), ALPHA)
        self.assertLess(sign_p([0.02] * 6), ALPHA)

    def test_reports_timing_verdict_and_times_per_operation(self):
        for candidate, expected in ((80, "IMPROVED"), (100, "INCONCLUSIVE"), (120, "REGRESSED")):
            group = [dict(label=label, jmh={"primaryMetric": {"score": candidate if label == "B" else 100}})
                     for label in "ABBA"]
            timing = timing_summary([group] * 6)
            self.assertEqual(timing["verdict"], expected)
            self.assertAlmostEqual(timing["baseline_ns_per_op"], 100)
            self.assertAlmostEqual(timing["candidate_ns_per_op"], candidate)
            self.assertEqual(timing_summary([group])["verdict"], "INCONCLUSIVE")

    def test_allocation_difference_accepts_zero_baseline(self):
        group = []
        for label in "BAAB":
            counters = {"cycles": 120 if label == "B" else 100, "instructions": 100,
                        "gc.alloc.rate.norm": 64 if label == "B" else 0}
            group.append(dict(label=label, jmh={"secondaryMetrics": {k: {"score": v} for k, v in counters.items()}}))
        result = profile_summary([group])["counters"]
        self.assertEqual(result["gc.alloc.rate.norm"], {"change": 64, "unit": "B/op"})
        self.assertAlmostEqual(result["cycles"]["change"], 20)
        self.assertEqual(result["instructions"]["change"], 0)
        with self.assertRaises(ValueError):
            profile_summary([group[:-1]])

    def test_timing_interval_inverts_exhaustive_test_including_endpoints(self):
        ds = [x / 128 for x in (-4, -2, -1, 0, 1, 1, 4, 8)]
        result = timing_inference(ds)
        self.assertEqual(result["p"], sign_p(ds))
        ci = result["timing_ci"]
        bounds = [math.log1p(ci[key] / 100) for key in ("lower_pct", "upper_pct")]
        shifts = [k / 1024 for k in range(-40, 80)]
        shifts += [endpoint + delta for endpoint in bounds for delta in (-1e-8, 0, 1e-8)]
        # Remove the proposed common log shift and enumerate every block sign
        # directly, independently of the group-mean endpoint calculation.
        choices = list(itertools.product((-1, 1), repeat=len(ds)))
        for shift in shifts:
            centered = [x - shift for x in ds]
            observed = abs(math.fsum(centered))
            extreme = sum(abs(math.fsum(s * x for s, x in zip(signs, centered))) >= observed - 1e-14
                          for signs in choices)
            self.assertEqual(bounds[0] <= shift <= bounds[1], extreme / len(choices) > ALPHA)

    def test_timing_interval_coverage_under_known_multiplicative_change(self):
        true_shift = 0.04
        noise = [2 ** k / 1024 for k in range(8)]
        covered = 0
        for assignment in itertools.product((-1, 1), repeat=8):
            ds = [true_shift + s * x for s, x in zip(assignment, noise)]
            ci = timing_inference(ds)["timing_ci"]
            covered += ci["lower_pct"] <= percent(true_shift) <= ci["upper_pct"]
        self.assertEqual(covered, 244)  # 244 of 256, with discrete exact p-values

    def test_timing_interval_handles_insufficient_resolution_and_identical_effects(self):
        ci = timing_inference([0.125] * 5)["timing_ci"]
        self.assertIsNone(ci["lower_pct"])
        self.assertIsNone(ci["upper_pct"])
        ci = timing_inference([0.125] * 6)["timing_ci"]
        self.assertEqual((ci["lower_pct"], ci["upper_pct"]), (percent(0.125), percent(0.125)))

    def test_sampled_interval_with_30_blocks_matches_direct_seeded_test(self):
        ds = [(i - 10) / 128 for i in range(30)]
        result = timing_inference(ds)
        ci = result["timing_ci"]
        self.assertEqual(ci["method"], "sampled_sign_flip")
        lo, hi = [math.log1p(ci[key] / 100) for key in ("lower_pct", "upper_pct")]
        rng = random.Random(405812)
        # Evaluate the sampled two-sided test directly, independently of group means.
        shifts = [0, lo - 1e-6, lo + 1e-6, hi - 1e-6, hi + 1e-6]
        counts = [1] * len(shifts)
        observed_sum = sum(ds)
        observed = [abs(observed_sum - len(ds) * shift) for shift in shifts]
        for _ in range(200_000):
            mask = rng.getrandbits(len(ds) - 1)
            flipped = [x for i, x in enumerate(ds) if mask & (1 << i)]
            signed_sum = observed_sum - 2 * sum(flipped)
            signed_n = len(ds) - 2 * len(flipped)
            for i, shift in enumerate(shifts):
                counts[i] += abs(signed_sum - signed_n * shift) >= observed[i] - 1e-12
        self.assertEqual(result["p"], counts[0] / 200_001)
        self.assertEqual([count / 200_001 > ALPHA for count in counts[1:]], [False, True, True, False])

    def test_null_randomization_size_and_clean_positive_control(self):
        # Unique absolute signed sums apart from symmetry, giving 12/256 rejections.
        noise = [1e-8 * 2 ** i for i in range(8)]
        for shift, expected in ((0, 12), (math.log1p(.02), 256)):
            hits = sum(sign_p([shift + x * s for x, s in zip(noise, signs)]) <= ALPHA
                       for signs in itertools.product((-1, 1), repeat=8))
            self.assertEqual(hits, expected)

    def test_schedule_is_reproducible_and_completes_calibration_first(self):
        cases = [{"name": name} for name in ("encode", "decode", "copy")]
        budgets = dict(calibration_blocks=3, timing_blocks=8, profile_blocks=2)
        planned = make_schedule(cases, seed=15, **budgets)
        self.assertEqual(planned, make_schedule(cases, seed=15, **budgets))
        self.assertNotEqual(planned, make_schedule(cases, seed=16, **budgets))
        self.assertEqual([e["sequence"] for e in planned], list(range(1, 40)))
        self.assertEqual({e["order"] for e in planned}, {"ABBA", "BAAB"})
        rounds = [(stage, block) for stage, count in (("calibration", 3), ("timing", 8), ("profile", 2))
                  for block in range(1, count + 1)]
        for i, expected in enumerate(rounds):
            entries = planned[3*i:3*i+3]
            self.assertEqual({(e["stage"], e["block"]) for e in entries}, {expected})
            self.assertEqual({e["name"] for e in entries}, {"encode", "decode", "copy"})


if __name__ == "__main__":
    unittest.main()
