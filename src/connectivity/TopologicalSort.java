package connectivity;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
//Topological ordering via Kahn's algorithm (repeatedly remove vertices of in-degree 0)

public final class TopologicalSort {
    private TopologicalSort() {}

    public static final class Result {
        public final boolean acyclic;
        public final List<Integer> order;        // complete order if acyclic, else the partial prefix
        public final List<Integer> unprocessed;  // empty if acyclic; else vertices blocked by a cycle
        public final Metrics metrics;

        Result(boolean acyclic, List<Integer> order, List<Integer> unprocessed, Metrics metrics) {
            this.acyclic = acyclic;
            this.order = order;
            this.unprocessed = unprocessed;
            this.metrics = metrics;
        }
    }

    public static Result kahn(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("topological order requires a directed graph");
        int n = g.vertexCount();
        Metrics m = new Metrics();
        int[] indeg = new int[n];
        for (Edge e : g.edges()) {
            indeg[e.to]++;
            m.edgesExamined++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int v = 0; v < n; v++) if (indeg[v] == 0) queue.add(v);

        List<Integer> order = new ArrayList<>(n);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            m.vertexVisits++;
            for (Edge e : g.neighbors(u)) {
                m.edgesExamined++;
                if (--indeg[e.to] == 0) queue.add(e.to);
            }
        }
        List<Integer> blocked = new ArrayList<>();
        if (order.size() != n) for (int v = 0; v < n; v++) if (indeg[v] > 0) blocked.add(v);
        return new Result(order.size() == n, order, blocked, m);
    }

    // True if order is a permutation of all vertices and every edge u->v has u before v. 
    public static boolean isValidOrder(Graph g, List<Integer> order) {
        int n = g.vertexCount();
        if (order.size() != n) return false;
        int[] pos = new int[n];
        java.util.Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) {
            int v = order.get(i);
            if (v < 0 || v >= n || pos[v] != -1) return false;
            pos[v] = i;
        }
        for (Edge e : g.edges()) if (pos[e.from] >= pos[e.to]) return false;
        return true;
    }
}
