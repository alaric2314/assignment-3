package mst;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

//Prim's algorithm with a binary-heap priority queue and lazy deletion
public final class Prim {
    private Prim() {}

    public static MSTResult run(Graph g) {
        if (g.isDirected()) throw new IllegalArgumentException("MST requires an undirected graph");
        int n = g.vertexCount();
        Metrics m = new Metrics();
        boolean[] inTree = new boolean[n];
        PriorityQueue<Edge> pq = new PriorityQueue<>((a, b) -> { m.comparisons++; return Integer.compare(a.weight, b.weight); });
        List<Edge> tree = new ArrayList<>();
        long total = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (inTree[start]) continue;
            components++;
            visit(g, start, inTree, pq, m);
            while (!pq.isEmpty()) {
                Edge e = pq.poll();         // e.from is already in the tree
                m.heapPops++;
                if (inTree[e.to]) continue; // stale entry
                tree.add(e);
                total += e.weight;
                visit(g, e.to, inTree, pq, m);
            }
        }
        return new MSTResult("Prim", tree, total, n <= 1 || components == 1, components, m);
    }

    private static void visit(Graph g, int u, boolean[] inTree, PriorityQueue<Edge> pq, Metrics m) {
        inTree[u] = true;
        m.vertexVisits++;
        for (Edge e : g.neighbors(u)) {
            m.edgesExamined++;
            if (!inTree[e.to]) {
                pq.add(e);
                m.heapPushes++;
            }
        }
    }
}
