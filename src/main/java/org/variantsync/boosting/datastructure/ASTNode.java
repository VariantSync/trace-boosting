package org.variantsync.boosting.datastructure;

import org.variantsync.boosting.TraceBoosting;
import org.variantsync.boosting.position.Position;

import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;

import java.io.*;
import java.util.Objects;

/**
 * Represents a node in an Abstract Syntax Tree (AST).
 * 
 * Each ASTNode contains information about a specific code element, such as a
 * class, method, or statement.
 * 
 */
public class ASTNode implements Serializable {

    public enum NODE_TYPE {
        ROOT, FOLDER, FILE, LINE, DEFAULT,
        CLASS_OR_INTERFACE_DECLARATION, METHOD_DECLARATION, IF_STATEMENT,
        ELSE_STATEMENT, THEN_STATEMENT, CONSTRUCTOR_DECLARATION,
        FOREACH_STATEMENT, FOR_STATEMENT, DO_STATEMENT, ENUM_CONSTANT_DECLARATION, ENUM_DECLARATION,
        SWITCH_ENTRY, SWITCH_STMT, MODULE_DECLARATION,
    }

    private final String code;
    private ASTNode parent;
    private final EccoSet<ASTNode> children;
    private final NODE_TYPE type;
    private transient Position startPosition;
    private transient Formula mapping;
    private ASTNode productEquivalent;
    private int sequenceNumber = 0;

    /**
     * Constructs a new ASTNode with the specified parameters.
     * 
     */
    public ASTNode(final ASTNode parent, final String code, final Position position, final NODE_TYPE type,
            final Formula mapping) {
        this.parent = parent;
        this.code = code;
        this.startPosition = Objects.requireNonNull(position);
        this.type = type;
        this.mapping = mapping;
        children = new EccoSet<>();
        productEquivalent = null;
    }

    // if two nodes are at the same position of the AST and contain the same code,
    // we distinguish them by sequence numbers
    /*
     * Warning: Later on we will match nodes from different products by their
     * sequence numbers, but that is not necessarily correct (they can e.g. be
     * shifted by inserts or deletes).
     * Correct matching of the sequence numbers would require further investigation
     * of the nodes' children and was omitted here.
     */
    public void addChild(final ASTNode child) {
        while (!children.add(child)) {
            child.sequenceNumber += 1;
        }
    }

    /**
     * Sets the equivalent node of this node.
     *
     * @param productEquivalent the equivalent AST node
     */
    public void setProductEquivalent(final ASTNode productEquivalent) {
        this.productEquivalent = productEquivalent;
    }

    /**
     * Gets the equivalent node of this node.
     */
    public ASTNode getProductEquivalent() {
        return this.productEquivalent;
    }

    /**
     * Checks whether the given ASTNode is similar to this one.
     *
     * @param eccoNode another ASTNode object
     */
    public boolean isSimilar(final ASTNode eccoNode) {
        if (this == eccoNode)
            return true;
        if (eccoNode == null || getClass() != eccoNode.getClass())
            return false;
        return Objects.equals(code, eccoNode.code) &&
                this.similarParent(eccoNode) &&
                type == eccoNode.type &&
                sequenceNumber == eccoNode.sequenceNumber;
    }

    /**
     * @return the code associated with this ASTNode
     */
    public String getCode() {
        return code;
    }

    private String getAncestorCode() {
        if (this.parent == null) {
            return null;
        } else {
            if (this.parent.code == null) {
                return this.parent.getAncestorCode();
            } else {
                return this.parent.code;
            }
        }
    }

    private boolean similarParent(final ASTNode other) {
        if (this.parent == other.parent)
            return true;
        if (this.parent == null || this.parent.getClass() != other.parent.getClass())
            return false;
        return Objects.equals(this.parent.code, other.parent.code) &&
                this.parent.type == other.parent.type &&
                this.parent.sequenceNumber == other.parent.sequenceNumber;
    }

    /**
     * @return the parent of this ASTNode
     */
    public ASTNode getParent() {
        return parent;
    }

    /**
     * Sets the parent of this ASTNode in the AST.
     */
    public void setParent(final ASTNode parent) {
        this.parent = parent;
    }

    /**
     * Returns a set of this node's child nodes.
     */
    public EccoSet<ASTNode> getChildren() {
        return children;
    }

    /**
     * Return the position (file, line) where the content of this node starts.
     */
    public Position getStartPosition() {
        if (productEquivalent != null) {
            return productEquivalent.startPosition;
        } else {
            return startPosition;
        }
    }

    /**
     * Returns set set of all possible mappings (if the mapping is a DNF formula,
     * each clause is a possible mapping on its own) for this node.
     */
    public EccoSet<Formula> getMappings() {
        final EccoSet<Formula> mappings = new EccoSet<>();
        if (mapping.type() == FType.OR) {
            for (final Formula clause : mapping) {
                mappings.add(clause);
            }
        } else {
            mappings.add(mapping);
        }
        return mappings;
    }

    /** Returns the best mapping as a formula */
    public Formula getMapping() {
        return mapping;
    }

    /** Sets a specific mapping for this node */
    public void setMapping(final Formula mapping) {
        this.mapping = mapping;
    }

    /** Gets the type of this node */
    public NODE_TYPE getType() {
        return type;
    }

    private void writeObject(final ObjectOutputStream oos)
            throws IOException {
        oos.defaultWriteObject();
        final Object o1;
        final Object o2;
        if (mapping != null) {
            o1 = mapping.toString();
        } else {
            o1 = null;
        }
        if (startPosition != null) {
            o2 = startPosition.serializedPosition();
        } else {
            o2 = null;
        }
        oos.writeObject(new Object[] { o1, o2 });
    }

    private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException, ParserException {
        ois.defaultReadObject();
        final Object[] transientObjects = (Object[]) ois.readObject();
        if (transientObjects[0] != null) {
            this.mapping = new PropositionalParser(TraceBoosting.f).parse((String) transientObjects[0]);
        }
        if (transientObjects[1] != null) {
            final String[] serializedPosition = (String[]) transientObjects[1];
            this.startPosition = Position.fromSerializedPosition(serializedPosition);
        }
    }
}
