/* ---------------------------------------------------------------
Práctica 2.
Código fuente: RootChildProblem.java
Grau Informàtica
48056540H - Aleix Segura Paz.
21161168H - Aniol Serrano Ortega.
--------------------------------------------------------------- */
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class RootChildProblem implements Runnable {
    private static final PriorityBlockingQueue<Node> nodePriorityQueue = new PriorityBlockingQueue<>(10,
            Comparator.comparingInt(Node::getCost));

    public static final PriorityBlockingQueue<Node> solutions = new PriorityBlockingQueue<>(10,
            Comparator.comparingInt(Node::getCost));

    private boolean cancel = false;

    private int processedNodes;
    private int purgedNodes;
    private final TSP tsp;
    private final Node root;


    public RootChildProblem(TSP tsp, Node root){
        this.tsp = tsp;
        this.root = root;
        processedNodes = 0;
        purgedNodes = 0;
    }

    // Getters
    public int getProcessedNodes() { return processedNodes; }
    public int getPurgedNodes() { return purgedNodes; }
    public static PriorityBlockingQueue<Node> getNodePriorityQueue() { return nodePriorityQueue; }
    private Node getSolution() { return solutions.peek(); }


    /**
     * Each thread adds a sub problem to a PQ: nodePriorityQueue, then the paths are explored concurrently and
     * each sub thread work with shared 'solution' that is the first element of solutions PQ.
     */
    @Override
    public void run(){
        try{
            pushNode(root);
            Node min;
            int row;

            while ((min = popNode()) != null && !cancel) {
                processedNodes++;
                row = min.getVertex();

                if (min.getLevel() == TSP.nCities - 1) {
                    min.addPathStep(row, 0);
                    if (getSolution() == null || min.getCost() < getSolution().getCost()){
                        updateSolution(min);
                    }
                }

                if ( processedNodes % 10 == 0 && getSolution() != null){
                    purgeWorseNodes(getSolution().getCost());
                }

                if (getSolution() == null || min.getCost() < getSolution().getCost()){
                    for (int column = 0; column < TSP.nCities; column++){
                        if (!min.cityVisited(column) && min.getCostMatrix(row, column) != TSP.INF){
                            Node child = new Node(tsp, min, min.getLevel() + 1, row, column);
                            int childCost = min.getCost() + min.getCostMatrix(row, column) + child.calculateCost();
                            child.setCost(childCost);

                            Node currentSolution = getSolution();
                            if (currentSolution == null || child.getCost() < currentSolution.getCost()) {
                                pushNode(child);
                            }
                            else if (child.getCost() > currentSolution.getCost())
                                purgedNodes++;
                        }
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            throw new OutOfMemoryError();
        }
    }

    private void updateSolution(Node min) {
        Node currentSolution = getSolution();
        if (currentSolution == null || min.getCost() < currentSolution.getCost()) {
            solutions.clear();
            solutions.add(min);
        }
    }

    private void pushNode(Node node) { nodePriorityQueue.add(node); }

    private Node popNode() { return nodePriorityQueue.poll(); }

    private void purgeWorseNodes(int minCost) {
        int pendingNodes = nodePriorityQueue.size();
        nodePriorityQueue.removeIf(node -> {
            boolean remove = node.getCost() >= minCost;
            if (remove) {
                node.cleanup();
            }
            return remove;
        });
        purgedNodes += pendingNodes - nodePriorityQueue.size();
    }


    public void cancelThread() {
        cancel = true;
    }
}
