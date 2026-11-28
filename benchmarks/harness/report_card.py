"""Render a fixed-budget JMH timing comparison and its separate profiler results."""

from html import escape
import math
import os
from pathlib import Path
from string import Template
from urllib.parse import quote


def signed_pct(value, digits=2):
    return f"{value:+.{digits}f}%".replace("-", "−")


def counter_rows(profiled):
    names = {"cycles": "Cycles/op", "instructions": "Instructions/op", "gc.alloc.rate.norm": "Allocated heap B/op"}
    rows = []
    for key, name in names.items():
        metric = profiled["counters"][key]
        def display(value):
            return signed_pct(value) if metric["unit"] == "%" else f"{value:+.4g}".replace("-", "−")
        interval = "Unavailable"
        if "ci" in metric:
            ci = metric["ci"]
            interval = ("All possible changes" if ci["lower"] is None else
                        f'{display(ci["lower"])} to {display(ci["upper"])}')
        estimate = display(metric["change"])
        if metric.get("direction"):
            estimate = f'<strong>{estimate} <small>{metric["direction"]}</small></strong>'
        p, adjusted = (f"{metric[k]:.4g}" if k in metric else "—" for k in ("p", "p_holm"))
        rows.append(f'<tr><th scope="row">{name}</th><td>{estimate}</td><td>{interval}</td>'
                    f'<td>{p}</td><td>{adjusted}</td></tr>')
    return "\n".join(rows)


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
    html = template.substitute(
        title=escape(title), context=escape(context), cpu=cpu,
        verdict=verdict, accent=accent, tint=tint, estimate=signed_pct(effect),
        confidence=f'{100 * ci["confidence"]:g}', bounds=bounds, interval_bar=interval_bar,
        estimate_position=f"{position(effect):.6f}", left_tick=signed_pct(-limit, 0),
        left_half_tick=signed_pct(-limit / 2, 0), right_half_tick=signed_pct(limit / 2, 0),
        right_tick=signed_pct(limit, 0), p_value=f'{timing["p"]:.4g}',
        counter_rows=counter_rows(profiled),
        timing_counts=f'{timing["blocks"]} / {timing["forks"]}', profile_counts=f'{profiled["blocks"]} / {profiled["forks"]}',
        mde="Unattainable at this budget" if mde is None else f"{mde:g}% at 80% power",
        blocks=timing["blocks"], source=escape(quote(os.path.relpath(source, destination.parent))),
    )
    destination.write_text(html)
