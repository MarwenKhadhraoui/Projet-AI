public class Edge {
    private Node target;
    private double cost;

    public Edge(Node target, double cost) {
        this.target = target;
        this.cost = cost;
    }

    public Node getTarget() {
        return target;
    }

    public double getCost() {
        return cost;
    }
}