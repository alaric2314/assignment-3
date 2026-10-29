package app;

import connectivity.StronglyConnectedComponents;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import mst.Kruskal;
import mst.MSTResult;
import mst.Prim;
import shortestpath.Dijkstra;

import java.util.ArrayList;
import java.util.List;

/** Small end-to-end demo: a 7-district city used for planning, connectivity and routing. */
public final class CityDemo {
    static final String[] NAME = {"Downtown", "Harbor", "Airport", "University", "Old Town", "Tech Park", "Suburbs"};

    public static void main(String[] args) {
        // ---- Task 1: cheapest road network connecting all districts (undirected, weight = cost in M$)
        Graph roads = new Graph(7, false);
        int[][] r = {{0,1,7},{0,3,5},{1,2,8},{1,3,9},{1,4,7},{2,4,5},{3,4,15},{3,5,6},{4,5,8},{4,6,9},{5,6,11}};
        for (int[] e : r) roads.addEdge(e[0], e[1], e[2]);
        System.out.println("== Infrastructure planning (MST) ==");
        for (MSTResult res : new MSTResult[]{Kruskal.run(roads), Prim.run(roads)}) {
            System.out.printf("%-8s total cost = %d, connected = %b, edges = %s%n",
                    res.algorithm, res.totalWeight, res.connected, label(res.edges));
        }

        // ---- Task 2: directed service dependencies
        Graph deps = new Graph(7, true);
        int[][] d = {{6,0},{0,1},{0,3},{3,5},{1,4},{4,2},{5,4}};
        for (int[] e : d) deps.addEdge(e[0], e[1], 1);
        System.out.println("\n== Service dependencies (topological order) ==");
        TopologicalSort.Result t = TopologicalSort.kahn(deps);
        System.out.println("acyclic = " + t.acyclic + ", order = " + names(t.order));

        deps.addEdge(2, 0, 1);       // add a circular dependency: Airport -> Downtown
        t = TopologicalSort.kahn(deps);
        System.out.println("after adding Airport -> Downtown: acyclic = " + t.acyclic
                + ", blocked districts = " + names(t.unprocessed));
        StronglyConnectedComponents.Result scc = StronglyConnectedComponents.kosaraju(deps);
        System.out.println("SCCs:");
        for (List<Integer> c : scc.components) System.out.println("  " + names(c));

        // ---- Task 3: shortest routes from Downtown on a one-way road network (weight = km)
        Graph oneWay = new Graph(7, true);
        int[][] w = {{0,1,4},{0,3,2},{3,5,3},{1,2,6},{5,2,1},{3,1,1},{4,6,2}};
        for (int[] e : w) oneWay.addEdge(e[0], e[1], e[2]);
        System.out.println("\n== Shortest routes from " + NAME[0] + " (Dijkstra) ==");
        Dijkstra.Result sp = Dijkstra.run(oneWay, 0);
        for (int v = 0; v < 7; v++) {
            if (sp.reachable(v)) System.out.printf("%-10s %3d km  via %s%n", NAME[v], sp.dist[v], names(sp.pathTo(v)));
            else System.out.printf("%-10s unreachable%n", NAME[v]);
        }
    }

    private static String label(List<Edge> edges) {
        List<String> s = new ArrayList<>();
        for (Edge e : edges) s.add(NAME[e.from] + "-" + NAME[e.to] + "(" + e.weight + ")");
        return s.toString();
    }

    private static String names(List<Integer> vs) {
        List<String> s = new ArrayList<>();
        for (int v : vs) s.add(NAME[v]);
        return s.toString();
    }
}
