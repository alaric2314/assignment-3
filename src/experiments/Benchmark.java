package experiments;

import connectivity.StronglyConnectedComponents;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Graph;
import graph.GraphGenerator;
import graph.Metrics;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Reproducible experiments. Usage: java -cp out experiments.Benchmark [outputDir]
 * Writes CSV + Markdown tables to <outputDir>/tables (default: results).
 */
public final class Benchmark {
    static final long SEED = 42;
    static final int REPS = 5;          // timed repetitions per experiment
    static final int WARMUP = 20;       // minimum untimed runs before the timed ones (JIT warm-up)
    static final long WARMUP_NANOS = 300_000_000L;   // ...and warm-up lasts at least 300 ms
    static final long BATCH_NANOS = 20_000_000L;     // target duration of one timed batch
    static final int MAX_W = 100;       // edge weights uniform in [1, 100]
    static final String[] NAMES = {"Small", "Medium", "Large"};
    static final int[][] CONFIGS = {{10, 30}, {100, 500}, {1000, 5000}};

    interface Task { Metrics run(); }

    static final class Stat {
        final double avgNs; final double sdNs; final Metrics m;
        Stat(double avgNs, double sdNs, Metrics m) { this.avgNs = avgNs; this.sdNs = sdNs; this.m = m; }
        double us() { return avgNs / 1000.0; }
        double sd() { return sdNs / 1000.0; }
    }

    static long sink;   // consumes results so the JIT cannot discard the work

    /**
     * Timing procedure (see README, "Experimental Setup"):
     *  1. warm-up: at least `warmup` runs and at least 300 ms, so the JIT has compiled the code;
     *  2. calibration: time a few runs to choose `inner`, the number of runs per batch (~20 ms per batch);
     *  3. REPS = 5 repetitions; each repetition times `inner` consecutive runs with System.nanoTime()
     *     and stores the per-run time. Reported value = mean of the 5 repetitions (SD = their std. deviation).
     * Batching keeps timer resolution and one-off pauses (GC, JIT threads) from dominating microsecond-scale runs.
     */
    static Stat measure(Task t, int warmup) {
        long start = System.nanoTime();
        int runs = 0;
        while (runs < warmup || System.nanoTime() - start < WARMUP_NANOS) { sink += t.run().edgesExamined; runs++; }

        long c0 = System.nanoTime();
        for (int i = 0; i < 5; i++) sink += t.run().edgesExamined;
        double est = Math.max(1, (System.nanoTime() - c0) / 5.0);
        int inner = (int) Math.max(1, Math.min(200_000, BATCH_NANOS / est));

        double[] per = new double[REPS];
        Metrics last = null;
        for (int r = 0; r < REPS; r++) {
            long s = System.nanoTime();
            for (int i = 0; i < inner; i++) { last = t.run(); sink += last.edgesExamined; }
            per[r] = (System.nanoTime() - s) / (double) inner;
        }
        double mean = 0;
        for (double x : per) mean += x;
        mean /= REPS;
        double var = 0;
        for (double x : per) var += (x - mean) * (x - mean);
        return new Stat(mean, Math.sqrt(var / (REPS - 1)), last);
    }

