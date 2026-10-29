#!/usr/bin/env python3
"""Reads results/tables/*.csv (written by experiments.Benchmark) and writes plots + a normalised-cost table."""
import csv, math, os, sys
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

root = sys.argv[1] if len(sys.argv) > 1 else "results"
T = os.path.join(root, "tables")
P = os.path.join(root, "plots")
os.makedirs(P, exist_ok=True)

def read(name):
    with open(os.path.join(T, name + ".csv")) as f:
        return list(csv.DictReader(f))

ALGOS = ["Kruskal", "Prim", "Topological", "SCC", "Dijkstra"]
STYLE = {"Kruskal": ("tab:blue", "o"), "Prim": ("tab:orange", "s"), "Topological": ("tab:green", "^"),
         "SCC": ("tab:red", "D"), "Dijkstra": ("tab:purple", "v")}

def line_plot(rows, xkey, title, xlabel, fname):
    fig, ax = plt.subplots(figsize=(7, 4.6))
    xs = [float(r[xkey]) for r in rows]
    for a in ALGOS:
        c, m = STYLE[a]
        ax.plot(xs, [float(r[a]) for r in rows], marker=m, color=c, label=a)
    ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_xlabel(xlabel); ax.set_ylabel("average execution time (µs)")
    ax.set_title(title); ax.grid(True, which="both", alpha=0.3); ax.legend()
    fig.tight_layout(); fig.savefig(os.path.join(P, fname), dpi=150); plt.close(fig)

summary = read("time_summary")
line_plot(summary, "V", "Execution time vs. number of vertices (Small / Medium / Large)", "vertices V", "time_vs_vertices.png")
line_plot(summary, "E", "Execution time vs. number of edges (Small / Medium / Large)", "edges E", "time_vs_edges.png")

sv, se = read("scaling_vertices"), read("scaling_edges")
line_plot(sv, "V", "Scaling sweep: time vs. V (E = 5V)", "vertices V (E = 5V)", "scaling_vs_vertices.png")
line_plot(se, "E", "Scaling sweep: time vs. E (V = 1000 fixed)", "edges E (V = 1000)", "scaling_vs_edges.png")

# Kruskal vs Prim: time and operation counts
mst = read("mst_kruskal_vs_prim")
graphs = ["Small", "Medium", "Large"]
fig, axes = plt.subplots(1, 3, figsize=(13, 4))
for ax, (col, title) in zip(axes, [("Avg time (us)", "time (µs)"), ("Comparisons", "comparisons"), ("Edges examined", "edges examined")]):
    w = 0.38
    for i, alg in enumerate(["Kruskal", "Prim"]):
        vals = [float(r[col]) for g in graphs for r in mst if r["Graph"] == g and r["Algorithm"] == alg]
        ax.bar([x + (i - 0.5) * w for x in range(3)], vals, w, label=alg, color=STYLE[alg][0])
    ax.set_xticks(range(3)); ax.set_xticklabels(graphs); ax.set_yscale("log"); ax.set_title("Kruskal vs Prim: " + title)
    ax.grid(True, axis="y", alpha=0.3); ax.legend()
fig.tight_layout(); fig.savefig(os.path.join(P, "kruskal_vs_prim.png"), dpi=150); plt.close(fig)

# DSU ablation: parent-pointer steps per find
dsu = read("dsu_ablation")
variants = ["naive", "path compression only", "union by rank only", "both"]
vcol = {"naive": "tab:red", "path compression only": "tab:blue", "union by rank only": "tab:orange", "both": "tab:green"}
fig, axes = plt.subplots(1, 3, figsize=(13, 4))
for ax, wl in zip(axes, ["chain", "binomial", "random"]):
    for v in variants:
        rows = [r for r in dsu if r["Workload"] == wl and r["Variant"] == v]
        ax.plot([int(r["n"]) for r in rows], [float(r["Steps per find"]) for r in rows], marker="o", color=vcol[v], label=v)
    ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel("n (elements)"); ax.set_ylabel("parent-pointer steps per find")
    ax.set_title("DSU workload: " + wl); ax.grid(True, which="both", alpha=0.3)
axes[0].legend(fontsize=8)
fig.tight_layout(); fig.savefig(os.path.join(P, "dsu_ablation.png"), dpi=150); plt.close(fig)

# Normalised cost: time divided by the theoretical growth term, plus empirical log-log slopes
def slope(xs, ys):
    lx, ly = [math.log(x) for x in xs], [math.log(y) for y in ys]
    mx, my = sum(lx) / len(lx), sum(ly) / len(ly)
    return sum((a - mx) * (b - my) for a, b in zip(lx, ly)) / sum((a - mx) ** 2 for a in lx)

lines = ["| Algorithm | Model | " + " | ".join(f"E={r['E']}" for r in se) + " | Slope vs E (V=1000) | Slope vs V (E=5V) |",
         "|---|---|" + "---|" * (len(se) + 2)]
models = {"Kruskal": ("E log2 E", lambda V, E: E * math.log2(E)), "Prim": ("E log2 V", lambda V, E: E * math.log2(V)),
          "Topological": ("V + E", lambda V, E: V + E), "SCC": ("V + E", lambda V, E: V + E),
          "Dijkstra": ("E log2 V", lambda V, E: E * math.log2(V))}
for a in ALGOS:
    name, f = models[a]
    cells = [f"{1000 * float(r[a]) / f(int(r['V']), int(r['E'])):.1f}" for r in se]
    s_e = slope([int(r["E"]) for r in se], [float(r[a]) for r in se])
    s_v = slope([int(r["V"]) for r in sv], [float(r[a]) for r in sv])
    lines.append(f"| {a} | ns per {name} | " + " | ".join(cells) + f" | {s_e:.2f} | {s_v:.2f} |")
with open(os.path.join(T, "normalized_cost.md"), "w") as f:
    f.write("\n".join(lines) + "\n")
print("\n".join(lines))
