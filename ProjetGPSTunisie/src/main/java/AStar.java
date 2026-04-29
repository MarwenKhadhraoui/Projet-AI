import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class AStar {

    // haversine formule
    public static double heuristic(Node a, Node b) {
        final double R = 6371.0; // Rayon de la Terre en km
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double sinDLat = Math.sin(dLat / 2);
        double sinDLon = Math.sin(dLon / 2);
        double x = sinDLat * sinDLat
                + Math.cos(Math.toRadians(a.getLatitude()))
                * Math.cos(Math.toRadians(b.getLatitude()))
                * sinDLon * sinDLon;
        return 2 * R * Math.asin(Math.sqrt(x));
    }

    public static PathResult findPath(Node start, Node goal) {
        Map<Node, Double> gScore = new HashMap<>();
        Map<Node, Double> fScore = new HashMap<>();
        Map<Node, Node> cameFrom = new HashMap<>();

        Comparator<Node> comparator = Comparator.comparingDouble(
                n -> fScore.getOrDefault(n, Double.MAX_VALUE)
        );

        PriorityQueue<Node> openSet = new PriorityQueue<>(comparator);

        gScore.put(start, 0.0);
        fScore.put(start, heuristic(start, goal));
        openSet.add(start);

        int explored = 0;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            explored++;

            if (current == goal) {
                List<Node> path = reconstructPath(cameFrom, current);
                return new PathResult(path, gScore.get(current), explored);
            }

            for (Edge edge : current.getNeighbors()) {
                Node neighbor = edge.getTarget();
                double tentativeG = gScore.get(current) + edge.getCost();

                if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    fScore.put(neighbor, tentativeG + heuristic(neighbor, goal));

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
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
}