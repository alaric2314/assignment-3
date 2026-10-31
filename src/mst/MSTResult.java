package mst;
import graph.Edge;
import graph.Metrics;
import java.util.List;

// Result of an MST run. For disconnected graphs this is a minimum spanning FOREST.
public final class MSTResult {
    public final String algorithm;
    public final List<Edge> edges;
    public final long totalWeight;
    public final boolean connected;
    public final int components;
    public final Metrics metrics;

    public MSTResult(String algorithm, List<Edge> edges, long totalWeight,
                     boolean connected, int components, Metrics metrics) {
        this.algorithm = algorithm;
        this.edges = edges;
        this.totalWeight = totalWeight;
        this.connected = connected;
        this.components = components;
        this.metrics = metrics;
    }
}
