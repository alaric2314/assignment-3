| Graph | V | E | Input | Avg time (us) | SD (us) | Acyclic | Output vertices | Edges examined | Vertices processed |
|---|---|---|---|---|---|---|---|---|---|
| Small | 10 | 30 | DAG | 1.16 | 1.80 | true | 10 | 60 | 10 |
| Small | 10 | 30 | Random digraph (cyclic) | 0.71 | 1.37 | false | 0 | 30 | 0 |
| Medium | 100 | 500 | DAG | 4.90 | 0.50 | true | 100 | 1000 | 100 |
| Medium | 100 | 500 | Random digraph (cyclic) | 1.40 | 0.22 | false | 0 | 500 | 0 |
| Large | 1000 | 5000 | DAG | 54.09 | 5.56 | true | 1000 | 10000 | 1000 |
| Large | 1000 | 5000 | Random digraph (cyclic) | 10.51 | 0.05 | false | 4 | 5014 | 4 |
