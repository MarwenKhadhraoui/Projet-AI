import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class BFS {

    public static PathResult findPath(Node start, Node goal) {
        Queue<Node> queue = new LinkedList<>();
        Set<Node> visited = new HashSet<>();
        Map<Node, Node> cameFrom = new HashMap<>();

        queue.add(start);
        visited.add(start);

        int explored = 0;

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            explored++;

            if (current == goal) {
                List<Node> path = reconstructPath(cameFrom, current);
                double totalCost = calculatePathCost(path);
                return new PathResult(path, totalCost, explored);
            }

            for (Edge edge : current.getNeighbors()) {
                Node neighbor = edge.getTarget();

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return new PathResult(Collections.emptyList(), Double.MAX_VALUE, explored);
    }

    private static List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }

        Collections.reverse(path);
        return path;
    }

    private static double calculatePathCost(List<Node> path) {
        double total = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            Node current = path.get(i);
            Node next = path.get(i + 1);

            for (Edge edge : current.getNeighbors()) {
                if (edge.getTarget() == next) {
                    total += edge.getCost();
                    break;
                }
            }
        }

        return total;
    }
}