    static final class Table {
        final String name; final String[] header; final List<String[]> rows = new ArrayList<>();
        Table(String name, String... header) { this.name = name; this.header = header; }
        void add(Object... cells) {
            String[] r = new String[cells.length];
            for (int i = 0; i < r.length; i++)
                r[i] = cells[i] instanceof Double ? String.format(Locale.ROOT, "%.2f", cells[i]) : String.valueOf(cells[i]);
            rows.add(r);
        }
        void write(Path dir) throws IOException {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve(name + ".csv")))) {
                w.println(String.join(",", header));
                for (String[] r : rows) w.println(String.join(",", r));
            }
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve(name + ".md")))) {
                w.println("| " + String.join(" | ", header) + " |");
                w.println("|" + "---|".repeat(header.length));
                for (String[] r : rows) w.println("| " + String.join(" | ", r) + " |");
            }
            System.out.println("wrote " + name);
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "results", "tables");
        Files.createDirectories(dir);
        System.out.println("Java " + System.getProperty("java.version") + ", seed=" + SEED + ", reps=" + REPS);
        globalWarmup();

        Table mst = new Table("mst_kruskal_vs_prim", "Graph", "V", "E", "Algorithm", "Avg time (us)", "SD (us)", "MST weight",
                "Tree edges", "Connected", "Edges examined", "Comparisons", "DSU finds", "DSU unions", "Heap pushes", "Heap pops");
        Table topo = new Table("topological_sort", "Graph", "V", "E", "Input", "Avg time (us)", "SD (us)", "Acyclic", "Output vertices",
                "Edges examined", "Vertices processed");
        Table scc = new Table("scc", "Graph", "V", "E", "Input", "Avg time (us)", "SD (us)", "#SCC", "Largest SCC",
                "Edges examined", "Vertex visits");
        Table dij = new Table("dijkstra", "Graph", "V", "E", "Avg time (us)", "SD (us)", "Reachable", "Edges examined",
                "Relaxations", "Heap pushes", "Heap pops", "Comparisons");
        Table summary = new Table("time_summary", "Graph", "V", "E", "Kruskal", "Prim", "Topological", "SCC", "Dijkstra");

        for (int c = 0; c < CONFIGS.length; c++) {
            int v = CONFIGS[c][0], e = CONFIGS[c][1];
            String name = NAMES[c];
            Graph und = GraphGenerator.connectedUndirected(v, e, SEED + v, MAX_W);
            Graph dag = GraphGenerator.directedDag(v, e, SEED + 2 * v, MAX_W);
            Graph cyc = GraphGenerator.directedRandom(v, e, SEED + 3 * v, MAX_W);

            Stat k = measure(() -> Kruskal.run(und).metrics, WARMUP);
            Stat p = measure(() -> Prim.run(und).metrics, WARMUP);
            MSTResult kr = Kruskal.run(und), pr = Prim.run(und);
            mst.add(name, v, e, "Kruskal", k.us(), k.sd(), kr.totalWeight, kr.edges.size(), kr.connected, k.m.edgesExamined,
                    k.m.comparisons, k.m.dsuFinds, k.m.dsuUnions, 0, 0);
            mst.add(name, v, e, "Prim", p.us(), p.sd(), pr.totalWeight, pr.edges.size(), pr.connected, p.m.edgesExamined,
                    p.m.comparisons, 0, 0, p.m.heapPushes, p.m.heapPops);

            Stat td = measure(() -> TopologicalSort.kahn(dag).metrics, WARMUP);
            Stat tc = measure(() -> TopologicalSort.kahn(cyc).metrics, WARMUP);
            TopologicalSort.Result rd = TopologicalSort.kahn(dag), rc = TopologicalSort.kahn(cyc);
            topo.add(name, v, e, "DAG", td.us(), td.sd(), rd.acyclic, rd.order.size(), td.m.edgesExamined, td.m.vertexVisits);
            topo.add(name, v, e, "Random digraph (cyclic)", tc.us(), tc.sd(), rc.acyclic, rc.order.size(), tc.m.edgesExamined, tc.m.vertexVisits);

            Stat sd = measure(() -> StronglyConnectedComponents.kosaraju(dag).metrics, WARMUP);
            Stat sc = measure(() -> StronglyConnectedComponents.kosaraju(cyc).metrics, WARMUP);
            StronglyConnectedComponents.Result qd = StronglyConnectedComponents.kosaraju(dag),
                    qc = StronglyConnectedComponents.kosaraju(cyc);
            scc.add(name, v, e, "DAG", sd.us(), sd.sd(), qd.count(), qd.largest(), sd.m.edgesExamined, sd.m.vertexVisits);
            scc.add(name, v, e, "Random digraph (cyclic)", sc.us(), sc.sd(), qc.count(), qc.largest(), sc.m.edgesExamined, sc.m.vertexVisits);

            Stat dj = measure(() -> Dijkstra.run(cyc, 0).metrics, WARMUP);
            Dijkstra.Result dr = Dijkstra.run(cyc, 0);
            dij.add(name, v, e, dj.us(), dj.sd(), dr.reachableCount() + "/" + v, dj.m.edgesExamined, dj.m.relaxations,
                    dj.m.heapPushes, dj.m.heapPops, dj.m.comparisons);

            summary.add(name, v, e, k.us(), p.us(), td.us(), sc.us(), dj.us());
        }
        mst.write(dir); topo.write(dir); scc.write(dir); dij.write(dir); summary.write(dir);

        dsuExperiment().write(dir);
        scalingVertices().write(dir);
        scalingEdges().write(dir);
        System.out.println("(sink=" + sink + ")");
    }

    /** Run every algorithm many times on mid-sized graphs so the JIT has compiled them before timing starts. */
    static void globalWarmup() {
        for (int[] cfg : new int[][]{{200, 1000, 2000}, {1000, 5000, 300}}) {
            int v = cfg[0], e = cfg[1];
            Graph u = GraphGenerator.connectedUndirected(v, e, 1, MAX_W);
            Graph d = GraphGenerator.directedDag(v, e, 2, MAX_W);
            Graph c = GraphGenerator.directedRandom(v, e, 3, MAX_W);
            for (int i = 0; i < cfg[2]; i++) {
                sink += Kruskal.run(u).totalWeight + Prim.run(u).totalWeight;
                sink += TopologicalSort.kahn(d).order.size() + TopologicalSort.kahn(c).order.size();
                sink += StronglyConnectedComponents.kosaraju(c).count();
                sink += Dijkstra.run(c, 0).reachableCount();
            }
        }
    }

    // ---------------------------------------------------------------- DSU ablation
    static Table dsuExperiment() {
        Table t = new Table("dsu_ablation", "Workload", "n", "Variant", "Avg time (us)", "SD (us)", "Finds", "Unions", "Parent-pointer steps", "Steps per find");
        boolean[][] variants = {{false, false}, {true, false}, {false, true}, {true, true}};
        String[] vnames = {"naive", "path compression only", "union by rank only", "both"};
        String[] workloads = {"chain", "binomial", "random"};
        for (String w : workloads) for (int n : new int[]{1000, 10000, 20000}) for (int i = 0; i < 4; i++) {
            final boolean pc = variants[i][0], rk = variants[i][1];
            Stat s = measure(() -> {
                DSU d = new DSU(n, pc, rk);
                runWorkload(d, w, n);
                Metrics m = new Metrics();
                m.dsuFinds = d.findCalls; m.dsuUnions = d.unionCalls; m.pointerSteps = d.pointerSteps;
                m.edgesExamined = d.pointerSteps;
                return m;
            }, 1);
            t.add(w, n, vnames[i], s.us(), s.sd(), s.m.dsuFinds, s.m.dsuUnions, s.m.pointerSteps,
                    (double) s.m.pointerSteps / s.m.dsuFinds);
        }
        return t;
    }

    /** All workloads: build sets with n-1 (chain/binomial) or n (random) unions, then 3 passes of find over all elements. */
    static void runWorkload(DSU d, String w, int n) {
        switch (w) {
            case "chain":      // union(i, i+1): worst case for naive linking (one chain of length n)
                for (int i = 0; i + 1 < n; i++) d.union(i, i + 1);
                break;
            case "binomial":   // merge equal-sized sets pairwise: deepest trees that union-by-rank can produce
                for (int step = 1; step < n; step *= 2)
                    for (int i = 0; i + step < n; i += 2 * step) d.union(i, i + step);
                break;
            default: {         // random pairs
                Random r = new Random(SEED);
                for (int i = 0; i < n; i++) d.union(r.nextInt(n), r.nextInt(n));
            }
        }
        long acc = 0;
        for (int pass = 0; pass < 3; pass++) for (int i = 0; i < n; i++) acc += d.find(i);
        sink += acc;
    }

    // ---------------------------------------------------------------- scaling sweeps (extra data points for the plots)
    static Table scalingVertices() {
        Table t = new Table("scaling_vertices", "V", "E", "Kruskal", "Prim", "Topological", "SCC", "Dijkstra");
        for (int v : new int[]{250, 500, 1000, 2000, 4000, 8000, 16000}) addScalingRow(t, v, 5 * v);
        return t;
    }

    static Table scalingEdges() {
        Table t = new Table("scaling_edges", "V", "E", "Kruskal", "Prim", "Topological", "SCC", "Dijkstra");
        for (int e : new int[]{1000, 2000, 5000, 10000, 20000, 50000, 100000}) addScalingRow(t, 1000, e);
        return t;
    }

    static void addScalingRow(Table t, int v, int e) {
        Graph und = GraphGenerator.connectedUndirected(v, e, SEED + v + e, MAX_W);
        Graph dag = GraphGenerator.directedDag(v, e, SEED + 2 * v + e, MAX_W);
        Graph cyc = GraphGenerator.directedRandom(v, e, SEED + 3 * v + e, MAX_W);
        t.add(v, e,
                measure(() -> Kruskal.run(und).metrics, WARMUP).us(),
                measure(() -> Prim.run(und).metrics, WARMUP).us(),
                measure(() -> TopologicalSort.kahn(dag).metrics, WARMUP).us(),
                measure(() -> StronglyConnectedComponents.kosaraju(cyc).metrics, WARMUP).us(),
                measure(() -> Dijkstra.run(cyc, 0).metrics, WARMUP).us());
    }
}
