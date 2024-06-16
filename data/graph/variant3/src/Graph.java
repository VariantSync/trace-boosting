import java.util.List;

// configuration: G and E and C

public interface Graph {
    List<Node> nodes();
    List<Node> nodes(Color c);
    List<Edge> edges();
    // Feat: C
    Graph subGraph(Color c);
}