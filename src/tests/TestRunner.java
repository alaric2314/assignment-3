package tests;

import connectivity.StronglyConnectedComponents;
import connectivity.TopologicalSort;
import dsu.DSU;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Dependency-free test runner (no JUnit needed). Exit code 1 if any check fails. */
public final class TestRunner {
    private static int passed, failed;

    public static void main(String[] args) {
        dsuTests();
        mstTests();
        topoTests();
        sccTests();
        dijkstraTests();
        generatorTests();
        System.out.printf("%n%d checks passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(boolean ok, String name) {
        if (ok) passed++; else { failed++; System.out.println("FAIL: " + name); }
    }

    // ------------------------------------------------------------------ DSU
    private static void dsuTests() {
        boolean[][] modes = {{false, false}, {true, false}, {false, true}, {true, true}};
        for (boolean[] md : modes) {
            String tag = "DSU(pc=" + md[0] + ",rank=" + md[1] + ")";
            DSU d = new DSU(10, md[0], md[1]);
            check(d.components() == 10, tag + " starts with n components");
            check(d.union(0, 1) && d.union(2, 3) && d.union(1, 3), tag + " unions succeed");
            check(!d.union(0, 2), tag + " union of already joined sets returns false");
            check(d.connected(0, 3) && !d.connected(0, 4), tag + " connectivity");
            check(d.components() == 7, tag + " component count");
        }
        Random r = new Random(1);
        for (int t = 0; t < 50; t++) {      // all four variants must agree with each other
            int n = 30;
            DSU[] ds = {new DSU(n, false, false), new DSU(n, true, false), new DSU(n, false, true), new DSU(n)};
            boolean agree = true;
            for (int i = 0; i < 40; i++) {
                int a = r.nextInt(n), b = r.nextInt(n);
                boolean first = ds[0].union(a, b);
                for (int k = 1; k < ds.length; k++) agree &= ds[k].union(a, b) == first;
            }
            for (int a = 0; a < n; a++) for (int b = 0; b < n; b++) {
                boolean c0 = ds[0].connected(a, b);
                for (DSU d : ds) agree &= d.connected(a, b) == c0;
            }
            check(agree, "DSU variants agree (random run " + t + ")");
        }
        DSU single = new DSU(1);
        check(single.find(0) == 0 && single.components() == 1, "DSU single element");
    }

    // ------------------------------------------------------------------ MST
    private static void mstTests() {
        // classic textbook graph, MST weight 37 (CLRS figure 23.4)
        Graph g = new Graph(9, false);
        int[][] es = {{0,1,4},{0,7,8},{1,2,8},{1,7,11},{2,3,7},{2,8,2},{2,5,4},{3,4,9},{3,5,14},
                      {4,5,10},{5,6,2},{6,7,1},{6,8,6},{7,8,7}};
        for (int[] e : es) g.addEdge(e[0], e[1], e[2]);
        MSTResult k = Kruskal.run(g), p = Prim.run(g);
        check(k.totalWeight == 37 && p.totalWeight == 37, "CLRS graph MST weight = 37");
        check(k.connected && p.connected, "CLRS graph connected");
        check(k.edges.size() == 8 && p.edges.size() == 8, "MST has V-1 edges");
        check(isSpanningTree(g, k.edges) && isSpanningTree(g, p.edges), "results are spanning trees");

        // single vertex
        Graph one = new Graph(1, false);
        check(Kruskal.run(one).totalWeight == 0 && Kruskal.run(one).connected, "single vertex: Kruskal");
        check(Prim.run(one).totalWeight == 0 && Prim.run(one).connected, "single vertex: Prim");

        // disconnected: two triangles + isolated vertex -> forest
        Graph dis = new Graph(7, false);
        dis.addEdge(0, 1, 1); dis.addEdge(1, 2, 2); dis.addEdge(0, 2, 3);
        dis.addEdge(3, 4, 5); dis.addEdge(4, 5, 4); dis.addEdge(3, 5, 6);
        MSTResult kd = Kruskal.run(dis), pd = Prim.run(dis);
        check(!kd.connected && !pd.connected, "disconnected graph detected by both");
        check(kd.components == 3 && pd.components == 3, "disconnected: 3 components");
        check(kd.totalWeight == 12 && pd.totalWeight == 12, "disconnected: forest weight 1+2+4+5 = 12");
        check(kd.edges.size() == 4, "forest has V - components edges");

        // multiple valid MSTs: 4-cycle with all weights equal (4 different MSTs) + triangle ties
        Graph sq = new Graph(4, false);
        sq.addEdge(0, 1, 5); sq.addEdge(1, 2, 5); sq.addEdge(2, 3, 5); sq.addEdge(3, 0, 5);
        check(Kruskal.run(sq).totalWeight == 15 && Prim.run(sq).totalWeight == 15, "all-equal weights: weight 15");
        check(isSpanningTree(sq, Kruskal.run(sq).edges) && isSpanningTree(sq, Prim.run(sq).edges), "ties: still spanning trees");

        // duplicate edge weights with a unique answer for the weight but not the tree
        Graph dup = new Graph(5, false);
        dup.addEdge(0, 1, 2); dup.addEdge(1, 2, 2); dup.addEdge(0, 2, 2);
        dup.addEdge(2, 3, 1); dup.addEdge(3, 4, 1); dup.addEdge(2, 4, 1);
        check(Kruskal.run(dup).totalWeight == 6 && Prim.run(dup).totalWeight == 6, "duplicate weights: weight 6");

        // empty graph / graph without edges (n isolated vertices)
        Graph iso = new Graph(3, false);
        check(!Kruskal.run(iso).connected && Kruskal.run(iso).components == 3, "3 isolated vertices not connected");

        // directed input rejected
        boolean threw = false;
        try { Kruskal.run(new Graph(2, true)); } catch (IllegalArgumentException ex) { threw = true; }
        check(threw, "Kruskal rejects directed graph");

        // brute force on tiny random graphs (weights 1..4 => many ties)
        Random r = new Random(7);
        int bruteOk = 0;
        for (int t = 0; t < 300; t++) {
            int n = 2 + r.nextInt(5);
            Graph h = new Graph(n, false);
            int m = r.nextInt(Math.min(11, n * (n - 1) / 2) + 1);
            for (int i = 0; i < m; i++) {
                int a = r.nextInt(n), b = r.nextInt(n);
                if (a != b) h.addEdge(a, b, 1 + r.nextInt(4));     // parallel edges allowed here
            }
            MSTResult a = Kruskal.run(h), b = Prim.run(h);
            boolean same = a.totalWeight == b.totalWeight && a.connected == b.connected && a.components == b.components;
            Long brute = bruteForceMst(h);
            boolean bruteMatch = a.connected ? (brute != null && brute == a.totalWeight) : brute == null;
            if (same && bruteMatch) bruteOk++;
        }
        check(bruteOk == 300, "Kruskal == Prim == brute force on 300 tiny random graphs (ok=" + bruteOk + ")");

        // random connected graphs of the experiment sizes
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] s : sizes) {
            boolean allEqual = true;
            for (int seed = 0; seed < 20; seed++) {
                Graph c = GraphGenerator.connectedUndirected(s[0], s[1], seed, 100);
                MSTResult a = Kruskal.run(c), b = Prim.run(c);
                allEqual &= a.connected && b.connected && a.totalWeight == b.totalWeight
                        && a.edges.size() == s[0] - 1 && isSpanningTree(c, a.edges) && isSpanningTree(c, b.edges);
            }
            check(allEqual, "Kruskal == Prim on 20 random connected graphs V=" + s[0] + " E=" + s[1]);
        }
    }

    private static boolean isSpanningTree(Graph g, List<Edge> tree) {
        int n = g.vertexCount();
        if (tree.size() != n - 1) return false;
        DSU d = new DSU(n);
        for (Edge e : tree) if (!d.union(e.from, e.to)) return false;   // a cycle would fail here
        return d.components() == 1;
    }

    /** Minimum weight over all (V-1)-edge subsets that form a spanning tree; null if none exists. */
    private static Long bruteForceMst(Graph g) {
        int n = g.vertexCount();
        List<Edge> es = g.edges();
        int m = es.size();
        Long best = null;
        for (int mask = 0; mask < (1 << m); mask++) {
            if (Integer.bitCount(mask) != n - 1) continue;
            DSU d = new DSU(n);
            long w = 0;
            boolean ok = true;
            for (int i = 0; i < m && ok; i++) if ((mask >> i & 1) == 1) {
                ok = d.union(es.get(i).from, es.get(i).to);
                w += es.get(i).weight;
            }
            if (ok && d.components() == 1 && (best == null || w < best)) best = w;
        }
        return best;
    }

    // ------------------------------------------------------------------ topological sort
    private static void topoTests() {
        Graph dag = new Graph(6, true);
        int[][] es = {{5,2},{5,0},{4,0},{4,1},{2,3},{3,1}};
        for (int[] e : es) dag.addEdge(e[0], e[1], 1);
        TopologicalSort.Result r = TopologicalSort.kahn(dag);
        check(r.acyclic && TopologicalSort.isValidOrder(dag, r.order), "known DAG: valid order " + r.order);

        Graph cyc = new Graph(4, true);
        cyc.addEdge(0, 1, 1); cyc.addEdge(1, 2, 1); cyc.addEdge(2, 1, 1); cyc.addEdge(2, 3, 1);
        TopologicalSort.Result rc = TopologicalSort.kahn(cyc);
        check(!rc.acyclic, "cycle 1->2->1 detected");
        check(rc.unprocessed.containsAll(Arrays.asList(1, 2, 3)) && rc.order.equals(List.of(0)), "cyclic: blocked vertices reported");

        Graph self = new Graph(2, true);
        self.addEdge(0, 0, 1);
        check(!TopologicalSort.kahn(self).acyclic, "self-loop is a cycle");

        check(TopologicalSort.kahn(new Graph(1, true)).acyclic, "single vertex is a DAG");
        TopologicalSort.Result noEdges = TopologicalSort.kahn(new Graph(5, true));
        check(noEdges.acyclic && noEdges.order.size() == 5, "no edges: any order, all 5 vertices");

        boolean ok = true;
        for (int s : new int[]{10, 100, 1000}) for (int seed = 0; seed < 25; seed++) {
            Graph d = GraphGenerator.directedDag(s, Math.min(5 * s, s * (s - 1) / 2), seed, 10);
            TopologicalSort.Result t = TopologicalSort.kahn(d);
            ok &= t.acyclic && TopologicalSort.isValidOrder(d, t.order);
        }
        check(ok, "75 random DAGs: order is always valid");

        boolean cycOk = true;
        for (int s : new int[]{10, 100, 1000}) for (int seed = 0; seed < 25; seed++) {
            Graph d = GraphGenerator.directedRandom(s, 5 * s, seed, 10);
            TopologicalSort.Result t = TopologicalSort.kahn(d);
            int sccCount = StronglyConnectedComponents.kosaraju(d).count();
            cycOk &= (t.acyclic == (sccCount == s));      // cyclic  <=>  some SCC has >= 2 vertices
        }
        check(cycOk, "75 random digraphs: Kahn's verdict agrees with SCC (acyclic iff all SCCs trivial)");
    }

    // ------------------------------------------------------------------ SCC
    private static void sccTests() {
        // textbook: {0,1,2}, {3,4}, {5}
        Graph g = new Graph(6, true);
        int[][] es = {{0,1},{1,2},{2,0},{2,3},{3,4},{4,3},{4,5}};
        for (int[] e : es) g.addEdge(e[0], e[1], 1);
        StronglyConnectedComponents.Result r = StronglyConnectedComponents.kosaraju(g);
        Set<Set<Integer>> got = new HashSet<>();
        for (List<Integer> c : r.components) got.add(new HashSet<>(c));
        Set<Set<Integer>> want = new HashSet<>(List.of(Set.of(0, 1, 2), Set.of(3, 4), Set.of(5)));
        check(got.equals(want), "known SCCs " + r.components);

        StronglyConnectedComponents.Result single = StronglyConnectedComponents.kosaraju(new Graph(1, true));
        check(single.count() == 1 && single.components.get(0).equals(List.of(0)), "single vertex: one SCC");

        Graph ring = new Graph(50, true);
        for (int i = 0; i < 50; i++) ring.addEdge(i, (i + 1) % 50, 1);
        check(StronglyConnectedComponents.kosaraju(ring).count() == 1, "directed ring: one SCC of 50");

        Graph dag = GraphGenerator.directedDag(100, 500, 3, 10);
        check(StronglyConnectedComponents.kosaraju(dag).count() == 100, "DAG: every vertex is its own SCC");

        // long path: iterative DFS must not overflow the stack
        Graph path = new Graph(200_000, true);
        for (int i = 0; i + 1 < 200_000; i++) path.addEdge(i, i + 1, 1);
        check(StronglyConnectedComponents.kosaraju(path).count() == 200_000, "200k-vertex path (no stack overflow)");

        // partition + mutual reachability brute force
        Random rnd = new Random(11);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            int n = 1 + rnd.nextInt(12);
            Graph h = new Graph(n, true);
            int m = rnd.nextInt(n * 2 + 1);
            for (int i = 0; i < m; i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a != b) h.addEdge(a, b, 1);
            }
            StronglyConnectedComponents.Result s = StronglyConnectedComponents.kosaraju(h);
            int total = 0;
            boolean[] seen = new boolean[n];
            for (List<Integer> c : s.components) for (int v : c) { total++; if (seen[v]) ok = false; seen[v] = true; }
            ok &= total == n;                                         // each vertex in exactly one SCC
            boolean[][] reach = closure(h);
            for (int a = 0; a < n; a++) for (int b = 0; b < n; b++)
                ok &= (s.componentOf[a] == s.componentOf[b]) == (reach[a][b] && reach[b][a]);
        }
        check(ok, "200 random digraphs: partition + SCC == mutual reachability");
    }

    private static boolean[][] closure(Graph g) {
        int n = g.vertexCount();
        boolean[][] r = new boolean[n][n];
        for (int i = 0; i < n; i++) r[i][i] = true;
        for (Edge e : g.edges()) r[e.from][e.to] = true;
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
            if (r[i][k] && r[k][j]) r[i][j] = true;
        return r;
    }

    // ------------------------------------------------------------------ Dijkstra
    private static void dijkstraTests() {
        Graph g = new Graph(6, true);
        int[][] es = {{0,1,7},{0,2,9},{0,5,14},{1,2,10},{1,3,15},{2,3,11},{2,5,2},{5,4,9},{3,4,6}};
        for (int[] e : es) g.addEdge(e[0], e[1], e[2]);
        Dijkstra.Result r = Dijkstra.run(g, 0);
        check(Arrays.equals(r.dist, new long[]{0, 7, 9, 20, 20, 11}), "known distances " + Arrays.toString(r.dist));
        check(r.pathTo(4).equals(List.of(0, 2, 5, 4)), "path to 4 = " + r.pathTo(4));

        // unreachable vertices
        Graph u = new Graph(5, true);
        u.addEdge(0, 1, 3); u.addEdge(1, 2, 4); u.addEdge(3, 4, 1);
        Dijkstra.Result ru = Dijkstra.run(u, 0);
        check(ru.reachable(2) && !ru.reachable(3) && !ru.reachable(4), "unreachable vertices flagged");
        check(ru.dist[3] == Dijkstra.INF && ru.pathTo(3).isEmpty(), "unreachable: INF distance, empty path");
        check(ru.reachableCount() == 3, "reachable count");

        // single vertex, zero weights, undirected usage
        Dijkstra.Result one = Dijkstra.run(new Graph(1, true), 0);
        check(one.dist[0] == 0 && one.pathTo(0).equals(List.of(0)), "single vertex");
        Graph z = new Graph(3, false);
        z.addEdge(0, 1, 0); z.addEdge(1, 2, 0);
        check(Dijkstra.run(z, 2).dist[0] == 0, "zero-weight undirected edges");

        // negative weights: checked version rejects, unchecked version gives a wrong answer
        Graph neg = new Graph(4, true);               // s=0, a=1, b=2, c=3
        neg.addEdge(0, 1, 1); neg.addEdge(0, 2, 2); neg.addEdge(2, 1, -2); neg.addEdge(1, 3, 1);
        boolean threw = false;
        try { Dijkstra.run(neg, 0); } catch (IllegalArgumentException ex) { threw = true; }
        check(threw, "negative weight rejected by run()");
        long wrong = Dijkstra.runUnchecked(neg, 0).dist[3];
        check(wrong == 2 && bellmanFord(neg, 0)[3] == 1, "negative-edge demo: Dijkstra says dist(c)=2, true value is 1");

        // random cross-check against Bellman-Ford + path consistency
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            int n = 1 + rnd.nextInt(15);
            Graph h = new Graph(n, true);
            int m = rnd.nextInt(n * 3 + 1);
            for (int i = 0; i < m; i++) {
                int a = rnd.nextInt(n), b = rnd.nextInt(n);
                if (a != b) h.addEdge(a, b, rnd.nextInt(20));   // includes weight 0 and parallel edges
            }
            int src = rnd.nextInt(n);
            Dijkstra.Result d = Dijkstra.run(h, src);
            long[] bf = bellmanFord(h, src);
            ok &= Arrays.equals(d.dist, bf);
            for (int v = 0; v < n; v++) if (d.reachable(v)) ok &= pathWeight(h, d.pathTo(v)) == d.dist[v];
        }
        check(ok, "200 random digraphs: Dijkstra == Bellman-Ford, and path weights match distances");
    }

    private static long[] bellmanFord(Graph g, int s) {
        long[] d = new long[g.vertexCount()];
        Arrays.fill(d, Dijkstra.INF);
        d[s] = 0;
        for (int i = 0; i < g.vertexCount(); i++)
            for (Edge e : g.edges())
                if (d[e.from] != Dijkstra.INF && d[e.from] + e.weight < d[e.to]) d[e.to] = d[e.from] + e.weight;
        return d;
    }

    private static long pathWeight(Graph g, List<Integer> path) {
        long total = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            long best = Long.MAX_VALUE;
            for (Edge e : g.neighbors(path.get(i))) if (e.to == path.get(i + 1)) best = Math.min(best, e.weight);
            total += best;
        }
        return total;
    }

    // ------------------------------------------------------------------ generators
    private static void generatorTests() {
        Graph a = GraphGenerator.connectedUndirected(100, 500, 42, 100);
        Graph b = GraphGenerator.connectedUndirected(100, 500, 42, 100);
        boolean same = a.edgeCount() == 500;
        for (int i = 0; i < 500; i++) {
            Edge x = a.edges().get(i), y = b.edges().get(i);
            same &= x.from == y.from && x.to == y.to && x.weight == y.weight;
        }
        check(same, "same seed => identical graph; exact edge count");
        List<Edge> es = new ArrayList<>(a.edges());
        Set<Long> keys = new HashSet<>();
        boolean simple = true;
        for (Edge e : es) simple &= e.from != e.to && keys.add((long) Math.min(e.from, e.to) * 100 + Math.max(e.from, e.to));
        check(simple, "no self-loops or parallel edges");
    }
}
