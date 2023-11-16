/* ---------------------------------------------------------------
Práctica 2.
Código fuente: Node.java
Grau Informàtica
48056540H - Aleix Segura Paz.
21161168H - Aniol Serrano Ortega.
--------------------------------------------------------------- */

import org.javatuples.Pair;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

// Node that implements the Branch&Bound search three elements in the solution space
public class Node {
    // Helps in tracing the path when the answer is found
    // Stores the edges of the path completed till current visited node
    Vector<Pair<Integer, Integer>> path;

    // Stores the reduced matrix
    int[][] reducedMatrix;
    // Stores the cities already visited.
    Boolean[] visitedCities;
    // Stores the lower bound
    int cost;
    // Stores the current city number
    int vertex;
    // Stores the total number of cities visited
    int level;

    TSP tsp;
    long id = 0;

    private static long totalNodes = 0;

    // Setters & Getters
    public long getId() {
        return id;
    }

    public static long getTotalNodes() {
        return totalNodes;
    }

    public Boolean[] getVisitedCities() {
        return visitedCities;
    }

    public Boolean cityVisited(int city) {
        return visitedCities[city];
    }

    public Vector<Pair<Integer, Integer>> getPath() {
        return path;
    }

    public void setPath(Vector<Pair<Integer, Integer>> path) {
        this.path = path;
    }

    public int[][] getReducedMatrix() {
        return reducedMatrix;
    }

    public void setReducedMatrix(int[][] reducedMatrix) {
        this.reducedMatrix = reducedMatrix;
    }

