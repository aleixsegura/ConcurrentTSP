/* ---------------------------------------------------------------
Práctica 2.
Código fuente: TSP.java
Grau Informàtica
48056540H - Aleix Segura Paz.
21161168H - Aniol Serrano Ortega.
--------------------------------------------------------------- */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;


public class TSP {
    private final int DEFAULT_NUMBER_OF_THREADS = 50;
    private final int numberOfThreads;
    private final String concurrentMethod;
    private Node root;
    private Node[] rootChildren;
    private RootChildProblem[] subProblems;
    private final ArrayList<Future<Node>> solutions = new ArrayList<>();

    public static final int INF = -1;
    public static final int CMatrixPadding = 3;

    public int[][] DistanceMatrix;

    public static int nCities = 0;

    private Node solution = null;

    // Statistics of purged and processed nodes.
    private long purgedNodes = 0;
    private long processedNodes = 0;


    // Getters & Setters
    public int getnCities() {
        return nCities;
    }
    public void setnCities(int nCities) {
        this.nCities = nCities;
    }
    public Node getSolution() {
        return solution;
    }
    public void setSolution(Node solution) {
        this.solution = solution;
    }
    public int getDistanceMatrix(int i, int j) { return DistanceMatrix[i][j]; }
    public int[][] getDistanceMatrix() {
        return DistanceMatrix;
    }
    public long getPurgedNodes() { return purgedNodes; }
    public long getProcessedNodes() { return processedNodes; }

    // Constructors.
    public TSP(String concurrentMethod){
        this.numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
        this.concurrentMethod = concurrentMethod;
        InitDefaultCitiesDistances();
    }

    public TSP(String citiesPath, String concurrentMethod){
        this.numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
        this.concurrentMethod = concurrentMethod;
        readCitiesFile(citiesPath);
    }

    public TSP(String citiesPath, int numberOfThreads, String concurrentMethod){
        this.numberOfThreads = numberOfThreads;
        this.concurrentMethod = concurrentMethod;
        readCitiesFile(citiesPath);
    }


    public void InitDefaultCitiesDistances() {
        DistanceMatrix = new int[][]{{INF, 10, 15, 20},
                                    {10, INF, 35, 25},
                                    {15, 35, INF, 30},
                                    {20, 25, 30, INF}};
        nCities = 4;
    }

    // Reads file and builds the matrix of distances not reduced.
    public void readCitiesFile(String citiesPath) {
        Scanner input = null;
        try {
            input = new Scanner(new File(citiesPath));
            // Read the number of cities
            nCities = 0;
            if (input.hasNextInt())
                nCities = input.nextInt();
            else
                System.err.printf("[TSP::ReadCitiesFile] Error reading cities number on %s.\n", citiesPath);

            // Init cities' distances matrix
            DistanceMatrix = new int[nCities][nCities];

            // Read cities distances
            for (int i = 0; i < nCities; ++i) {
                for (int j = 0; j < nCities; ++j) {
                    DistanceMatrix[i][j] = 0;
                    if (input.hasNextInt())
                        DistanceMatrix[i][j] = input.nextInt();
                    else
                        System.err.printf("[TSP::ReadCitiesFile] Error reading distance beetwen cities %d-%d.\n", i, j);
                }
            }

        } catch (FileNotFoundException e) {
            System.err.printf("[TSP::ReadCitiesFile] File %s not found.\n",citiesPath);
            e.printStackTrace();
        }
    }

    public void solve() throws ExecutionException, InterruptedException {
        Instant start = Instant.now();

        Node solution = solve(DistanceMatrix);
        printSolution("\n Optimal Solution: ", solution);

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        System.out.printf("Total execution time: %.3f secs with %d cities.\n", timeElapsed / 1000.0, getnCities());
        System.out.println("\n___________________________________________________________________________________________________________________________________________________");
    }

    /**
     * Initializes root node, executor services or ForkJoinPool depending on the parameter 'concurrentMethod'
     * introduced and calls a method to solve the problem.
     */
    public Node solve(int[][] CostMatrix) throws ExecutionException, InterruptedException {
        ExecutorService executorService;
        root = new Node(this, CostMatrix);
        root.calculateSetCost();

        printTypeTest();

        switch (concurrentMethod) {
            case "FixedThreadPool" -> {
                executorService = Executors.newFixedThreadPool(numberOfThreads);
                solve(executorService);
            }
            case "CachedThreadPool" -> {
                executorService = Executors.newCachedThreadPool();
                solve(executorService);
            }
            case "ForkJoinPool" -> {
                ForkJoinPool pool = new ForkJoinPool(numberOfThreads);
            }
        }
        printStatistics();
        return getSolution();
    }

    private void printTypeTest(){
        System.out.println("\n___________________________________________________________________________________________________________________________________________________");
        System.out.printf("Test with %d cities.\n", getnCities());
    }

    /**
     * Launches root children sub problems in a way that the parallelized tasks have a proper granularity to be
     * launched to multiple threads.
     */
    public void solve(ExecutorService executorService) throws ExecutionException, InterruptedException {
        subProblems = new RootChildProblem[nCities - 1];
        getRootChildren();

        int i = 0;
        for (Node rootChild: rootChildren){
            RootChildProblem subProblem = new RootChildProblem(this, rootChild);
            Future<Node> subProblemSolution = executorService.submit(subProblem);
            subProblems[i++] = subProblem;
            solutions.add(subProblemSolution);
        }
        executorService.shutdown();
        getBestSolution();
        updateStatistics();
    }

    /**
     * Simply generates root children and calculates it cost.
     */
    public void getRootChildren(){
        rootChildren = new Node[nCities - 1];
        int rootCity = root.getVertex();
        processedNodes++;

        int i = 0;
        for (int city = 1; city < nCities; city++){
            Node child = new Node(this, root, 1, rootCity, city);
            int childCost = root.getCost() + root.getCostMatrix(rootCity, city) + child.calculateCost();
            child.setCost(childCost);
            rootChildren[i++] = child;
        }
    }

    /**
     * Waits for tasks to finalize so the best optimal solution can be obtained.
     */
    public void getBestSolution() throws ExecutionException, InterruptedException {
        for (Future<Node> solutionFuture : solutions) solutionFuture.get();
        solution = RootChildProblem.getSolution();
    }

    /**
     * Reclaims local statistics from sub-problem tasks and integrates to globals.
     */
    public void updateStatistics(){
        for (RootChildProblem subProblem: subProblems){
            processedNodes += subProblem.getProcessedNodes();
            purgedNodes += subProblem.getPurgedNodes();
        }
    }

    /**
     * Simply prints final statistics.
     */
    public void printStatistics(){
        System.out.printf("\nFinal Total nodes: %d \tProcessed nodes: %d \tPurged nodes: %d \tPending nodes: %d \tBest Solution: %d.",
                Node.getTotalNodes(), processedNodes, purgedNodes, RootChildProblem.getNodePriorityQueue().size(), getSolution() == null ? 0 : getSolution().getCost());
    }

    public void printSolution(String msg, Node sol) { printSolution(System.out, msg, sol); }

    public void printSolution(PrintStream out, String msg, Node sol) {
        out.print(msg);
        sol.printPath(out, true);
    }
}
