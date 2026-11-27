#!/usr/bin/env python3
"""Generates the SVG figures for docs/vector-kernels.md.

Usage: python3 docs/images/generate_figures.py docs/images docs/vector-kernels.md
(the results chart reads the per-width table from the markdown file).
"""
import os
import re
import sys

OUT = sys.argv[1]
TABLE = sys.argv[2]
os.makedirs(OUT, exist_ok=True)

INK = "#191919"
INK2 = "#666666"
GRID = "#d9d9d9"
SURF = "#ffffff"
HUES = ["#257de0", "#f47230", "#00b894", "#f2a600", "#ee7fac", "#008a17", "#6a4ce2", "#ec4647"]
OLD = "#9c9c9c"
NEW = HUES[0]
HOT = HUES[7]
FONT = "font-family='ui-sans-serif, -apple-system, Segoe UI, Helvetica, Arial, sans-serif'"
MONO = "font-family='ui-monospace, SFMono-Regular, Menlo, Consolas, monospace'"


def tint(hexc, a):
    r, g, b = (int(hexc[i:i + 2], 16) for i in (1, 3, 5))
    r, g, b = (round(c * a + 255 * (1 - a)) for c in (r, g, b))
    return f"#{r:02x}{g:02x}{b:02x}"


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class Svg:
    def __init__(self, w, h):
        self.w, self.h, self.parts = w, h, []
        self.parts.append(f"<rect width='{w}' height='{h}' fill='{SURF}'/>")

    def add(self, s):
        self.parts.append(s)

    def rect(self, x, y, w, h, fill="none", stroke=INK2, sw=1, rx=0, dash=None):
        d = f" stroke-dasharray='{dash}'" if dash else ""
        self.add(f"<rect x='{x}' y='{y}' width='{w}' height='{h}' rx='{rx}' fill='{fill}' stroke='{stroke}' stroke-width='{sw}'{d}/>")

    def text(self, x, y, s, size=12, color=INK, anchor="start", mono=False, weight="normal"):
        f = MONO if mono else FONT
        self.add(f"<text x='{x}' y='{y}' font-size='{size}' fill='{color}' text-anchor='{anchor}' font-weight='{weight}' {f}>{esc(s)}</text>")

    def line(self, x1, y1, x2, y2, color=INK2, sw=1, arrow=False, dash=None):
        m = f" marker-end='url(#a{color[1:]})'" if arrow else ""
        d = f" stroke-dasharray='{dash}'" if dash else ""
        self.add(f"<line x1='{x1}' y1='{y1}' x2='{x2}' y2='{y2}' stroke='{color}' stroke-width='{sw}'{m}{d}/>")

    def path(self, d, color=INK2, sw=1, arrow=False, fill="none"):
        m = f" marker-end='url(#a{color[1:]})'" if arrow else ""
        self.add(f"<path d='{d}' stroke='{color}' stroke-width='{sw}' fill='{fill}'{m}/>")

    def save(self, name):
        colors = set(re.findall(r"url\(#a([0-9a-f]{6})\)", "".join(self.parts)))
        defs = "".join(
            f"<marker id='a{c}' viewBox='0 0 10 10' refX='9' refY='5' markerWidth='7' markerHeight='7' orient='auto'>"
            f"<path d='M0,1 L9,5 L0,9 z' fill='#{c}'/></marker>" for c in sorted(colors))
        body = "\n".join(self.parts)
        with open(os.path.join(OUT, name), "w") as f:
            f.write(f"<svg xmlns='http://www.w3.org/2000/svg' width='{self.w}' height='{self.h}' viewBox='0 0 {self.w} {self.h}'>\n<defs>{defs}</defs>\n{body}\n</svg>\n")


def box(s, x, y, w, h, lines, fill="#f5f5f5", stroke=INK2, size=12, mono_first=False):
    s.rect(x, y, w, h, fill=fill, stroke=stroke, rx=6)
    n = len(lines)
    for i, ln in enumerate(lines):
        yy = y + h / 2 + (i - (n - 1) / 2) * (size + 4) + size * 0.35
        s.text(x + w / 2, yy, ln, size=size if i == 0 else size - 1, anchor="middle",
               color=INK if i == 0 else INK2, mono=(mono_first and i == 0), weight="bold" if i == 0 else "normal")


