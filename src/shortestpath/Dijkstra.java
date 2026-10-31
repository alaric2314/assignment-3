package shortestpath;

import graph.Edge;
import graph.Graph;
import graph.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

// Dijkstra's single-source shortest paths with a binary heap (lazy deletion).
 
public final class Dijkstra {
    public static final long INF = Long.MAX_VALUE;

    private Dijkstra() {}

    public static final class Result {
        public final int source;
        public final long[] dist;
        public final int[] prev;
        public final Metrics metrics;

        Result(int source, long[] dist, int[] prev, Metrics metrics) {
            this.source = source;
            this.dist = dist;
            this.prev = prev;
            this.metrics = metrics;
        }

        public boolean reachable(int v) { return dist[v] != INF; }

        public int reachableCount() {
            int c = 0;
            for (long d : dist) if (d != INF) c++;
            return c;
        }

        // Vertex sequence source..target, or an empty list if target is unreachable. 
        public List<Integer> pathTo(int target) {
            List<Integer> path = new ArrayList<>();
            if (!reachable(target)) return path;
            for (int v = target; v != -1; v = prev[v]) path.add(v);
            Collections.reverse(path);
            return path;
        }
    }

    public static Result run(Graph g, int source) {
        for (Edge e : g.edges())
            if (e.weight < 0)
                throw new IllegalArgumentException("Dijkstra requires non-negative weights, found " + e);
        return compute(g, source);
    }

    // NOT for production use: skips the negative-weight check to show the algorithm failing. 
    public static Result runUnchecked(Graph g, int source) {
        return compute(g, source);
    }

    private static Result compute(Graph g, int source) {
        int n = g.vertexCount();
        if (source < 0 || source >= n) throw new IllegalArgumentException("bad source " + source);
        Metrics m = new Metrics();
        long[] dist = new long[n];
        int[] prev = new int[n];
        boolean[] settled = new boolean[n];
        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);
        dist[source] = 0;

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> { m.comparisons++; return Long.compare(a[0], b[0]); });
        pq.add(new long[]{0, source});
        m.heapPushes++;
        while (!pq.isEmpty()) {
            long[] top = pq.poll();
            m.heapPops++;
            int u = (int) top[1];
            if (settled[u]) continue; // stale entry
            settled[u] = true;
            m.vertexVisits++;
            for (Edge e : g.neighbors(u)) {
                m.edgesExamined++;
                long nd = dist[u] + e.weight;
                if (nd < dist[e.to]) {
                    dist[e.to] = nd;
                    prev[e.to] = u;
                    m.relaxations++;
                    pq.add(new long[]{nd, e.to});
                    m.heapPushes++;
                }
            }
        }
        return new Result(source, dist, prev, m);
    }
}
