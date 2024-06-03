package de.hub.mse.variantsync.boosting.ecco;

import de.hub.mse.variantsync.boosting.TraceBoosting;
import de.hub.mse.variantsync.boosting.parsing.AbstractAST;
import de.hub.mse.variantsync.boosting.position.ProductPosition;
import de.hub.mse.variantsync.boosting.position.UnspecifiedPosition;
import de.hub.mse.variantsync.boosting.product.Product;

import org.logicng.formulas.Formula;

import java.io.Serializable;
import java.util.*;

/**
 * Represents the merged AST of several product ASTs.
 */
public class MainTree implements Serializable {

    private final AbstractAST tree;
    private final Map<ASTNode, Set<ProductPosition>> positionMap;
    private Map<ProductPosition, ASTNode> inversePositionMap;

    /**
     * Constructs a MainTree object with the given AbstractAST tree.
     *
     * @param tree the AbstractAST tree to be set for this MainTree
     */
    public MainTree(final AbstractAST tree) {
        this.tree = tree;
        positionMap = new HashMap<>();
        inversePositionMap = null;
    }

    /**
     * Merge this Abstract Syntax Tree (AST) into another AST (main tree) and return
     * the set of all nodes
     * in the resulting main tree corresponding to the nodes of this AST.
     *
     * @param product The product containing the main tree to merge this AST into.
     * @return The set of all nodes in the resulting main tree corresponding to the
     *         nodes of this AST.
     */
    public EccoSet<ASTNode> unite(final Product product) {
        final EccoSet<ASTNode> result = new EccoSet<>();
        uniteChildren(result, product.getProductAst().getRoot(), tree.getRoot(), product);
        tree.getAstNodes().addAll(result);
        return result;
    }

    private void uniteChildren(final EccoSet<ASTNode> result, final ASTNode productNode, final ASTNode nodeMainTree,
            final Product product) {
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
                    // TODO how can we compare the formulas, better option than with string?
                    if (mainTreeEquivalent.getMapping() != null && !mainTreeEquivalent.getMapping().toString()
                            .equals(productChild.getMapping().toString())) {
                        mainTreeEquivalent.setMapping(
                                TraceBoosting.f.or(mainTreeEquivalent.getMapping(), productChild.getMapping()));
                    } else {
                        mainTreeEquivalent.setMapping(productChild.getMapping());
                    }
                }
                uniteChildren(result, productChild, mainTreeEquivalent, product);
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
                addAllSubNodes(result, childToAdd, productChild.getChildren(), product);
            }
            productPositions.add(new ProductPosition(product, productChild.getStartPosition()));
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

    private void addAllSubNodes(final EccoSet<ASTNode> result, final ASTNode mainTreeParent,
            final EccoSet<ASTNode> productChildren, final Product product) {
        for (final ASTNode child : productChildren) {
            final ASTNode childCopy = new ASTNode(mainTreeParent, child.getCode(), UnspecifiedPosition.INSTANCE,
                    child.getType(), child.getMapping());
            mainTreeParent.addChild(childCopy);
            childCopy.setParent(mainTreeParent);
            result.add(childCopy);
            final Set<ProductPosition> productPositions = new HashSet<>();
            productPositions.add(new ProductPosition(product, child.getStartPosition()));
            positionMap.put(childCopy, productPositions);

            productPositions.add(new ProductPosition(product, child.getStartPosition()));
            addAllSubNodes(result, childCopy, child.getChildren(), product);
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