# ---------------------------------------------------------------- 1. streams
def fig_streams():
    cw, ch, x0, y0 = 42, 26, 120, 56
    s = Svg(x0 + 16 * cw + 30, y0 + 8 * ch + 60)
    s.text(x0 + 8 * cw, 22, "lane j of every vector belongs to stream j", size=13, anchor="middle", color=INK2)
    for j in range(16):
        s.text(x0 + j * cw + cw / 2, y0 - 10, f"s{j}", size=11, anchor="middle", color=NEW if j == 3 else INK2,
               weight="bold" if j == 3 else "normal")
    for k in range(8):
        y = y0 + k * ch
        s.text(x0 - 12, y + ch / 2 + 4, f"vector {k}  (in[{16 * k}..{16 * k + 15}])", size=11, anchor="end", color=INK2)
        for j in range(16):
            x = x0 + j * cw
            hl = j == 3
            s.rect(x, y, cw, ch, fill=tint(NEW, 0.22) if hl else SURF, stroke=GRID)
            s.text(x + cw / 2, y + ch / 2 + 4, str(16 * k + j), size=11, anchor="middle", mono=True,
                   color=INK if hl else INK2, weight="bold" if hl else "normal")
    s.rect(x0 + 3 * cw, y0, cw, 8 * ch, stroke=NEW, sw=2)
    s.text(x0 + 3 * cw + cw / 2, y0 + 8 * ch + 22, "stream 3 = values 3, 19, 35, … 115", size=12, anchor="middle", color=NEW,
           weight="bold")
    s.text(x0 + 3 * cw + cw / 2, y0 + 8 * ch + 40, "(8 values, packed into one 32-bit lane)", size=11, anchor="middle", color=INK2)
    s.save("01-streams.svg")


# ---------------------------------------------------------------- 2. bits of one stream, width 13
def fig_bits13():
    px, x0, y0, b = 8, 30, 70, 13
    s = Svg(x0 + 104 * px + 30, 300)
    s.text(x0, 24, "One stream at width 13: 8 values × 13 bits = 104 bits, written low bit first", size=13, color=INK)
    s.text(x0, 44, "bit 0 →", size=11, color=INK2)
    s.text(x0 + 104 * px, 44, "← bit 103", size=11, color=INK2, anchor="end")
    for i in range(8):
        x = x0 + i * b * px
        s.rect(x, y0, b * px, 34, fill=tint(HUES[i], 0.35), stroke=HUES[i])
        lo, hi = b * i, b * i + b
        cuts = sorted({lo, hi} | {c for c in (32, 64, 96) if lo < c < hi})
        a, z = max(zip(cuts, cuts[1:]), key=lambda t: t[1] - t[0])
        s.text(x0 + (a + z) / 2 * px, y0 + 22, f"v{i}", size=13, anchor="middle", weight="bold")
    words = [(0, 32, "w0", "bytes 0–63", "v0 | v1<<13 | v2<<26"),
             (32, 64, "w1", "bytes 64–127", "v2>>>6 | v3<<7 | v4<<20"),
             (64, 96, "w2", "bytes 128–191", "v4>>>12 | v5<<1 | v6<<14 | v7<<27"),
             (96, 104, "tail", "bytes 192–207", "v7>>>5")]
    for lo, hi, name, where, f in words:
        xa, xb = x0 + lo * px, x0 + hi * px
        s.line(xa, y0 - 8, xa, y0 + 60, color=INK, sw=2)
        s.path(f"M{xa + 2},{y0 + 52} L{xa + 2},{y0 + 58} L{xb - 2},{y0 + 58} L{xb - 2},{y0 + 52}", color=INK)
        s.text((xa + xb) / 2, y0 + 80, name, size=13, anchor="middle", weight="bold")
        if name != "tail":
            s.text((xa + xb) / 2, y0 + 98, f"32 bits · {where}", size=11, anchor="middle", color=INK2)
            s.text((xa + xb) / 2, y0 + 120, f"{name} = {f}", size=11, anchor="middle", mono=True)
    s.line(x0 + 104 * px, y0 - 8, x0 + 104 * px, y0 + 60, color=INK, sw=2)
    tx = x0 + 100 * px
    s.text(tx, y0 + 98, "8 bits · 1 byte", size=11, anchor="middle", color=INK2)
    s.text(tx, y0 + 112, "bytes 192–207", size=11, anchor="middle", color=INK2)
    s.text(tx - 20, y0 + 134, "tail = v7>>>5", size=11, anchor="middle", mono=True)
    s.text(x0, 250, "Pack: each output word is an OR of shifted fields; a field that crosses a word boundary (v2, v4, v7)", size=12, color=INK2)
    s.text(x0, 268, "is split: its low part goes in one word, the rest (>>>) starts the next. Unpack reverses it: shift right, OR the", size=12, color=INK2)
    s.text(x0, 286, "high part from the next word, mask with 0x1FFF.  All 16 streams do this at once, one per 32-bit lane.", size=12, color=INK2)
    s.save("02-bits-width13.svg")


