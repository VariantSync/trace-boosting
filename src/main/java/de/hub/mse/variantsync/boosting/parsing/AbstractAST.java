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

public abstract class AbstractAST implements Serializable {
    protected final ASTNode root;
    protected final EccoSet<ASTNode> astNodes;
    // Used for filtering; no filtering is applied if the set is empty
    protected final Set<String> fileTypes;

    public AbstractAST(final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        root = new ASTNode(null, null, RootPosition.INSTANCE, ASTNode.NODE_TYPE.ROOT, null);
        astNodes = new EccoSet<>();
    }

    public AbstractAST(final File rootFile, final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        root = new ASTNode(null, null, RootPosition.INSTANCE, ASTNode.NODE_TYPE.ROOT, null);
        visitFile(root, rootFile);
        astNodes = collectAstNodes();
    }

    public AbstractAST(final ASTNode root, final EccoSet<ASTNode> astNodes,
            final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        this.root = root;
        this.astNodes = astNodes;
    }

    // collects all nodes (except the root node) of the AST in one set to simplify
    // their access
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

    // create the AST from a file
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

    protected abstract void visitFileContent(final ASTNode fileNode, final File fileToVisit);

    public ASTNode getRoot() {
        return root;
    }

    public EccoSet<ASTNode> getAstNodes() {
        return astNodes;
    }
}
