package org.variantsync.boosting.parsing;

import java.io.File;

import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.datastructure.CustomHashSet;

/**
 * Represents a Syntax Tree (CAST) for C source code files.
 * 
 * This class extends LineAST and provides functionality specific to C source
 * code files.
 * 
 * The supported file types for this CAST are ".c" and ".h".
 */
public class CAST extends LineAST {
    private static final String[] fileTypes = new String[] { ".c", ".h" };

    /**
     * Constructs a new CAST object with default file types ".c" and ".h".
     */
    public CAST() {
        super(fileTypes);
    }

    /**
     * Constructs a new CAST object with the specified root file and default file
     * types ".c" and ".h".
     * 
     * @param rootFile The root file for the CAST.
     */
    public CAST(final File rootFile) {
        super(rootFile, fileTypes);
    }

    /**
     * Constructs a new CAST object with the specified root node and AST nodes, and
     * default file types ".c" and ".h".
     * 
     * @param root     The root node for the CAST.
     * @param astNodes The set of AST nodes for the CAST.
     */
    public CAST(final ASTNode root, final CustomHashSet<ASTNode> astNodes) {
        super(root, astNodes, fileTypes);
    }
}