# ---------------------------------------------------------------- 3. block bytes, width 13
def fig_block13():
    cw, ch, x0, y0 = 44, 30, 150, 50
    s = Svg(x0 + 16 * cw + 40, y0 + 4 * ch + 3 * 14 + 90)
    s.text(x0 + 8 * cw, 22, "Block at width 13 = 16 × 13 = 208 bytes", size=13, anchor="middle")
    rows = [("w0", "bytes 0–63"), ("w1", "bytes 64–127"), ("w2", "bytes 128–191")]
    y = y0
    for r, (name, where) in enumerate(rows):
        s.text(x0 - 12, y + ch / 2 + 4, f"{name} · {where}", size=11, anchor="end", color=INK2)
        for j in range(16):
            s.rect(x0 + j * cw, y, cw, ch, fill=tint(HUES[0], 0.10 + 0.08 * r), stroke=GRID)
            s.text(x0 + j * cw + cw / 2, y + ch / 2 + 4, f"s{j}", size=11, anchor="middle", color=INK2)
        y += ch + 14
    s.text(x0 - 12, y + ch / 2 + 4, "tail · bytes 192–207", size=11, anchor="end", color=INK2)
    tw = cw / 4
    for j in range(16):
        s.rect(x0 + j * tw, y, tw, ch, fill=tint(HUES[3], 0.45), stroke=GRID)
    s.text(x0 + 16 * tw + 10, y + ch / 2 + 4, "1 byte per stream (s0 … s15)", size=11, color=INK2)
    s.text(x0 - 130, y + ch + 34, "Each full row is one 512-bit vector: stream j's word sits at bytes 4j…4j+3 (little-endian).", size=12, color=INK2)
    s.text(x0 - 130, y + ch + 52, "The tail holds the last (bits mod 4) bytes of every stream, packed tightly: 0, 16, 32 or 48 bytes.", size=12, color=INK2)
    s.save("03-block-width13.svg")


# ---------------------------------------------------------------- 4. tails
def fig_tails():
    s = Svg(900, 500)
    bw, bh = 230, 54

    def pipe(y, title, color, steps, note=None):
        s.text(20, y - 12, title, size=13, weight="bold", color=color)
        x = 20
        for i, st in enumerate(steps):
            box(s, x, y, bw, bh, st, fill=tint(color, 0.12), stroke=color)
            if i + 1 < len(steps):
                s.line(x + bw + 4, y + bh / 2, x + bw + 44, y + bh / 2, color=INK2, arrow=True)
            x += bw + 48
        if note:
            s.text(20, y + bh + 18, note, size=11, color=INK2)

    s.text(20, 26, "Load a 1-byte tail into 16 int lanes (2-byte tail: same shape with 16-bit elements)", size=14)
    pipe(64, "Before", OLD, [["masked 64-byte load", "mask = first 16 bytes"],
                             ["vpermb, zeroing mask", "byte j → byte 4j of lane j"],
                             ["w: 16 × int", "[t0 0 0 0][t1 0 0 0]…"]],
         "When the block is the last thing in the array, the 64-byte window runs past its end: the masked load falls back to a slow path.")
    pipe(176, "After", NEW, [["16-byte load", "exactly the tail bytes"],
                             ["vpmovzxbd", "zero-extend 16 × u8 → 16 × u32"],
                             ["w: 16 × int", "[t0 0 0 0][t1 0 0 0]…"]],
         "Never touches bytes outside the block, no mask, one permute fewer. 2-byte tail: 32-byte load + vpmovzxwd.")
    s.line(20, 268, 880, 268, color=GRID)
    s.text(20, 300, "Store 16 int lanes as a 1-byte tail", size=14)
    pipe(338, "Before", OLD, [["w: 16 × int", "low byte of each lane"],
                              ["vpermb compact", "byte 4j → byte j"],
                              ["masked 64-byte store", "16 bytes enabled"]])
    pipe(420, "After", NEW, [["w: 16 × int", "low byte of each lane"],
                             ["vpmovdb", "truncate 16 × u32 → 16 × u8"],
                             ["16-byte store", "2-byte tail: vpmovdw + 32 bytes"]])
    s.save("04-tails.svg")


