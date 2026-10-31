package graph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Weighted graph stored as an adjacency list plus a flat edge list.
 * Vertices are 0..n-1 (city districts); edges are roads / infrastructure links.
 * Undirected graphs store each edge once in {@link #edges()} (needed by Kruskal) and twice in the adjacency lists (needed by Prim / traversals).
 */
public final class Graph {
    private final int n;
    private final boolean directed;
    private final List<List<Edge>> adj;
    private final List<Edge> edges = new ArrayList<>();

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        this.n = n;
        this.directed = directed;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int u, int v, int weight) {
        checkVertex(u);
        checkVertex(v);
        Edge e = new Edge(u, v, weight);
        edges.add(e);
        adj.get(u).add(e);
        if (!directed) adj.get(v).add(new Edge(v, u, weight));
    }

    public int vertexCount() { return n; }
    public int edgeCount() { return edges.size(); }
    public boolean isDirected() { return directed; }
    public List<Edge> neighbors(int u) { return Collections.unmodifiableList(adj.get(u)); }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }

    // Transpose graph (all edges reversed). Only defined for directed graphs.
    public Graph reversed() {
        if (!directed) throw new IllegalStateException("reversed() requires a directed graph");
        Graph r = new Graph(n, true);
        for (Edge e : edges) r.addEdge(e.to, e.from, e.weight);
        return r;
    }

    private void checkVertex(int v) {
        if (v < 0 || v >= n) throw new IllegalArgumentException("vertex out of range: " + v);
    }
}
