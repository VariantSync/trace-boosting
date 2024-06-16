package org.variantsync.boosting.datastructure;

import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.parsing.AbstractAST;
import org.variantsync.boosting.position.ProductPosition;
import org.variantsync.boosting.position.UnspecifiedPosition;
import org.variantsync.boosting.product.Variant;

import org.logicng.formulas.Formula;

import java.io.Serializable;
import java.util.*;

/**
 * The MainTree represents the AST resulting from merging several product ASTs.
 */
public class MainTree implements Serializable {

    private final AbstractAST tree;
    private final Map<ASTNode, Set<ProductPosition>> positionMap;
    private Map<ProductPosition, ASTNode> inversePositionMap;

    /**
     * Initializes a MainTree object with the given AbstractAST tree.
     *
     * @param tree the AbstractAST tree to be set for this MainTree
     */
    public MainTree(final AbstractAST tree) {
        this.tree = tree;
        positionMap = new HashMap<>();
        inversePositionMap = null;
    }

    /**
     * Merges the Abstract Syntax Tree (AST) of the given product into the of this
     * main AST (main tree) and returns the set of all nodes
     * in the resulting main tree corresponding to the nodes of this AST.
     *
     * @param variant The product containing the main tree to merge this AST into.
     * @return The set of all nodes in the resulting main tree corresponding to the
     *         nodes of this AST.
     */
    public CustomHashSet<ASTNode> unite(final Variant variant) {
        final CustomHashSet<ASTNode> result = new CustomHashSet<>();
        uniteChildren(result, variant.getProductAst().getRoot(), tree.getRoot(), variant);
        tree.getAstNodes().addAll(result);
        return result;
    }

    private void uniteChildren(final CustomHashSet<ASTNode> result, final ASTNode productNode, final ASTNode nodeMainTree,
                               final Variant variant) {
        for (final ASTNode productChild : productNode.getChildren()) {
            Set<ProductPosition> productPositions = new HashSet<>();

            // Check whether there is a similar node somewhere among the descendants
            final ASTNode mainTreeEquivalent = findSimilarDescendant(productChild, nodeMainTree);
            if (mainTreeEquivalent != null) {
                result.add(mainTreeEquivalent);
                productPositions = positionMap.get(mainTreeEquivalent);
                // update mapping of the main tree node
                if (productChild.getMapping() != null) {
                    // only if they are not the same, but
                    if (mainTreeEquivalent.getMapping() != null && !mainTreeEquivalent.getMapping().toString()
                            .equals(productChild.getMapping().toString())) {
                        mainTreeEquivalent.setMapping(
                                TraceBoosting.f.or(mainTreeEquivalent.getMapping(), productChild.getMapping()));
                    } else {
                        mainTreeEquivalent.setMapping(productChild.getMapping());
                    }
                }
                uniteChildren(result, productChild, mainTreeEquivalent, variant);
            } else {
                // add a copy of the product child node to the main tree
                final ASTNode childToAdd = new ASTNode(nodeMainTree, productChild.getCode(),
                        UnspecifiedPosition.INSTANCE, productChild.getType(), productChild.getMapping());
                nodeMainTree.addChild(childToAdd);
                childToAdd.setParent(nodeMainTree);
                result.add(childToAdd);
                positionMap.put(childToAdd, productPositions);
                // add the new node and all its children to the result (since they become
                // corresponding nodes of the main tree)
                addAllSubNodes(result, childToAdd, productChild.getChildren(), variant);
            }
            productPositions.add(new ProductPosition(variant, productChild.getStartPosition()));
        }
    }

    private ASTNode findSimilarDescendant(final ASTNode productNode, final ASTNode nodeMainTree) {
        if (productNode.isSimilar(nodeMainTree)) {
            return nodeMainTree;
        } else if (nodeMainTree.getChildren().isEmpty()) {
            return null;
        } else {
            for (final ASTNode mainTreeChild : nodeMainTree.getChildren()) {
                final ASTNode foundDescendant = findSimilarDescendant(productNode, mainTreeChild);
                if (foundDescendant != null) {
                    return foundDescendant;
                }
            }
            return null;
        }
    }

    private void addAllSubNodes(final CustomHashSet<ASTNode> result, final ASTNode mainTreeParent,
                                final CustomHashSet<ASTNode> productChildren, final Variant variant) {
        for (final ASTNode child : productChildren) {
            final ASTNode childCopy = new ASTNode(mainTreeParent, child.getCode(), UnspecifiedPosition.INSTANCE,
                    child.getType(), child.getMapping());
            mainTreeParent.addChild(childCopy);
            childCopy.setParent(mainTreeParent);
            result.add(childCopy);
            final Set<ProductPosition> productPositions = new HashSet<>();
            productPositions.add(new ProductPosition(variant, child.getStartPosition()));
            positionMap.put(childCopy, productPositions);

            productPositions.add(new ProductPosition(variant, child.getStartPosition()));
            addAllSubNodes(result, childCopy, child.getChildren(), variant);
        }
    }

    /**
     * Retrieves the mapping formula associated with a given product position.
     * 
     * @param position The product position for which to retrieve the mapping
     *                 formula
     * @return The mapping formula associated with the given product position
     * @throws NullPointerException if the inversePositionMap is not initialized
     */
    public Formula getMapping(final ProductPosition position) {
        if (inversePositionMap == null) {
            initializeInversePositionMap();
        }
        return inversePositionMap.get(position).getMapping();
    }

    private void initializeInversePositionMap() {
        this.inversePositionMap = new HashMap<>();
        for (final ASTNode node : positionMap.keySet()) {
            for (final ProductPosition position : positionMap.get(node)) {
                this.inversePositionMap.put(position, node);
            }
        }
    }

    /**
     * Retrieves the set of ProductPositions associated with the given ASTNode.
     * 
     * @param node the ASTNode for which to retrieve the ProductPositions
     * @return a Set of ProductPositions associated with the given ASTNode
     */
    public Set<ProductPosition> getProductPositions(final ASTNode node) {
        return positionMap.get(node);
    }

    /**
     * Retrieves the AbstractAST tree associated with this MainTree.
     * 
     * @return the AbstractAST tree associated with this object
     */
    public AbstractAST getTree() {
        return this.tree;
    }

}
