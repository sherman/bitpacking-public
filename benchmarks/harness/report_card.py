"""Render a fixed-budget JMH timing comparison and its separate profiler results."""

from html import escape
import math
import os
from pathlib import Path
from string import Template
from urllib.parse import quote


def signed_pct(value, digits=2):
    return f"{value:+.{digits}f}%".replace("-", "−")


def write_timing_card(timing, profiled, mde, source, destination, *, title, context, cpu):
    ci = timing["timing_ci"]
    low, high = ci["lower_pct"], ci["upper_pct"]
    effect = timing["effect_pct"]
    verdict = timing["verdict"]
    accent, tint = {"IMPROVED": ("#78d9ab", "#233b2e"), "REGRESSED": ("#ffac9f", "#45302f"),
                    "INCONCLUSIVE": ("#f0ce85", "#3e3625")}[verdict]

    extent = max(abs(v) for v in (low, high, effect) if v is not None)
    limit = max(2, 2 * math.ceil(extent / 2))

    def position(value):
        return 50 + 50 * value / limit

    bounds, interval_bar = "All possible timing changes", ""
    if low is not None and high is not None:
        bounds = f"{signed_pct(low)} to {signed_pct(high)}"
        interval_bar = (f'<div class="interval" style="left: {position(low):.6f}%; '
                        f'width: {position(high) - position(low):.6f}%"></div>')

    template = Template(Path(__file__).with_name("report_card.html.template").read_text())
    counters = profiled["counters"]
    allocation = counters["gc.alloc.rate.norm"]["change"]
    allocation_text = f"{allocation:+.2g}" if 0 < abs(allocation) < .0005 else f"{allocation:+.3f}"
    html = template.substitute(
        title=escape(title), context=escape(context), cpu=cpu,
        verdict=verdict, accent=accent, tint=tint, estimate=signed_pct(effect),
        confidence=f'{100 * ci["confidence"]:g}', bounds=bounds, interval_bar=interval_bar,
        estimate_position=f"{position(effect):.6f}", left_tick=signed_pct(-limit, 0),
        left_half_tick=signed_pct(-limit / 2, 0), right_half_tick=signed_pct(limit / 2, 0),
        right_tick=signed_pct(limit, 0), p_value=f'{timing["p"]:.4g}',
        cycles=signed_pct(counters["cycles"]["change"]), instructions=signed_pct(counters["instructions"]["change"]),
        allocation=allocation_text + " B/op",
        timing_counts=f'{timing["blocks"]} / {timing["forks"]}', profile_counts=f'{profiled["blocks"]} / {profiled["forks"]}',
        mde="Unattainable at this budget" if mde is None else f"{mde:g}% at 80% power",
        blocks=timing["blocks"], source=escape(quote(os.path.relpath(source, destination.parent))),
    )
    destination.write_text(html)
