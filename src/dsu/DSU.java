package dsu;

//Disjoint Set Union (union-find).
public final class DSU {
    private final int[] parent;
    private final int[] rank;
    private final boolean pathCompression;
    private final boolean unionByRank;
    private int components;

    public long findCalls;
    public long unionCalls;
    public long pointerSteps;

    public DSU(int n) { this(n, true, true); }

    public DSU(int n, boolean pathCompression, boolean unionByRank) {
        this.parent = new int[n];
        this.rank = new int[n];
        this.pathCompression = pathCompression;
        this.unionByRank = unionByRank;
        this.components = n;
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    // Returns the representative of x's set (iterative, two-pass; no recursion depth issues). 
    public int find(int x) {
        findCalls++;
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
            pointerSteps++;
        }
        if (pathCompression) {
            while (parent[x] != root) {
                int next = parent[x];
                parent[x] = root;
                x = next;
            }
        }
        return root;
    }

    // Merges the sets of a and b. Returns false if they were already in the same set.
    public boolean union(int a, int b) {
        unionCalls++;
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (unionByRank) {
            if (rank[ra] < rank[rb]) { int t = ra; ra = rb; rb = t; }
            parent[rb] = ra;
            if (rank[ra] == rank[rb]) rank[ra]++;
        } else {
            parent[ra] = rb; // naive linking: can build chains of length n
        }
        components--;
        return true;
    }

    public boolean connected(int a, int b) { return find(a) == find(b); }
    public int components() { return components; }
}
