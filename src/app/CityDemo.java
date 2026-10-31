package app;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

public class CityDemo {

    static String[] names = {"Downtown", "Harbor", "Airport", "University", "Old Town", "Tech Park", "Suburbs"};

    public static void main(String[] args) {

        //Task 1: cheapest road network (Kruskal) 
        int[][] roads = {{0,1,7},{0,3,5},{1,2,8},{1,3,9},{1,4,7},{2,4,5},
                {3,4,15},{3,5,6},{4,5,8},{4,6,9},{5,6,11}};
        System.out.println("== Infrastructure planning (MST) ==");
        kruskal(roads, 7);

        //Task 2: service dependencies (topological sort)
        int[][] deps = {{6,0},{0,1},{0,3},{3,5},{1,4},{4,2},{5,4}};
        System.out.println("\n== Service dependencies ==");
        topologicalSort(deps, 7);

        // add a circular dependency: Airport -> Downtown
        int[][] depsWithCycle = {{6,0},{0,1},{0,3},{3,5},{1,4},{4,2},{5,4},{2,0}};
        System.out.println("\nAfter adding Airport -> Downtown:");
        topologicalSort(depsWithCycle, 7);

        //Task 3: shortest routes (Dijkstra)
        int[][] oneWay = {{0,1,4},{0,3,2},{3,5,3},{1,2,6},{5,2,1},{3,1,1},{4,6,2}};
        System.out.println("\n== Shortest routes from " + names[0] + " ==");
        dijkstra(oneWay, 7, 0);
    }

    //Kruskal 
    
    // helper: find the "leader" of a group of districts
    static int find(int[] parent, int x) {
        while (parent[x] != x) {
            x = parent[x];
        }
        return x;
    }

    static void kruskal(int[][] edges, int n) {
        // sort roads from cheapest to most expensive
        int[][] sorted = edges.clone();
        Arrays.sort(sorted, (a, b) -> a[2] - b[2]);

        // at the start every district is its own group
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        int total = 0;
        int count = 0;
        for (int[] e : sorted) {
            int a = find(parent, e[0]);
            int b = find(parent, e[1]);
            if (a != b) {              // different groups -> no cycle, take this road
                parent[a] = b;         // merge the groups
                total += e[2];
                count++;
                System.out.println("  " + names[e[0]] + " - " + names[e[1]] + " (" + e[2] + ")");
            }
        }
        System.out.println("Total cost = " + total + ", connected = " + (count == n - 1));
    }

    // Topological sort (Kahn) 
    static void topologicalSort(int[][] edges, int n) {
        // count how many things each district depends on
        int[] indegree = new int[n];
        for (int[] e : edges) {
            indegree[e[1]]++;
        }

        // start with districts that depend on nothing
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.add(i);
            }
        }

        List<Integer> order = new ArrayList<>();
        boolean[] done = new boolean[n];
        while (!queue.isEmpty()) {
            int u = queue.remove();
            order.add(u);
            done[u] = true;
            for (int[] e : edges) {
                if (e[0] == u) {
                    indegree[e[1]]--;
                    if (indegree[e[1]] == 0) {
                        queue.add(e[1]);
                    }
                }
            }
        }

        if (order.size() == n) {
            System.out.println("No cycle. Order: " + toNames(order));
        } else {
            List<Integer> blocked = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (!done[i]) blocked.add(i);
            }
            System.out.println("Cycle found! Blocked districts: " + toNames(blocked));
        }
    }

    // Dijkstra 
    static void dijkstra(int[][] edges, int n, int start) {
        int INF = Integer.MAX_VALUE;
        int[] dist = new int[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];
        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);
        dist[start] = 0;

        for (int round = 0; round < n; round++) {
            // pick the unvisited district with the smallest distance
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!visited[i] && dist[i] != INF && (u == -1 || dist[i] < dist[u])) {
                    u = i;
                }
            }
            if (u == -1) break;        // nothing reachable is left
            visited[u] = true;

            // try to improve distances through u
            for (int[] e : edges) {
                if (e[0] == u && dist[u] + e[2] < dist[e[1]]) {
                    dist[e[1]] = dist[u] + e[2];
                    prev[e[1]] = u;
                }
            }
        }

        for (int v = 0; v < n; v++) {
            if (dist[v] == INF) {
                System.out.println(names[v] + ": unreachable");
            } else {
                // build the path by walking backwards
                List<Integer> path = new ArrayList<>();
                for (int x = v; x != -1; x = prev[x]) {
                    path.add(0, x);
                }
                System.out.println(names[v] + ": " + dist[v] + " km via " + toNames(path));
            }
        }
    }

    // helper 
    static String toNames(List<Integer> list) {
        String s = "";
        for (int i = 0; i < list.size(); i++) {
            s += names[list.get(i)];
            if (i < list.size() - 1) s += " -> ";
        }
        return s;
    }
}