# ---------------------------------------------------------------- 5. deltas in sorted pack
def fig_delta():
    cw, ch, x0 = 40, 26, 150
    s = Svg(x0 + 16 * cw + 50, 330)
    s.text(20, 24, "Sorted pack: delta = current − (the value just before it in input order)", size=13)
    rows = [("previous vector", "p", 50, tint(OLD, 0.25)), ("current vector", "c", 120, tint(NEW, 0.18))]
    for label, ch_, y, fill in rows:
        s.text(x0 - 12, y + ch / 2 + 4, label, size=11, anchor="end", color=INK2)
        for j in range(16):
            s.rect(x0 + j * cw, y, cw, ch, fill=fill, stroke=GRID)
            s.text(x0 + j * cw + cw / 2, y + ch / 2 + 4, f"{ch_}{j}", size=11, anchor="middle", mono=True)
    y2 = 200
    s.text(x0 - 12, y2 + ch / 2 + 4, "selectFrom(prev, cur)", size=11, anchor="end", color=INK2, mono=True)
    for j in range(16):
        isprev = j == 0
        s.rect(x0 + j * cw, y2, cw, ch, fill=tint(OLD, 0.25) if isprev else tint(NEW, 0.18), stroke=GRID)
        s.text(x0 + j * cw + cw / 2, y2 + ch / 2 + 4, "p15" if isprev else f"c{j - 1}", size=11, anchor="middle", mono=True)
    xr = x0 + 16 * cw
    s.path(f"M{xr},{50 + ch / 2} L{xr + 18},{50 + ch / 2} L{xr + 18},{178} L{x0 + cw / 2},{178} L{x0 + cw / 2},{y2 - 2}", color=OLD,
           sw=1.5, arrow=True)
    for j in (0, 1, 7, 13, 14):
        s.line(x0 + j * cw + cw / 2, 120 + ch, x0 + (j + 1) * cw + cw / 2 - 4, y2 - 2, color=NEW, arrow=True)
    s.text(x0, 258, "delta = cur − selectFrom(prev, cur)       index vector = [15, 16, 17, … 30]", size=12, mono=True)
    s.text(x0, 280, "Before: previous.rearrange(PREVIOUS, current)  →  compare + 2 permutes + blend", size=12, color=INK2)
    s.text(x0, 300, "After:  PREVIOUS_INDEX.selectFrom(previous, current)  →  one vpermi2d", size=12, color=NEW, weight="bold")
    s.text(x0, 320, "For the first vector, prev = broadcast(initial). Strict mode subtracts 1 more.", size=12, color=INK2)
    s.save("05-delta-pack.svg")


# ---------------------------------------------------------------- 6/7. prefix sums
def scan_fig(name, title, steps, note):
    cw, x0, top, gap = 44, 150, 64, 70
    n = len(steps)
    s = Svg(x0 + 16 * cw + 40, top + n * gap + 60 + 40)
    s.text(20, 26, title, size=14)
    ys = [top + i * gap for i in range(n + 1)]
    for i, (label, sub, color, srcfn) in enumerate(steps):
        ya, yb = ys[i] + 8, ys[i + 1] - 10
        s.text(x0 - 16, (ya + yb) / 2 + 2, label, size=12, anchor="end", color=color, weight="bold")
        s.text(x0 - 16, (ya + yb) / 2 + 18, sub, size=10, anchor="end", color=INK2)
        for j in range(16):
            src = srcfn(j)
            if src is not None:
                s.line(x0 + src * cw + cw / 2, ya, x0 + j * cw + cw / 2, yb, color=color, sw=1.2, arrow=True)
    for i, y in enumerate(ys):
        for j in range(16):
            s.add(f"<circle cx='{x0 + j * cw + cw / 2}' cy='{y}' r='7' fill='{SURF}' stroke='{INK2}' stroke-width='1.2'/>")
    for j in range(16):
        s.text(x0 + j * cw + cw / 2, top - 16, str(j), size=11, anchor="middle", color=INK2)
    s.text(x0 - 16, top - 16, "lane", size=11, anchor="end", color=INK2)
    s.text(20, ys[-1] + 40, note, size=12, color=INK2)
    s.save(name)


