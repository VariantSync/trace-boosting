import java.util.List;
import java.util.LinkedList;

// configuration: G and E and W
// Feat: Graph
public interface Graph {
    List<Node> nodes();
    // Feat: E
    List<Edge> edges();
    List<Node> nodes(double weight);
}