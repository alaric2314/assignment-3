package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Reproducible random graph generators. All use java.util.Random with an explicit seed,
 * no self-loops and no parallel edges. Weights are uniform integers in [1, maxW].
 */
public final class GraphGenerator {
    private GraphGenerator() {}

    /**
     * Connected undirected graph: random spanning tree first (guarantees connectivity),
     * then random extra distinct edges until exactly e edges exist.
     */
    public static Graph connectedUndirected(int v, int e, long seed, int maxW) {
        if (v < 1 || e < v - 1 || e > (long) v * (v - 1) / 2)
            throw new IllegalArgumentException("need V-1 <= E <= V(V-1)/2");
        Random r = new Random(seed);
        Graph g = new Graph(v, false);
        Set<Long> used = new HashSet<>();
        int[] perm = shuffled(v, r);
        for (int i = 1; i < v; i++) {
            int a = perm[i], b = perm[r.nextInt(i)];
            used.add(key(a, b, v, false));
            g.addEdge(a, b, 1 + r.nextInt(maxW));
        }
        while (g.edgeCount() < e) {
            int a = r.nextInt(v), b = r.nextInt(v);
            if (a != b && used.add(key(a, b, v, false))) g.addEdge(a, b, 1 + r.nextInt(maxW));
        }
        return g;
    }

    /**
     * Directed acyclic graph: a random vertex permutation defines a hidden topological order;
     * every edge goes from an earlier to a later vertex in that order, so no cycle can exist.
     */
    public static Graph directedDag(int v, int e, long seed, int maxW) {
        if (v < 1 || e > (long) v * (v - 1) / 2) throw new IllegalArgumentException("E too large for a DAG");
        Random r = new Random(seed);
        Graph g = new Graph(v, true);
        Set<Long> used = new HashSet<>();
        int[] perm = shuffled(v, r);
        while (g.edgeCount() < e) {
            int i = r.nextInt(v), j = r.nextInt(v);
            if (i == j) continue;
            if (i > j) { int t = i; i = j; j = t; }
            int a = perm[i], b = perm[j];
            if (used.add(key(a, b, v, true))) g.addEdge(a, b, 1 + r.nextInt(maxW));
        }
        return g;
    }

    /**
     * General random directed graph (uniformly random ordered pairs). With E >= V it almost
     * surely contains cycles; callers verify this with SCC / topological sort.
     */
    public static Graph directedRandom(int v, int e, long seed, int maxW) {
        if (v < 1 || e > (long) v * (v - 1)) throw new IllegalArgumentException("E too large");
        Random r = new Random(seed);
        Graph g = new Graph(v, true);
        Set<Long> used = new HashSet<>();
        while (g.edgeCount() < e) {
            int a = r.nextInt(v), b = r.nextInt(v);
            if (a != b && used.add(key(a, b, v, true))) g.addEdge(a, b, 1 + r.nextInt(maxW));
        }
        return g;
    }

    private static int[] shuffled(int n, Random r) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        return p;
    }

    private static long key(int a, int b, int v, boolean directed) {
        if (!directed && a > b) { int t = a; a = b; b = t; }
        return (long) a * v + b;
    }
}
