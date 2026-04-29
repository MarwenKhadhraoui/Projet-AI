import java.util.HashMap;
import java.util.Map;

public class Graph {
    private Map<String, Node> nodes;

    public Graph() {
        nodes = new HashMap<>();
    }

    public void addNode(String name, double latitude, double longitude, double x, double y) {
        nodes.put(name, new Node(name, latitude, longitude, x, y));
    }

    public Node getNode(String name) {
        return nodes.get(name);
    }

    public Map<String, Node> getNodes() {
        return nodes;
    }

    public void addEdge(String from, String to, double distance) {
        Node source = nodes.get(from);
        Node destination = nodes.get(to);

        if (source != null && destination != null) {
            source.addNeighbor(destination, distance);
            destination.addNeighbor(source, distance);
        }
    }
}
