"""Audit small fixed-budget reports by exhaustive enumeration, independently of harness.analysis."""

import itertools
import math
from statistics import geometric_mean, mean


def verify_profile(groups, summary):
    n = len(groups)
    assert (summary["blocks"], summary["forks"]) == (n, 4*n)
    signs = list(itertools.product((-1, 1), repeat=n))
    raw_p = {}
    for key in ("cycles", "instructions", "gc.alloc.rate.norm"):
        saved = summary["counters"][key]
        allocation = key == "gc.alloc.rate.norm"
        effects = []
        for group in groups:
            def values(side):
                return [row["jmh"]["secondaryMetrics"][key]["score"] for row in group if row["label"] == side]
            a, b = values("A"), values("B")
            assert len(a) == len(b) == 2
            effects.append(mean(b)-mean(a) if allocation else math.log(geometric_mean(b)/geometric_mean(a)))
        def exact_p(shift):
            xs = [x-shift for x in effects]
            threshold = abs(math.fsum(xs)) - 32*math.ulp(math.fsum(map(abs, xs)))
            count = sum(abs(math.fsum(s*x for s, x in zip(mask, xs))) >= threshold for mask in signs)
            return count/len(signs)

        estimate = mean(effects) if allocation else 100*math.expm1(mean(effects))
        assert math.isclose(estimate, saved["change"], abs_tol=1e-10, rel_tol=1e-10)
        assert saved["unit"] == ("B/op" if allocation else "%")
        raw_p[key] = exact_p(0)
        assert saved["p"] == raw_p[key], (key, saved["p"], raw_p[key])
        ci = saved["ci"]
        assert ci["confidence"] == .95 and ci["method"] == "exact_sign_flip"
        if 2/len(signs) > .05:
            assert ci["lower"] is None and ci["upper"] is None
        else:
            low, high = ci["lower"], ci["upper"]
            if not allocation:
                low, high = math.log1p(low/100), math.log1p(high/100)
            delta = max(1e-12, 1e-7*max(map(abs, effects)))
            assert exact_p(low-delta) <= .05 and exact_p(high+delta) <= .05
            assert exact_p(min(low+delta, (low+high)/2)) > .05
            assert exact_p(max(high-delta, (low+high)/2)) > .05
    # Closed Bonferroni tests over every subset give the Holm-adjusted p-values.
    for key, saved in summary["counters"].items():
        intersections = [len(subset)*min(raw_p[k] for k in subset)
                         for size in range(1, len(raw_p)+1) for subset in itertools.combinations(raw_p, size)
                         if key in subset]
        adjusted = min(1, max(intersections))
        assert saved["p_holm"] == adjusted
        direction = "increase" if saved["change"] > 0 else "decrease"
        assert saved["direction"] == (direction if adjusted <= .05 else None)
