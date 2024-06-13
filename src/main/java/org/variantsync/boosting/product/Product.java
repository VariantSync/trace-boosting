package org.variantsync.boosting.product;

import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.datastructure.CustomHashSet;
import org.variantsync.boosting.datastructure.Feature;
import org.variantsync.boosting.parsing.AbstractAST;
import org.variantsync.boosting.parsing.JavaAST;
import org.variantsync.boosting.parsing.LineAST;
import org.variantsync.boosting.position.Position;

import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The Product class represents a product with features and an abstract syntax tree (AST).
 * 
 * This class defines a product with a set of features and a product
 * AST.
 * It also stores the AST nodes of the main tree that correspond to the AST
 * nodes of the product AST.
 */
public class Product implements Serializable {
    private final CustomHashSet<Feature> features;
    private final String name;
    // the original product AST
    private AbstractAST productAST;
    // the AST nodes of the main tree that correspond to the AST nodes of the
    // product AST
    private CustomHashSet<ASTNode> astNodesMainTree;

    /**
     * Constructs a new Product object with the given parameters.
     * 
     * @param name             the name of the product
     * @param astNodesMainTree the main tree of AST nodes for the product
     * @param productAST       the abstract syntax tree for the product
     * @param features         the set of features associated with the product
     */
    public Product(final String name, final CustomHashSet<ASTNode> astNodesMainTree, final AbstractAST productAST,
                   final CustomHashSet<Feature> features) {
        this.name = name;
        this.astNodesMainTree = astNodesMainTree;
        this.productAST = productAST;
        this.features = features;
    }

    /**
     * Creates a new Product object by copying the contents of another Product
     * object.
     * 
     * @param other The Product object to copy from
     * @throws UnsupportedOperationException if the productAST type is not JavaAST
     *                                       or LineAST
     */
    public Product(final Product other) {
        // Copy the name from the other Product object
        this.name = other.name;

        // Create a new set of ASTNodes to copy from the other Product object
        final CustomHashSet<ASTNode> nodesToCopy = new CustomHashSet<>(other.astNodesMainTree);
        nodesToCopy.addAll(other.productAST.getAstNodes());
        nodesToCopy.add(other.productAST.getRoot());

        // Create a mapping of original ASTNodes to copied ASTNodes
        final Map<ASTNode, ASTNode> originalToCopyMap = copyAstNodes(nodesToCopy);

        // Copy the ASTNodes from the other Product object and update the main tree
        final CustomHashSet<ASTNode> astNodes = new CustomHashSet<>(
                other.astNodesMainTree
                        .stream()
                        .map(originalToCopyMap::get)
                        .collect(Collectors.toCollection(CustomHashSet::new)));
        this.astNodesMainTree = new CustomHashSet<>(astNodes);

        // Copy the ASTNodes from the other Product object and update the tree
        final CustomHashSet<ASTNode> treeNodes = new CustomHashSet<>(
                other.productAST.getAstNodes()
                        .stream()
                        .map(originalToCopyMap::get)
                        .collect(Collectors.toCollection(CustomHashSet::new)));

        // Create a new productAST object based on the type of the other Product
        // object's productAST
        if (other.productAST instanceof JavaAST) {
            final JavaAST ast = (JavaAST) other.productAST;
            this.productAST = new JavaAST(originalToCopyMap.get(ast.getRoot()), treeNodes);
        } else if (other.productAST instanceof LineAST) {
            final LineAST ast = (LineAST) other.productAST;
            this.productAST = new LineAST(originalToCopyMap.get(ast.getRoot()), treeNodes);
        } else {
            throw new UnsupportedOperationException("Unsupported productAST type");
        }

        // Copy the features from the other Product object
        this.features = new CustomHashSet<>(other.getFeatures());
    }

    /**
     * Clears the product Abstract Syntax Tree (AST) by setting it to null.
     */
    public void forgetAST() {
        this.productAST = null;
    }

    /**
     * Retrieves the set of features associated with the product.
     *
     * @return The set of features associated with the product.
     */
    public CustomHashSet<Feature> getFeatures() {
        return features;
    }

    /**
     * Returns the set of AST nodes in the main tree.
     * 
     * @return a set of AST nodes in the main tree
     */
    public CustomHashSet<ASTNode> getAstNodesMainTree() {
        return astNodesMainTree;
    }

    /**
     * Returns the mapping of the node at the given position in the product AST or,
     * if it has already been merged into the main tree, the mapping of the
     * corresponding
     * node in the main tree.
     *
     * @param position The position of the node to find the mapping for
     * @return The mapping of the node at the given position, or null if no node is
     *         found
     */
    public Formula getMappingFromPosition(final Position position) {
        // look for the right node in the product AST's nodes
        for (final ASTNode oldNode : productAST.getAstNodes()) {
            if (position.equals(oldNode.getStartPosition())) {
                // find and return the mapping of the corresponding node in the main tree
                for (final ASTNode newNode : astNodesMainTree) {
                    if (newNode.isSimilar(oldNode)) {
                        return newNode.getMapping();
                    }
                }
            }
        }
        System.out.println("No node with given position");
        return null;
    }

    /**
     * Returns the name of the object.
     *
     * @return the name of the object as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ASTNode at the given position in the product AST or, if it has
     * already been merged into the main tree,
     * returns the corresponding node in the main tree.
     *
     * @param position The position to search for in the product AST.
     * @return The ASTNode at the given position in the product AST, or the
     *         corresponding node in the main tree if merged.
     */
    public ASTNode getNodeFromPosition(final Position position) {
        // look for the right node in the product AST's nodes
        for (final ASTNode oldNode : productAST.getAstNodes()) {
            if (position.equals(oldNode.getStartPosition())) {
                // find and return the corresponding node in the main tree
                for (final ASTNode newNode : astNodesMainTree) {
                    if (newNode.isSimilar(oldNode)) {
                        return newNode;
                    }
                }
            }
        }
        System.out.println("No node with given position");
        return null;
    }

    /**
     * Returns the Abstract Syntax Tree (AST) representing the product.
     *
     * @return the AST representing the product
     */
    public AbstractAST getProductAst() {
        return productAST;
    }

    /**
     * Sets the AST nodes for the main tree.
     *
     * @param astNodesMainTree the AST nodes for the main tree
     */
    public void setAstNodesMainTree(final CustomHashSet<ASTNode> astNodesMainTree) {
        this.astNodesMainTree = astNodesMainTree;
    }

    private static Map<ASTNode, ASTNode> copyAstNodes(final CustomHashSet<ASTNode> astNodes) {
        final Map<ASTNode, ASTNode> originalToCopyMap = new HashMap<>();
        for (final var node : astNodes) {
            final ASTNode nodeCopy;
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
                    final ASTNode parentCopy;
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
                final CustomHashSet<ASTNode> childrenOfCopy = nodeCopy.getChildren();
                for (final var child : children) {
                    final ASTNode childCopy;
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
                final ASTNode productEquivalentCopy;
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

    private static ASTNode copyNode(final ASTNode node) {
        try {
            final Formula mapping;
            if (node.getMapping() == null) {
                mapping = null;
            } else {
                mapping = TraceBoosting.f.parse(node.getMapping().toString());
            }
            return new ASTNode(null, node.getCode(), node.getStartPosition(), node.getType(), mapping);
        } catch (final ParserException e) {
            throw new RuntimeException("Was not able to parse mapping.");
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Product))
            return false;
        final Product product = (Product) o;
        return features.equals(product.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(features);
    }
}
