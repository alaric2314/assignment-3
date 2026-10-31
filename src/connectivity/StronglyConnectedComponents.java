package connectivity;
import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//Kosaraju's algorithm (iterative DFS, so deep graphs cannot overflow the Java stack)
public final class StronglyConnectedComponents {
    private StronglyConnectedComponents() {}

    public static final class Result {
        public final List<List<Integer>> components;
        public final int[] componentOf;
        public final Metrics metrics;

        Result(List<List<Integer>> components, int[] componentOf, Metrics metrics) {
            this.components = components;
            this.componentOf = componentOf;
            this.metrics = metrics;
        }

        public int count() { return components.size(); }

        public int largest() {
            int best = 0;
            for (List<Integer> c : components) best = Math.max(best, c.size());
            return best;
        }
    }

    public static Result kosaraju(Graph g) {
        if (!g.isDirected()) throw new IllegalArgumentException("SCC requires a directed graph");
        int n = g.vertexCount();
        Metrics m = new Metrics();

        // Pass 1: finishing order on G
        boolean[] visited = new boolean[n];
        int[] finish = new int[n];
        int finished = 0;
        int[] stack = new int[n];
        int[] next = new int[n];                       // next adjacency index to explore per vertex
        for (int s = 0; s < n; s++) {
            if (visited[s]) continue;
            int sp = 0;
            stack[sp++] = s;
            visited[s] = true;
            m.vertexVisits++;
            while (sp > 0) {
                int u = stack[sp - 1];
                List<Edge> out = g.neighbors(u);
                if (next[u] < out.size()) {
                    Edge e = out.get(next[u]++);
                    m.edgesExamined++;
                    if (!visited[e.to]) {
                        visited[e.to] = true;
                        m.vertexVisits++;
                        stack[sp++] = e.to;
                    }
                } else {
                    sp--;
                    finish[finished++] = u;
                }
            }
        }

        // Pass 2: DFS on the transpose in decreasing finishing time
        Graph gt = g.reversed();
        int[] comp = new int[n];
        java.util.Arrays.fill(comp, -1);
        List<List<Integer>> components = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            int s = finish[i];
            if (comp[s] != -1) continue;
            int id = components.size();
            List<Integer> members = new ArrayList<>();
            int sp = 0;
            stack[sp++] = s;
            comp[s] = id;
            while (sp > 0) {
                int u = stack[--sp];
                members.add(u);
                m.vertexVisits++;
                for (Edge e : gt.neighbors(u)) {
                    m.edgesExamined++;
                    if (comp[e.to] == -1) {
                        comp[e.to] = id;
                        stack[sp++] = e.to;
                    }
                }
            }
            Collections.sort(members);
            components.add(members);
        }
        return new Result(components, comp, m);
    }
}
