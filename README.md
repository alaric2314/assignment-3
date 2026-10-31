# Assignment 3 — Smart City Infrastructure Network (Java)

A Java application that models a city as a weighted graph and implements and tests the graph algorithms a city planner needs:

| Task | Question for the planner | Algorithms |
|---|---|---|
| 1 | What is the cheapest road network that connects every district? | Kruskal (with DSU), Prim (binary heap) |
| 2 | Which services depend on each other? Is there a circular dependency? | Topological sort (Kahn), Strongly Connected Components (Kosaraju) |
| 3 | What is the shortest route from a district to every other one? | Dijkstra (binary heap) |

Everything is plain, simple Java (arrays, loops, `ArrayList`, `PriorityQueue`; no external libraries). Tests use a small built-in runner, so the project builds with just a JDK.

## Contents

1. [Overview](#1-overview) · 2. [Implementation](#2-implementation) · 3. [Complexity Analysis](#3-complexity-analysis) · 4. [Testing](#4-testing) · 5. [Conclusion](#5-conclusion) · [How to build and run](#how-to-build-and-run)

## Repository layout

```
assignment-3/
├── src/
│   ├── graph/          Graph, Edge, GraphGenerator (seeded)
│   ├── dsu/            DSU (find/union, path compression, union by rank, switchable)
│   ├── mst/            Kruskal, Prim
│   ├── connectivity/   TopologicalSort (Kahn), SCC (Kosaraju)
│   ├── shortestpath/   Dijkstra
│   ├── tests/          TestRunner (62 checks, no JUnit needed)
│   └── app/            CityDemo (stand-alone example with named districts)
├── results/            test_output.txt, demo_output.txt (saved program output)
├── build.sh, run_all.sh
└── README.md           (this report)
```

---

## 1. Overview

**Model.** A city is a graph: vertices `0..V-1` are districts; edges are roads or infrastructure links; the integer weight is a construction cost (Task 1) or a distance (Task 3). Task 1 uses an **undirected** graph; Tasks 2 and 3 use **directed** graphs (one-way roads, service dependencies).

**Representation.** `Graph` stores an **adjacency list** (`List<List<Edge>>`, used by Prim, Kahn, Kosaraju, Dijkstra) *and* a flat **edge list** (used by Kruskal, which must sort all edges). An undirected edge is stored once in the edge list and twice in the adjacency lists. Memory is `O(V + E)`; iterating the neighbours of `u` costs `O(deg(u))`. An adjacency list is the right choice because all test graphs are sparse (`E = 3V … 5V`, far below `V²`).

**Results.** Every algorithm class keeps its results (total weight, order, components, distances...) in `public static` fields that are filled by `run(...)`. (Some classes also keep counters such as comparisons or edges examined; they are optional extras and are not needed to understand the algorithms.)

**Demo.** `java -cp out app.CityDemo` runs all three tasks on a 7-district city (output in `results/demo_output.txt`). It is deliberately stand-alone and written with the most basic approach (plain edge arrays, simple loops, so it is slower than the library classes): Kruskal finds a road network of total cost 39, an added circular dependency (Airport → Downtown) is detected by the topological sort and the blocked districts are listed, and Dijkstra reports shortest routes and unreachable districts.

## 2. Implementation

### DSU (`dsu/DSU.java`)
* `find(x)` is **iterative and two-pass**: pass 1 walks to the root; pass 2 (path compression) re-points every node on the path directly at the root. No recursion, so a long chain cannot overflow the stack.
* `union(a, b)` finds both roots, returns `false` if they are equal (this is how Kruskal detects a cycle), otherwise attaches the root of **lower rank** below the other (union by rank) and maintains the number of components.
* Both optimisations can be switched off through the constructor (used by the tests to check that all four variants give the same answers). The algorithms always use the default (both on).
* Optional counters: `finds`, `unions`, `steps` (number of parent hops).

### Kruskal (`mst/Kruskal.java`)
Copies the edge list, sorts it by weight, then scans it with a DSU. It **stops as soon as `V-1` edges are chosen** (the remaining edges cannot be useful), which is why it often examines fewer than `E` edges. For a disconnected graph the loop finishes without reaching `V-1` edges, the DSU reports more than one component, and the result is flagged `connected = false` — the returned edges then form a minimum spanning *forest*.

### Prim (`mst/Prim.java`)
Binary-heap (`PriorityQueue`) implementation with **lazy deletion**: when a vertex joins the tree, all edges to non-tree vertices are pushed; when an edge is popped whose far end is already in the tree it is simply skipped. If the queue empties before all vertices are visited, Prim **restarts from the next unvisited vertex** and counts one more component, giving the same minimum spanning forest as Kruskal and the same connectivity verdict.

### Topological ordering (`connectivity/TopologicalSort.java`)
Kahn's algorithm: compute in-degrees, repeatedly output a vertex of in-degree 0 and decrement its successors. If fewer than `V` vertices were output, the graph contains a cycle: the result says `acyclic = false`, returns no complete order, and lists the **blocked vertices** (those on, or reachable only through, a cycle). `isValidOrder` independently verifies an order (permutation + every edge goes forward) and is used in the tests.

### SCC (`connectivity/SCC.java`)
Kosaraju: DFS on `G` to record finishing order, then DFS on the transpose graph in decreasing finishing order; each tree of the second pass is one SCC. Both DFS passes are written **recursively** (the simplest form). Java's default stack is enough for graphs of a few thousand vertices; for much bigger inputs run it in a thread with a larger stack. Result (static fields): list of components (vertices sorted), `componentOf[v]`, `count()` and `largest()`.

### Dijkstra (`shortestpath/Dijkstra.java`)
Binary-heap with lazy deletion and a `settled[]` array (every vertex is expanded once). Results (static fields): `dist[]` (`Long.MAX_VALUE` = unreachable) and `prev[]` for **path reconstruction**; helper methods `reachableCount()` and `pathTo(v)` (empty list when unreachable). `run()` **throws `IllegalArgumentException` on any negative weight**; `runUnchecked()` exists only to demonstrate the failure (below).

**Why Dijkstra fails with negative edges.** Dijkstra relies on the invariant *"the vertex with the smallest tentative distance can no longer be improved"*: any other route to it must pass through a vertex that is at least as far away, and adding non-negative edges can only make it longer. A negative edge breaks this: a vertex that was already settled can later be reached more cheaply through a vertex settled after it, but it is never expanded again, so the error propagates to its successors. Counter-example (`s→a:1, s→b:2, b→a:−2, a→c:1`): Dijkstra settles `a` at distance 1 and then sets `c = 2`; the true shortest path `s→b→a→c` costs `2 − 2 + 1 = 1`. This exact graph is a test: `runUnchecked` returns 2, Bellman–Ford returns 1. Suitable alternatives are Bellman–Ford `O(V·E)` (also detects negative cycles) or Johnson's re-weighting; with a negative *cycle* shortest paths are not even defined.

### Graph generation (`graph/GraphGenerator.java`)
All generators use `java.util.Random` with an explicit seed, never create self-loops or parallel edges, and draw integer weights uniformly from `[1, 100]`.

| Generator | Method | Cycles? |
|---|---|---|
| `connectedUndirected(V,E)` | random permutation → random spanning tree (each vertex joins a random earlier one) → add random distinct extra edges until exactly `E` | undirected cycles yes; **always connected** by construction |
| `directedDag(V,E)` | hidden random vertex order; each edge goes from an earlier to a later vertex | **never** (acyclic by construction) |
| `directedRandom(V,E)` | `E` distinct uniformly random ordered pairs | **yes** — in practice it has cycles; the tests check that Kahn's algorithm and the SCC algorithm agree about this on 75 random graphs |

## 3. Complexity Analysis

**Assumptions.** Adjacency-list graph; `V` vertices, `E` edges; binary heap (`java.util.PriorityQueue`, `O(log n)` push/pop, no decrease-key, hence lazy deletion); comparison-based sort for Kruskal; `α` is the inverse Ackermann function (`α(n) ≤ 4` for any practical `n`); "space" counts auxiliary memory beyond the input graph.

| Algorithm | Time | Space | Depends on V / E how? |
|---|---|---|---|
| **DSU** `find` / `union` (both optimisations) | `O(α(n))` amortised per operation; `m` operations on `n` elements: `O(m·α(n))` | `O(n)` | Independent of `E`; grows only with the number of elements `n`, and so slowly it is effectively constant |
| DSU, union by rank only | `O(log n)` worst case per operation | `O(n)` | Tree height ≤ log₂ n |
| DSU, path compression only | `O(log n)` amortised | `O(n)` | |
| DSU, no optimisation | `O(n)` worst case per operation | `O(n)` | Chains of length n are possible |
| **Kruskal** | sort `O(E log E)` + `E` DSU ops `O(E·α(V))` = **`O(E log E)` = `O(E log V)`** (since `E ≤ V²`, `log E ≤ 2 log V`) | `O(V + E)` (sorted copy of the edge list + DSU) | Sorting dominates, so cost follows `E`; `V` matters only through the DSU, which is nearly free. Dense graphs (`E≈V²`) cost `O(V² log V)` |
| **Prim** (binary heap, lazy) | each edge pushed at most once and popped once: `O(E log E)` = **`O(E log V)`** | `O(V + E)` (heap may hold up to `E` entries) | Same bound as Kruskal; with decrease-key and a Fibonacci heap `O(E + V log V)` is possible, which only pays off on dense graphs |
| **Topological sort** (Kahn) | **`O(V + E)`**: every vertex enqueued once, every edge examined twice (in-degree pass + removal) | `O(V)` (in-degree array, queue, output) | Linear in both; no log factor |
| **SCC** (Kosaraju) | two DFS passes plus building the transpose: **`O(V + E)`** | `O(V + E)` (transpose graph) + `O(V)` arrays and recursion stack | Linear in both; ≈2× the constant of a single traversal. Tarjan avoids the transpose |
| **Dijkstra** (binary heap, lazy) | each successful relaxation pushes once: `O((V + E) log V)`, i.e. **`O(E log V)`** for connected graphs. An array-based version is `O(V²)` (better for very dense graphs) | `O(V + E)` worst case (heap may hold `E` entries); `O(V)` for `dist`/`prev`/`settled` | Both matter: `E` relaxations, each possibly a `log V` heap operation. In practice with random weights far fewer than `E` relaxations succeed, so the heap work is often smaller than the bound |

**Remarks on how `V` and `E` affect each algorithm**

* For connected graphs `E ≥ V − 1`, so `O(V + E) = O(E)`. For sparse road-like graphs (`E` about `3V…5V`) `V` and `E` grow together; for dense graphs `E` can reach `V²`, and then the `E log V` algorithms become much more expensive than the linear ones.
* **Why the two DSU optimisations help.** *Union by rank* always hangs the shorter tree under the taller one, so a tree with `n` elements has height at most `log₂ n` and no `find` can be long. *Path compression* makes every element visited by a `find` point directly at the root, so later finds on the same elements take one step. Without them, unions like `union(0,1), union(1,2), ...` build a chain of length `n` and one `find` costs `O(n)`. Together they give the almost-constant amortised `O(α(n))` per operation.
* Kruskal and Prim have the same asymptotic bound but different constants and different *best cases*: Kruskal always sorts all `E` edges, but can skip the scan of the heavy tail; Prim's heap grows with `E` and every pushed edge is also popped.
* Kahn and Kosaraju have no logarithmic factor, so they scale roughly linearly in `V + E` and are the cheapest algorithms here.

## 4. Testing

`java -cp out tests.TestRunner` runs **62 checks, all passing** (`results/test_output.txt`). No framework is needed.

| Required test case | What is tested |
|---|---|
| Small graphs | textbook MST graph (CLRS, weight 37), textbook SCC graph (`{0,1,2},{3,4},{5}`), textbook shortest-path graph, textbook DAG |
| Disconnected graphs | MST: two triangles + isolated vertex → `connected = false`, 3 components, forest weight 12 (both algorithms); 3 isolated vertices; a DAG with no edges; SCC / Dijkstra on disconnected inputs |
| Graphs containing cycles | Kahn rejects `1→2→1` and a self-loop, and is run on 75 random digraphs; for those 75 digraphs Kahn's verdict must equal "some SCC has ≥ 2 vertices" (independent cross-check) |
| Multiple valid MSTs | 4-cycle with all weights equal (4 different MSTs): both algorithms return weight 15 and a valid spanning tree |
| Unreachable vertices | Dijkstra: `INF` distance, empty path, correct `reachableCount` |
| Duplicate edge weights | MST with ties (weight 6); 300 random tiny graphs with weights in 1…4 (many ties); random graphs with weights in 1…100 |
| Single vertex | MST (weight 0, connected), DSU, topological order, SCC (one component), Dijkstra (`dist = 0`) |

**Correctness properties verified**

* **Kruskal = Prim:** same weight, edge count and connectivity on 300 tiny random graphs, **additionally checked against brute force** (minimum over all `(V−1)`-edge subsets that form a tree), and on 20 random connected graphs for each of the sizes 10/30, 100/500 and 1000/5000 (vertices/edges); both results are verified to be spanning trees (`V−1` edges, acyclic, connected).
* **Topological order valid for every DAG:** 75 random DAGs (V = 10, 100, 1000) plus hand-made ones; validity checked edge by edge.
* **Cyclic graphs identified:** see above, with an independent SCC-based oracle.
* **Every vertex in exactly one SCC:** on 200 random digraphs the components partition the vertex set, and `u, v` share a component **iff** they are mutually reachable (Floyd–Warshall transitive closure as an oracle). A DAG gives `V` singleton SCCs; a directed ring gives one.
* **Dijkstra correct:** distances equal **Bellman–Ford** on 200 random digraphs (including zero weights, parallel edges, unreachable vertices), and the reconstructed path's weight equals the reported distance. Negative weights: `run()` throws, and the counter-example above shows `runUnchecked` giving a wrong answer.
* **DSU:** the four variants (naive / compression / rank / both) agree on connectivity in 50 random runs; component counts and `union` return values are checked.
* **Generators:** same seed ⇒ identical graph; exact edge count; no self-loops or parallel edges; undirected graphs connected; DAGs acyclic.

## 5. Conclusion

* **Infrastructure planning (MST).** Kruskal and Prim always return the same optimal cost (checked by the tests, including against brute force). **Kruskal + DSU** is simple and fits sparse road networks well: it sorts the edges once and handles a disconnected graph by returning a spanning forest. **Prim** with a binary heap has the same `O(E log V)` bound and is a good choice when the graph is already stored as adjacency lists; it is mainly attractive for very dense graphs with an array-based `O(V²)` or Fibonacci-heap version.
* **DSU optimisations are essential.** Without union by rank and path compression a find can cost `O(n)`; with both it is almost constant (`O(α(n))`), which makes Kruskal's cycle checks cheap compared with the sort.
* **Dependencies and connectivity.** **Kahn's algorithm** gives a valid dependency order or shows that none exists (a cycle) in `O(V + E)`; **Kosaraju** then finds *which* districts depend on each other in a loop (`O(V + E)`). Use Kahn for scheduling and SCC to find and fix circular dependencies.
* **Routing.** **Dijkstra** with a binary heap is the right tool for non-negative distances (`O(E log V)`). With negative weights it gives wrong answers (shown by a counter-example and a test), so Bellman–Ford or Johnson's algorithm must be used instead.
* **Summary of the complexity.** The linear-time algorithms (Kahn, Kosaraju) are the cheapest; the `E log V` algorithms (Kruskal, Prim, Dijkstra) cost an extra logarithmic factor because of sorting or the heap.

## How to build and run

Requirements: JDK 11+ (tested with OpenJDK 21).

```bash
./build.sh                                   # javac -d out $(find src -name '*.java')
java -cp out tests.TestRunner                # 62 correctness checks
java -cp out app.CityDemo                    # worked example with named districts
# or everything at once:
./run_all.sh
```

Graphs are generated with fixed seeds, so all tests are reproducible.
