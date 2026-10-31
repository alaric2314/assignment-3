package mst;
import dsu.DSU;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;

// Kruskal's algorithm: sort edges, add the cheapest edge that joins two different DSU sets.
public final class Kruskal {
    private Kruskal() {}

    public static MSTResult run(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST requires an undirected graph");
        int n = g.vertexCount();
        Metrics m = new Metrics();

        List<Edge> sorted = new ArrayList<>(g.edges());
        sorted.sort((a, b) -> { m.comparisons++; return Integer.compare(a.weight, b.weight); });

        DSU dsu = new DSU(n);
        List<Edge> tree = new ArrayList<>();
        long total = 0;
        for (Edge e : sorted) {
            if (tree.size() == n - 1) break;           // spanning tree complete: stop early
            m.edgesExamined++;
            if (dsu.union(e.from, e.to)) {
                tree.add(e);
                total += e.weight;
            }
        }
        m.dsuFinds = dsu.findCalls;
        m.dsuUnions = dsu.unionCalls;
        m.pointerSteps = dsu.pointerSteps;
        int comps = dsu.components();
        return new MSTResult("Kruskal", tree, total, n <= 1 || comps == 1, comps, m);
    }
}
