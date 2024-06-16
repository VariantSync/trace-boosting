import java.util.List;

// configuration: G and E and D

public interface Graph {
    List<Node> nodes();
    List<Edge> edges();
    List<Edge> incomingEdges(Node n);
    // Feat: C
    Graph subGraph(Color c);
}