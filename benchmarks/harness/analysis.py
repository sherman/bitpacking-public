"""Paired timing effects, randomization intervals and A/A calibration."""

from bisect import bisect_left
import itertools
import math
import random
from statistics import mean

COUNTERS = ("cycles", "instructions", "gc.alloc.rate.norm")
ALPHA = 0.05
SHIFTS = [0, 0.5, 1, 2, 3, 5, 10]


def log_effect(values, order):
    if len(values) != 4 or order not in ("ABBA", "BAAB"):
        raise ValueError("A block must contain four slots ordered ABBA or BAAB")
    return math.fsum(math.log(x) * (1 if side == "B" else -1)
                     for x, side in zip(values, order, strict=True)) / 2


def signed_sums(xs):
    totals = [0.0]
    for x in xs:
        totals = [v + x for v in totals] + [v - x for v in totals]
    return totals


def sign_p(xs):
    # Count the upper tail by pairing half-block sums. Symmetry gives the lower tail.
    threshold = abs(math.fsum(xs)) - 32 * math.ulp(math.fsum(map(abs, xs)))
    if threshold <= 0:
        return 1.0
    middle = len(xs) // 2
    left = signed_sums(xs[:middle])
    right = sorted(signed_sums(xs[middle:]))
    count = sum(len(right) - bisect_left(right, threshold - v) for v in left)
    return 2 * count / (2 ** len(xs))


def percent(log_ratio):
    return 100 * math.expm1(log_ratio)


def timing_inference(effects, alpha=ALPHA):
    """Invert the two-sided block sign-flip test of a common shift in log time."""
    n = len(effects)
    if not n or not all(map(math.isfinite, effects)) or not 0 < alpha < 1:
        raise ValueError("Expected finite block log ratios and 0 < alpha < 1")
    # Opposite sign assignments have the same absolute statistic. Keep one per pair.
    exact = n <= 20
    if exact:
        masks = range(1 << (n - 1))
        total = len(masks)
        always = 0
    else:
        rng = random.Random(405812)
        masks = (rng.getrandbits(n - 1) for _ in range(200_000))
        total = 200_001
        always = 1  # Include the observed assignment in the Monte Carlo p-value.

    lower, upper = [], []
    for mask in masks:
        if mask == 0:
            always += 1
            continue
        # For a proposed shift h, split the centered observations into sums A and B.
        # |A-B| >= |A+B| iff A*B <= 0, i.e. h lies between the two group means.
        flipped, retained = [], []
        for i, value in enumerate(effects):
            (flipped if mask & (1 << i) else retained).append(value)
        a, b = math.fsum(flipped) / len(flipped), math.fsum(retained) / len(retained)
        lower.append(min(a, b))
        upper.append(max(a, b))

    lower.sort()
    upper.sort()
    # Every interval contains the overall mean. p(h) > alpha therefore describes
    # one closed interval. Count ties inclusively, as in the verdict test.
    rank = math.floor(alpha * total) - always  # zero-based endpoint rank
    low_pct = percent(lower[rank]) if rank >= 0 else None
    high_pct = percent(upper[-1 - rank]) if rank >= 0 else None
    p = (always + sum(v <= 0 for v in lower) - sum(v < 0 for v in upper)) / total
    return dict(p=p, timing_ci=dict(confidence=1 - alpha, lower_pct=low_pct, upper_pct=high_pct,
                                    method="exact_sign_flip" if exact else "sampled_sign_flip"))


def minimum_detectable_effects(noise, budgets, draws=2000):
    # Grid step is 0.1 percentage point. At the upper bound every shifted block is positive.
    limit = math.ceil(1000 * math.expm1(max(noise))) + 1
    shifts = [math.log1p(i / 1000) for i in range(limit + 1)]
    rng = random.Random(405811)
    result = {}
    for n in budgets:
        changes = [0] * (limit + 1)
        for _ in range(draws):
            sample = [rng.choice(noise) * rng.choice((-1, 1)) for _ in range(n)]
            if 2 / 2 ** n > ALPHA:
                continue

            def direction(index):
                ds = [x + shifts[index] for x in sample]
                if sign_p(ds) > ALPHA:
                    return 0
                return 1 if math.fsum(ds) > 0 else -1

            # With increasing shift, the number of negative subset sums can only fall.
            # A two-sided rejection can occur on either side of the acceptance interval.
            if direction(0) < 0:
                changes[0] += 1
                stop = bisect_left(range(limit + 1), True, key=lambda i: direction(i) >= 0)
                changes[stop] -= 1
            start = bisect_left(range(limit + 1), True, key=lambda i: direction(i) > 0)
            changes[start] += 1
        rates = [count / draws for count in itertools.accumulate(changes)]
        crossing = next((i for i, rate in enumerate(rates) if rate >= 0.8), None)
        result[n] = dict(mde_slowdown_pct=None if crossing is None else crossing / 10,
                         attainable_p_min=2 / 2 ** n, draws=draws,
                         estimated_power=None if crossing is None else rates[crossing],
                         power_at_grid_points={str(s): rates[int(10*s)] for s in SHIFTS if int(10*s) <= limit})
    return result


def metric_score(row, key):
    if key == "time":
        return row["jmh"]["primaryMetric"]["score"]
    return row["jmh"]["secondaryMetrics"][key]["score"]


def paired_change(rows, key):
    order = "".join(row["label"] for row in rows)
    xs = [metric_score(row, key) for row in rows]
    if key == "gc.alloc.rate.norm":
        if order not in ("ABBA", "BAAB"):
            raise ValueError("A block must contain four slots ordered ABBA or BAAB")
        return math.fsum(value if side == "B" else -value for value, side in zip(xs, order)) / 2
    return log_effect(xs, order)


def timing_summary(groups):
    effects = [paired_change(group, "time") for group in groups]
    timing = dict(blocks=len(groups), forks=4 * len(groups), effect_pct=percent(mean(effects)),
                  **timing_inference(effects))
    for side, key in (("A", "baseline_ns_per_op"), ("B", "candidate_ns_per_op")):
        scores = [metric_score(row, "time") for group in groups for row in group if row["label"] == side]
        timing[key] = math.exp(mean(map(math.log, scores)))
    ci = timing["timing_ci"]
    if ci["upper_pct"] is not None and ci["upper_pct"] < 0:
        timing["verdict"] = "IMPROVED"
    elif ci["lower_pct"] is not None and ci["lower_pct"] > 0:
        timing["verdict"] = "REGRESSED"
    else:
        timing["verdict"] = "INCONCLUSIVE"
    return timing


def profile_summary(groups):
    counters = {}
    for key in COUNTERS:
        ds = [paired_change(g, key) for g in groups]
        counters[key] = dict(change=mean(ds) if key == "gc.alloc.rate.norm" else percent(mean(ds)),
                             unit="B/op" if key == "gc.alloc.rate.norm" else "%")
    return dict(blocks=len(groups), forks=4 * len(groups), counters=counters)