def fig_prefix():
    old = [(f"+ lane−{d}", f"vpermb, {4 * d}-byte shift", OLD, (lambda d: lambda j: j - d if j >= d else None)(d)) for d in (1, 2, 4, 8)]
    scan_fig("06-prefix-old.svg", "Before: 16-lane inclusive prefix sum, 4 shuffle steps (Hillis–Steele, byte permutes)", old,
             "Every step is a masked byte permute (vpermb) plus an add. Each arrow means “add the source lane into this lane”.")

    def pair(sft):
        return lambda j: ((j // 2 - sft) * 2 + 1) if j // 2 >= sft else None

    new = [("+ in pair", "vpsllq 32 (no shuffle)", HUES[2], lambda j: j - 1 if j % 2 else None),
           ("+ pair −1", "vpermd, zero-masked", NEW, pair(1)),
           ("+ pair −2", "vpermd, zero-masked", NEW, pair(2)),
           ("+ pair −4", "vpermd, zero-masked", NEW, pair(4))]
    scan_fig("07-prefix-new.svg", "After: qword shift for step 1, then 3 steps over lane pairs", new,
             "Step 1 adds lane 2p into 2p+1 by shifting each 64-bit qword left 32. Odd lanes then hold pair totals, and each later step adds them.")


# ---------------------------------------------------------------- 8. carry chain
def fig_carry():
    s = Svg(900, 560)
    bw, bh = 150, 34
    s.text(20, 26, "Joining the 8 per-vector prefix sums into one running sum (critical path in red)", size=14)

    # before
    xL = 20
    s.text(xL, 62, "Before", size=13, weight="bold", color=INK2)
    y = 80
    bx = xL + bw + 40
    cxo = bx + 75
    prev = None
    for k in range(8):
        if k == 4:
            box(s, bx - 60, y + 4, 270, 34, ["store out[0..63], reload out[63]", "Part0 → Part1 handoff via memory"],
                fill=tint(HOT, 0.12), stroke=HOT, size=11)
            s.line(cxo, prev, cxo, y + 2, color=HOT, sw=2, arrow=True)
            prev = y + 38
            y += 52
        box(s, xL, y, bw, bh, [f"scan(v{k})"], size=12)
        box(s, bx, y, 150, bh, [f"out{k} = scan + bcast"], fill=tint(HOT, 0.10), stroke=HOT, size=12)
        s.line(xL + bw + 2, y + bh / 2, bx - 4, y + bh / 2, color=INK2, arrow=True)
        if prev:
            s.line(cxo, prev + 1, cxo, y - 3, color=HOT, sw=2, arrow=True)
        prev = y + bh
        y += bh + 14
    s.text(xL, 534, "bcast(out[k−1] lane 15) is a permute of the previous result,", size=11, color=INK2)
    s.text(xL, 548, "so every output waits for: permute + add, 8 times in a row.", size=11, color=INK2)

    # after
    xR = 470
    s.text(xR, 62, "After", size=13, weight="bold", color=NEW)
    y = 80
    prevc = None
    for k in range(8):
        box(s, xR, y, 110, bh, [f"scan(v{k})"], size=12)
        if k < 7:
            box(s, xR + 125, y + 20, 110, bh - 4, [f"bcast(v{k}[15])"], size=11, fill=tint(NEW, 0.10), stroke=NEW)
            s.line(xR + 110, y + bh / 2 + 6, xR + 123, y + 35, color=INK2, arrow=True)
        cx = xR + 255
        box(s, cx, y, 70, bh, ["carry" if k == 0 else "+ add"], size=11, fill=tint(HOT, 0.10), stroke=HOT)
        if prevc:
            s.line(cx + 35, prevc + 1, cx + 35, y - 3, color=HOT, sw=2, arrow=True)
            s.line(xR + 235, y - 12, cx - 3, y + 8, color=NEW, arrow=True)
        prevc = y + bh
        box(s, cx + 95, y, 70, bh, [f"out{k}"], size=12, fill=tint(NEW, 0.12), stroke=NEW)
        s.line(cx + 72, y + bh / 2, cx + 93, y + bh / 2, color=INK2, arrow=True)
        y += bh + 20
    s.text(xR, 534, "carry += bcast(v[k] lane 15) depends only on the scan,", size=11, color=INK2)
    s.text(xR, 548, "so the serial path is one add per vector; permutes run in parallel.", size=11, color=INK2)
    s.save("08-carry-chain.svg")


# ---------------------------------------------------------------- 9. per-width results chart
def fig_chart():
    rows = []
    for ln in open(TABLE):
        m = re.match(r"\|\s*(\d+)\s*\|(.*)\|\s*$", ln)
        if not m or "→" not in m.group(2):
            continue
        cells = [c.strip() for c in m.group(2).split("|")]
        vals = []
        for c in cells:
            a, b = c.replace("~", "").split("→")
            vals.append((float(a), float(b)))
        rows.append((int(m.group(1)), vals))
    names = ["Plain decode", "Sorted decode", "Strict decode", "Plain pack", "Sorted pack", "Strict pack", "Sorted detect", "Strict detect"]
    panels = [0, 1, 2, 3, 4, 5]
    pw, ph, ml, mt, gx, gy = 400, 180, 50, 80, 70, 90
    cols = 2
    W = ml + cols * pw + (cols - 1) * gx + 20
    H = mt + 3 * ph + 2 * gy + 84
    s = Svg(W, H)
    s.text(ml - 30, 26, "ns per 128-value block, by bit width — committed vs. new (lower is better)", size=14)
    # legend
    s.line(ml - 30, 48, ml - 6, 48, color=OLD, sw=2)
    s.text(ml, 52, "before", size=12, color=INK)
    s.line(ml + 60, 48, ml + 84, 48, color=NEW, sw=2)
    s.text(ml + 90, 52, "after", size=12, color=INK)
    ymax = 20
    for pi, col in enumerate(panels):
        px = ml + (pi % cols) * (pw + gx)
        py = mt + (pi // cols) * (ph + gy)
        mo = sum(v[col][0] for _, v in rows) / len(rows)
        mn = sum(v[col][1] for _, v in rows) / len(rows)
        s.text(px, py - 2, names[col], size=13, weight="bold")
        s.text(px + pw, py - 2, f"mean {mo:.1f} → {mn:.1f} ns", size=12, anchor="end", color=INK2)
        top = py + 12

        def X(w):
            return px + (w - 1) / 31 * pw

        def Y(v):
            return top + ph - v / ymax * ph

        for t in range(0, ymax + 1, 5):
            s.line(px, Y(t), px + pw, Y(t), color=GRID, sw=1 if t else 1.2)
            s.text(px - 6, Y(t) + 4, str(t), size=10, anchor="end", color=INK2)
        for w in (1, 8, 16, 24, 32):
            s.text(X(w), top + ph + 16, str(w), size=10, anchor="middle", color=INK2)
        s.text(px + pw / 2, top + ph + 32, "bit width", size=10, anchor="middle", color=INK2)
        for idx, color in ((0, OLD), (1, NEW)):
            pts = " ".join(f"{X(w):.1f},{Y(v[col][idx]):.1f}" for w, v in rows)
            s.add(f"<polyline points='{pts}' fill='none' stroke='{color}' stroke-width='2' stroke-linejoin='round'/>")
    s.text(ml - 30, H - 14, "Means are over widths 1–32. JDK 25, AMD Zen 5, 16 blocks in L1, 2 forks × 5 s.", size=11,
           color=INK2)
    s.save("09-results-by-width.svg")


fig_streams()
fig_bits13()
fig_block13()
fig_tails()
fig_delta()
fig_prefix()
fig_carry()
fig_chart()
print("ok")
