import org.logicng.formulas.FormulaFactory;
import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.datastructure.Feature;
import org.variantsync.boosting.datastructure.MainTree;
import org.variantsync.boosting.parsing.ESupportedLanguages;
import org.variantsync.boosting.product.Variant;
import org.variantsync.boosting.product.VariantPassport;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Demo {

    public static void main(String[] args) {

        List<VariantPassport> variantPassports = createVariantPassports();

        TraceBoosting traceBoosting = new TraceBoosting(
                variantPassports,
                Path.of(System.getProperty("user.dir") + "/data/graph/workdir"),
                ESupportedLanguages.LINES);

        // optionally the number of threads can be adjusted, per default it is set to the number of available system threads
        traceBoosting.setNumThreads(1);

        List<Variant> variants = traceBoosting.getVariants();
        System.out.println("initiated with " + variants.size() + " products");
        applyDistribution(variants);

        //collect and print set of extracted features (from configurations)
        Set<String> relevantFeatures = variants.stream().flatMap(p -> p.getFeatures().stream().map(Feature::getName))
                .collect(Collectors.toSet());
        System.out.println(relevantFeatures);

        // runs the boosted comparison-based feature tracoing algorithm
        MainTree tree = traceBoosting.computeMappings();

        // verify the results by checking which feature expressions are mapped onto the nodes of the variants
        for (ASTNode n : tree.getTree().getAstNodes()) {
            if (!n.getMapping().toString().equals("$true")) {
                System.out.println(n.getMapping()+ " mapped onto " + n.getCode());
            }
        }

    }

    private static List<VariantPassport> createVariantPassports() {
        var variantPassports = new ArrayList<VariantPassport>(3);
        variantPassports.add(
                new VariantPassport("weighted",
                        Path.of(System.getProperty("user.dir") + "/data/graph/variant1/src"),
                        //configFileMap.get(variantName)
                        Path.of(System.getProperty("user.dir") + "/data/graph/variant1/config1.config")
                )
        );

        variantPassports.add(
                new VariantPassport("directed",
                        Path.of(System.getProperty("user.dir") + "/data/graph/variant2/src"),
                        //configFileMap.get(variantName)
                        Path.of(System.getProperty("user.dir") + "/data/graph/variant2/config2.config")
                )
        );

        variantPassports.add(
                new VariantPassport("colored",
                        Path.of(System.getProperty("user.dir") + "/data/graph/variant3/src"),
                        Path.of(System.getProperty("user.dir") + "/data/graph/variant3/config3.config"))
        );
        return variantPassports;
    }

    // simulates setting manually annotations of some AST nodes
    private static void applyDistribution(List<Variant> variants) {
        var factory = new FormulaFactory();
        for (Variant p : variants) {
            // Nodes that can be mapped
            if (p.getName().equals("weighted")) {
                for (ASTNode n : p.getProductAst().getAstNodes()) {
                    if (n.getCode().contains("List<Edge> edges();"))
                        n.setMapping(factory.variable("E"));
                }
            }
            if (p.getName().equals("weighted")) {
                for (ASTNode n : p.getProductAst().getAstNodes()) {
                    if (n.getCode().contains("public interface Graph {"))
                        n.setMapping(factory.variable("G"));
                }
            }
            if (p.getName().equals("directed")) {
                for (ASTNode n : p.getProductAst().getAstNodes()) {
                    if (n.getCode().contains("Graph subGraph(Color c);"))
                        n.setMapping(factory.variable("C"));
                }
            }
            if (p.getName().equals("colored")) {
                for (ASTNode n : p.getProductAst().getAstNodes()) {
                    if (n.getCode().contains("Graph subGraph(Color c);"))
                        n.setMapping(factory.variable("C"));
                }
            }
        }
    }
}