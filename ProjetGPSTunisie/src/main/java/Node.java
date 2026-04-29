import java.util.ArrayList;
import java.util.List;

public class Node {
    private String name;
    private double latitude;
    private double longitude;
    private double x;
    private double y;
    private List<Edge> neighbors;

    public Node(String name, double latitude, double longitude, double x, double y) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.x = x;
        this.y = y;
        this.neighbors = new ArrayList<>();
    }

    public void addNeighbor(Node destination, double distance) {
        neighbors.add(new Edge(destination, distance));
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<Edge> getNeighbors() {
        return neighbors;
    }

    @Override
    public String toString() {
        return name;
    }
}