    public int getCostMatrix(int i, int j) {
        return reducedMatrix[i][j];
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void calculateSetCost() {
        setCost(calculateCost());
    }

    public int getVertex() {
        return vertex;
    }

    public void setVertex(int vertex) {
        this.vertex = vertex;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void addPathStep(int startCity, int destCity) {
        path.addElement(new Pair<>(startCity, destCity));
    }

    // Constructors
    public Node(TSP tsp, int[][] reducedMatrix) {
        this.tsp = tsp;

        this.reducedMatrix = new int[tsp.getnCities()][tsp.getnCities()]; // inicialitza el tamany de la matriu reuida, encara no redueix
        for (int x = 0; x < reducedMatrix.length; x++)
            this.reducedMatrix[x] = reducedMatrix[x].clone();


        this.visitedCities = new Boolean[tsp.getnCities()]; // inicialitza el tamany de les ciutats visitades
        Arrays.fill(this.visitedCities, false); // inicialitza totes a fals
        this.visitedCities[0] = true; // la primera a true (root city)

        this.path = new Vector<>(); // creem un cami, el cost, el id del vertex, el nivell i el id
        this.cost = 0;
        this.vertex = 0;
        this.level = 0;
        this.id = ++totalNodes; // inicialitza a 1 totalnodes
    }

    public Node(TSP tsp, Node parentNode, int level, int i, int j) {
        this.tsp = tsp;
        this.cost = 0;
        this.vertex = j;
        this.level = level;
        this.id = ++totalNodes;

        // Copy path data from the parent node to the current node
        if (parentNode.getPath() != null)
            this.path = (Vector<Pair<Integer, Integer>>) parentNode.getPath().clone();
        else
            this.path = new Vector<>();
        // Skip for the root node
        if (level != 0)
            // Add a current edge to the path
            addPathStep(i, j);

        // Copy reduce matrix data from the parent node to the current node
        this.reducedMatrix = new int[tsp.getnCities()][tsp.getnCities()];
        for (int x = 0; x < parentNode.getReducedMatrix().length; x++)
            this.reducedMatrix[x] = parentNode.getReducedMatrix()[x].clone();

        // Reserve and clone the visited cities array.
        this.visitedCities = new Boolean[tsp.getnCities()];
        this.visitedCities = parentNode.getVisitedCities().clone();
        this.visitedCities[j] = true;

        // Change all entries of row i and column j to INF skip for the root node
        for (int k = 0; level != 0 && k < tsp.getnCities(); k++) {
            // Set outgoing edges for the city i to INF
            reducedMatrix[i][k] = TSP.INF;
            // Set incoming edges to city j to INF
            reducedMatrix[k][j] = TSP.INF;
        }

        // Set (j, 0) to INF here start node is 0
        reducedMatrix[j][0] = TSP.INF;
    }

    // Calcula la Cota Inferior de root (inici problema aka node 0)
    public int calculateCost() {
        // Initialize cost to 0
        int cost = 0;

        // Row Reduction
        int[] row = new int[tsp.getnCities()];
        rowReduction(reducedMatrix, row);

        // Column Reduction
        int[] col = new int[tsp.getnCities()];
        columnReduction(reducedMatrix, col);

        // The total expected cost is the sum of all reductions
        for (int i = 0; i < tsp.getnCities(); i++) {
            cost += (row[i] != TSP.INF) ? row[i] : 0;
            cost += (col[i] != TSP.INF) ? col[i] : 0;
        }

        return cost;
    }

    // Redueix cada fila de forma que hi hagi almenys un valor 0 a cada fila.
    public void rowReduction(int[][] reducedMatrix, int[] row) {
        // Initialize row array to INF
        java.util.Arrays.fill(row, TSP.INF);

        // row[i] contains minimum in row i
        for (int i = 0; i < tsp.getnCities(); i++) {
            for (int j = 0; j < tsp.getnCities(); j++) {
                if (reducedMatrix[i][j] != TSP.INF && (row[i] == TSP.INF || reducedMatrix[i][j] < row[i])) {
                    row[i] = reducedMatrix[i][j];
                }
            }
        }

        // Reduce the minimum value from each element in each row
        for (int i = 0; i < tsp.getnCities(); i++) {
            for (int j = 0; j < tsp.getnCities(); j++) {
                if (reducedMatrix[i][j] != TSP.INF && row[i] != TSP.INF) {
                    reducedMatrix[i][j] -= row[i];
                }
            }
        }

    }

    // Redueix cada columna de forma que hi hagi almenys un valor 0 a cada columna.
    public void columnReduction(int[][] reducedMatrix, int[] col) {
        // Initialize all elements of array col with INF
        java.util.Arrays.fill(col, TSP.INF);

        // col[j] contains minimum in col j
        for (int i = 0; i < tsp.getnCities(); i++) {
            for (int j = 0; j < tsp.getnCities(); j++) {
                if (reducedMatrix[i][j] != TSP.INF && (col[j] == TSP.INF || reducedMatrix[i][j] < col[j])) {
                    col[j] = reducedMatrix[i][j];
                }
            }
        }
        // Reduce the minimum value from each element
        // in each column
        for (int i = 0; i < tsp.getnCities(); i++) {
            for (int j = 0; j < tsp.getnCities(); j++) {
                if (reducedMatrix[i][j] != TSP.INF && col[j] != TSP.INF) {
                    reducedMatrix[i][j] -= col[j];
                }
            }
        }
    }

    public void cleanup() {
        this.path.clear();
        this.reducedMatrix = null;
        this.visitedCities = null;
    }

    // Function to print list of cities visited following least cost
    void printPath(PrintStream out, Boolean withCosts) {
        out.print(pathToString(withCosts));
    }

    public String pathToString(Boolean withCosts) {
        //String out ="[Node %d] "+getId()+"\n";
        String out = path.stream()
                .map((step) -> {
                    String out_step;
                    if (withCosts)
                        out_step = (step.getValue0() + 1) + "->" + (step.getValue1() + 1) + " (" + tsp.getDistanceMatrix(step.getValue0(), step.getValue1()) + ")";
                    else
                        out_step = (step.getValue0() + 1) + "->" + (step.getValue1() + 1);
                    return out_step;
                })
                .collect(Collectors.joining(", ", "{", "}"));

        if (withCosts)
            out += " ==> Total Cost " + getCost() + ".\n";
        else
            out += ".\n";

        return out;
    }

    public String ReducedMatrixToString() {
        StringBuilder str = new StringBuilder();
        for (int[] matrix : reducedMatrix) {
            str.append((str.length() > 0) ? "\n\t\t" : "").append("[");
            for (int j = 0; j < reducedMatrix[0].length; j++)
                str.append(String.format("%1$" + TSP.CMatrixPadding + "s", matrix[j])).append(" ");
            str.append("]");
        }
        return str + "\n";
    }

    @Override
    public String toString() {
        //return "["+getId()+":"+getLevel()+","+getCost()+","+getVertex()+"]";
        return "___________________________________________________________________________________________\n" +
                "Node:  \t" + getId() + "\n" +
                "Path:  \t" + pathToString(true) +
                "Matrix:\t" + ReducedMatrixToString() +
                "Cost:  \t" + getCost() + "\n" +
                "Vertex:\t" + getVertex() + "\n" +
                "Level: \t" + getLevel() + "\n";
    }
}
