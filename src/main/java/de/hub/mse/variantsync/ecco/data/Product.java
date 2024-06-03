package de.hub.mse.variantsync.ecco.data;

import de.hub.mse.variantsync.ecco.ECCO_LIGHT;
import de.hub.mse.variantsync.ecco.data.position.Position;
import de.hub.mse.variantsync.ecco.data.specialization.JavaAST;
import de.hub.mse.variantsync.ecco.data.specialization.LineAST;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Product implements Serializable {
    private final EccoSet<Feature> features;
    private final String name;
    //the original product AST
    private AbstractAST productAST;
    //the AST nodes of the main tree that correspond to the AST nodes of the product AST
    private EccoSet<EccoNode> astNodesMainTree;

    public Product(final String name, final EccoSet<EccoNode> astNodesMainTree, final AbstractAST productAST, final EccoSet<Feature> features) {
        this.name = name;
        this.astNodesMainTree = astNodesMainTree;
        this.productAST = productAST;
        this.features = features;
    }

    public Product(final Product other) {
        this.name = other.name;
        final EccoSet<EccoNode> nodesToCopy = new EccoSet<>(other.astNodesMainTree);
        nodesToCopy.addAll(other.productAST.getAstNodes());
        nodesToCopy.add(other.productAST.getRoot());
        final Map<EccoNode, EccoNode> originalToCopyMap = copyAstNodes(nodesToCopy);

        final EccoSet<EccoNode> astNodes = new EccoSet<>(
                other.astNodesMainTree
                        .stream()
                        .map(originalToCopyMap::get)
                        .collect(Collectors.toCollection(EccoSet::new)));
        this.astNodesMainTree = new EccoSet<>(astNodes);

        final EccoSet<EccoNode> treeNodes = new EccoSet<>(
                other.productAST.getAstNodes()
                        .stream()
                        .map(originalToCopyMap::get)
                        .collect(Collectors.toCollection(EccoSet::new)));
        if (other.productAST instanceof JavaAST) {
            final JavaAST ast = (JavaAST) other.productAST;
            this.productAST = new JavaAST(originalToCopyMap.get(ast.getRoot()), treeNodes);
        } else if (other.productAST instanceof LineAST) {
            final LineAST ast = (LineAST) other.productAST;
            this.productAST = new LineAST(originalToCopyMap.get(ast.getRoot()), treeNodes);
        } else {
            throw new UnsupportedOperationException();
        }
        this.features = new EccoSet<>(other.getFeatures());
    }

    public void forgetAST() {
        this.productAST = null;
    }

    public EccoSet<Feature> getFeatures() {
        return features;
    }

    public EccoSet<EccoNode> getAstNodesMainTree() {
        return astNodesMainTree;
    }

    //returns the mapping of the node at given position in the product or (if it was already merged into the main tree) the mapping of the corresponding node in the main tree
    public Formula getMappingFromPosition(final Position position) {
        //look for the right node in the product AST's nodes
        for (final EccoNode oldNode : productAST.getAstNodes()) {
            if (position.equals(oldNode.getStartPosition())) {
                //find and return the mapping of the corresponding node in the main tree
                for (final EccoNode newNode : astNodesMainTree) {
                    if (newNode.isSimilar(oldNode)) {
                        return newNode.getMapping();
                    }
                }
            }
        }
        System.out.println("No node with given position");
        return null;
    }

    public String getName() {
        return name;
    }

    //returns the node at given position in the product or (if it was already merged into the main tree) the corresponding node in the main tree
    public EccoNode getNodeFromPosition(final Position position) {
        //look for the right node in the product AST's nodes
        for (final EccoNode oldNode : productAST.getAstNodes()) {
            if (position.equals(oldNode.getStartPosition())) {
                //find and return the corresponding node in the main tree
                for (final EccoNode newNode : astNodesMainTree) {
                    if (newNode.isSimilar(oldNode)) {
                        return newNode;
                    }
                }
            }
        }
        System.out.println("No node with given position");
        return null;
    }

    public AbstractAST getProductAst() {
        return productAST;
    }

    public void setAstNodesMainTree(final EccoSet<EccoNode> astNodesMainTree) {
        this.astNodesMainTree = astNodesMainTree;
    }

    private static Map<EccoNode, EccoNode> copyAstNodes(final EccoSet<EccoNode> astNodes) {
        final Map<EccoNode, EccoNode> originalToCopyMap = new HashMap<>();
        for (final var node : astNodes) {
            final EccoNode nodeCopy;
            if (!originalToCopyMap.containsKey(node)) {
                nodeCopy = copyNode(node);
                originalToCopyMap.put(node, nodeCopy);
            } else {
                nodeCopy = originalToCopyMap.get(node);
            }

            // Handle parent
            {
                final var parent = node.getParent();
                if (parent == null) {
                    nodeCopy.setParent(null);
                } else {
                    final EccoNode parentCopy;
                    if (!originalToCopyMap.containsKey(parent)) {
                        parentCopy = copyNode(parent);
                        originalToCopyMap.put(parent, parentCopy);
                    } else {
                        parentCopy = originalToCopyMap.get(parent);
                    }
                    nodeCopy.setParent(parentCopy);
                }
            }

            // Handle children
            {
                final var children = node.getChildren();
                final EccoSet<EccoNode> childrenOfCopy = nodeCopy.getChildren();
                for (final var child : children) {
                    final EccoNode childCopy;
                    if (!originalToCopyMap.containsKey(child)) {
                        childCopy = copyNode(child);
                        originalToCopyMap.put(child, childCopy);
                    } else {
                        childCopy = originalToCopyMap.get(child);
                    }
                    childrenOfCopy.add(childCopy);
                }
            }

            // Handle product equivalent
            {
                final EccoNode productEquivalentCopy;
                final var productEquivalent = node.getProductEquivalent();
                if (productEquivalent == null) {
                    nodeCopy.setProductEquivalent(null);
                } else {
                    if (!originalToCopyMap.containsKey(productEquivalent)) {
                        productEquivalentCopy = copyNode(productEquivalent);
                        originalToCopyMap.put(productEquivalent, productEquivalentCopy);
                    } else {
                        productEquivalentCopy = originalToCopyMap.get(productEquivalent);
                    }
                    nodeCopy.setProductEquivalent(productEquivalentCopy);
                }
            }
        }
        return originalToCopyMap;
    }

    private static EccoNode copyNode(final EccoNode node) {
        try {
            final Formula mapping;
            if (node.getMapping() == null) {
                mapping = null;
            } else {
                mapping = ECCO_LIGHT.f.parse(node.getMapping().toString());
            }
            return new EccoNode(null, node.getCode(), node.getStartPosition(), node.getType(), mapping);
        } catch (final ParserException e) {
            throw new RuntimeException("Was not able to parse mapping.");
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        final Product product = (Product) o;
        return features.equals(product.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(features);
    }
}
