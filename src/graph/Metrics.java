package graph;

/** Operation counters shared by all algorithms (unused counters stay 0). */
public final class Metrics {
    public long edgesExamined;
    public long comparisons;
    public long vertexVisits;
    public long dsuFinds;
    public long dsuUnions;
    public long pointerSteps;   // parent-pointer hops inside DSU.find
    public long heapPushes;
    public long heapPops;
    public long relaxations;    // successful distance improvements (Dijkstra)
}
