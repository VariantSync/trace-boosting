package de.hub.mse.variantsync.boosting.ecco;

import de.hub.mse.variantsync.boosting.TraceBoosting;
import de.hub.mse.variantsync.boosting.parsing.AbstractAST;
import de.hub.mse.variantsync.boosting.position.ProductPosition;
import de.hub.mse.variantsync.boosting.position.UnspecifiedPosition;
import de.hub.mse.variantsync.boosting.product.Product;

import org.logicng.formulas.Formula;

import java.io.Serializable;
import java.util.*;

public class MainTree implements Serializable {

    private final AbstractAST tree;
    private final Map<EccoNode, Set<ProductPosition>> positionMap;
    private Map<ProductPosition, EccoNode> inversePositionMap;

    public MainTree(final AbstractAST tree) {
        this.tree = tree;
        positionMap = new HashMap<>();
        inversePositionMap = null;
    }

    // merge this AST into another AST (main tree) and return the set of all nodes
    // in the resulting main tree corresponding to the nodes of this AST
    public EccoSet<EccoNode> unite(final Product product) {
        final EccoSet<EccoNode> result = new EccoSet<>();
        uniteChildren(result, product.getProductAst().getRoot(), tree.getRoot(), product);
        tree.getAstNodes().addAll(result);
        return result;
    }

    private void uniteChildren(final EccoSet<EccoNode> result, final EccoNode productNode, final EccoNode nodeMainTree,
            final Product product) {
        for (final EccoNode productChild : productNode.getChildren()) {
            Set<ProductPosition> productPositions = new HashSet<>();

            // Check whether there is a similar node somewhere among the descendants
            final EccoNode mainTreeEquivalent = findSimilarDescendant(productChild, nodeMainTree);
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
                final EccoNode childToAdd = new EccoNode(nodeMainTree, productChild.getCode(),
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

    private EccoNode findSimilarDescendant(final EccoNode productNode, final EccoNode nodeMainTree) {
        if (productNode.isSimilar(nodeMainTree)) {
            return nodeMainTree;
        } else if (nodeMainTree.getChildren().isEmpty()) {
            return null;
        } else {
            for (final EccoNode mainTreeChild : nodeMainTree.getChildren()) {
                final EccoNode foundDescendant = findSimilarDescendant(productNode, mainTreeChild);
                if (foundDescendant != null) {
                    return foundDescendant;
                }
            }
            return null;
        }
    }

    private void addAllSubNodes(final EccoSet<EccoNode> result, final EccoNode mainTreeParent,
            final EccoSet<EccoNode> productChildren, final Product product) {
        for (final EccoNode child : productChildren) {
            final EccoNode childCopy = new EccoNode(mainTreeParent, child.getCode(), UnspecifiedPosition.INSTANCE,
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

    public Formula getMapping(final ProductPosition position) {
        if (inversePositionMap == null) {
            initializeInversePositionMap();
        }
        return inversePositionMap.get(position).getMapping();
    }

    private void initializeInversePositionMap() {
        this.inversePositionMap = new HashMap<>();
        for (final EccoNode node : positionMap.keySet()) {
            for (final ProductPosition position : positionMap.get(node)) {
                this.inversePositionMap.put(position, node);
            }
        }
    }

    public Set<ProductPosition> getProductPositions(final EccoNode node) {
        return positionMap.get(node);
    }

    public AbstractAST getTree() {
        return this.tree;
    }

}
