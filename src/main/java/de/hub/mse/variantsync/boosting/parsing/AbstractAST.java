package de.hub.mse.variantsync.boosting.parsing;

import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.ecco.EccoSet;
import de.hub.mse.variantsync.boosting.position.FilePosition;
import de.hub.mse.variantsync.boosting.position.RootPosition;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * AbstractAST is an abstract class representing an Abstract Syntax Tree (AST)
 * with a root node and a set of AST nodes.
 * This class provides constructors to create an AST with optional filtering
 * based on file types.
 */
public abstract class AbstractAST implements Serializable {
    protected final ASTNode root;
    protected final EccoSet<ASTNode> astNodes;
    protected final Set<String> fileTypes;

    /**
     * Constructs an AbstractAST with no filtering based on file types.
     * 
     * @param fileTypes an array of file types to filter the AST nodes by
     */
    public AbstractAST(final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        root = new ASTNode(null, null, RootPosition.INSTANCE, ASTNode.NODE_TYPE.ROOT, null);
        astNodes = new EccoSet<>();
    }

    /**
     * Constructs an AbstractAST with filtering based on file types.
     * 
     * @param rootFile  the root file to build the AST from
     * @param fileTypes an array of file types to filter the AST nodes by
     */
    public AbstractAST(final File rootFile, final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        root = new ASTNode(null, null, RootPosition.INSTANCE, ASTNode.NODE_TYPE.ROOT, null);
        visitFile(root, rootFile);
        astNodes = collectAstNodes();
    }

    /**
     * Constructs an AbstractAST with a specified root node, set of AST nodes, and
     * optional filtering based on file types.
     * 
     * @param root      the root node of the AST
     * @param astNodes  the set of AST nodes in the tree
     * @param fileTypes an array of file types to filter the AST nodes by
     */
    public AbstractAST(final ASTNode root, final EccoSet<ASTNode> astNodes,
            final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        this.root = root;
        this.astNodes = astNodes;
    }

    /**
     * Collects all nodes (except the root node) of the Abstract Syntax Tree (AST)
     * in one set to simplify their access.
     * 
     * @return a set containing all nodes of the AST
     */
    public EccoSet<ASTNode> collectAstNodes() {
        final EccoSet<ASTNode> result = new EccoSet<>();
        final ArrayList<ASTNode> nodesToVisit = new ArrayList<>();
        nodesToVisit.add(this.getRoot());
        while (!nodesToVisit.isEmpty()) {
            result.add(nodesToVisit.get(0));
            nodesToVisit.addAll(nodesToVisit.remove(0).getChildren());
        }
        result.remove(root);
        return result;
    }

    /**
     * Creates the AST from a file.
     * 
     * @param parent     - the parent ASTNode to which the file belongs
     * @param parentFile - the File object representing the parent directory
     */
    private void visitFile(final ASTNode parent, final File parentFile) {
        final File[] children = parentFile.listFiles();
        if (children == null)
            return;
        for (final File childFile : children) {
            if (childFile.isFile()) {
                final ASTNode fileNode = new ASTNode(parent, childFile.getName(),
                        new FilePosition(childFile.toString()), ASTNode.NODE_TYPE.FILE, null);
                parent.addChild(fileNode);
                if (fileTypes.isEmpty()) {
                    // If there are no file types specified, we assume that all should be used
                    visitFileContent(fileNode, childFile);
                } else if (fileTypes.stream()
                        .anyMatch(t -> childFile.getAbsolutePath().endsWith(t))) {
                    visitFileContent(fileNode, childFile);
                }
            } else if (childFile.isDirectory()) {
                final ASTNode directoryNode = new ASTNode(parent, childFile.getName(),
                        new FilePosition(childFile.toString()), ASTNode.NODE_TYPE.FOLDER, null);
                parent.addChild(directoryNode);
                visitFile(directoryNode, childFile);
            } else {
                System.out.println("File error, neither file nor directory");
            }
        }
    }

    /**
     * This method is responsible for visiting the content of a file represented by
     * the given ASTNode and File object.
     * 
     * @param fileNode    The ASTNode representing the content of the file to be
     *                    visited.
     * @param fileToVisit The File object representing the file to be visited.
     * 
     * @throws NullPointerException     if either fileNode or fileToVisit is null.
     * @throws IllegalArgumentException if the fileToVisit does not exist or is not
     *                                  a valid file.
     */
    protected abstract void visitFileContent(final ASTNode fileNode, final File fileToVisit);

    /**
     * Returns the root node of the Abstract Syntax Tree (AST).
     *
     * @return the root node of the AST
     */
    public ASTNode getRoot() {
        return root;
    }

    /**
     * Returns a set of all AST nodes in the tree.
     *
     * @return a set of all AST nodes
     */
    public EccoSet<ASTNode> getAstNodes() {
        return astNodes;
    }

}
