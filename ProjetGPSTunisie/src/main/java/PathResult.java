import java.util.List;

public class PathResult {
    private List<Node> path;
    private double totalCost;
    private int exploredNodes;

    public PathResult(List<Node> path, double totalCost, int exploredNodes) {
        this.path = path;
        this.totalCost = totalCost;
        this.exploredNodes = exploredNodes;
    }

    public List<Node> getPath() {
        return path;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public int getExploredNodes() {
        return exploredNodes;
    }
}
