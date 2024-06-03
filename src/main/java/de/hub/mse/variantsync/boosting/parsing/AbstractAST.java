package de.hub.mse.variantsync.boosting.parsing;

import de.hub.mse.variantsync.boosting.ecco.EccoNode;
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
    protected final EccoNode root;
    protected final EccoSet<EccoNode> astNodes;
    // Used for filtering; no filtering is applied if the set is empty
    protected final Set<String> fileTypes;

    public AbstractAST(final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        root = new EccoNode(null, null, RootPosition.INSTANCE, EccoNode.NODE_TYPE.ROOT, null);
        astNodes = new EccoSet<>();
    }

    public AbstractAST(final File rootFile, final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        root = new EccoNode(null, null, RootPosition.INSTANCE, EccoNode.NODE_TYPE.ROOT, null);
        visitFile(root, rootFile);
        astNodes = collectAstNodes();
    }

    public AbstractAST(final EccoNode root, final EccoSet<EccoNode> astNodes,
            final String... fileTypes) {
        this.fileTypes = new HashSet<>();
        Collections.addAll(this.fileTypes, fileTypes);
        this.root = root;
        this.astNodes = astNodes;
    }

    // collects all nodes (except the root node) of the AST in one set to simplify
    // their access
    public EccoSet<EccoNode> collectAstNodes() {
        final EccoSet<EccoNode> result = new EccoSet<>();
        final ArrayList<EccoNode> nodesToVisit = new ArrayList<>();
        nodesToVisit.add(this.getRoot());
        while (!nodesToVisit.isEmpty()) {
            result.add(nodesToVisit.get(0));
            nodesToVisit.addAll(nodesToVisit.remove(0).getChildren());
        }
        result.remove(root);
        return result;
    }

    // create the AST from a file
    private void visitFile(final EccoNode parent, final File parentFile) {
        final File[] children = parentFile.listFiles();
        if (children == null)
            return;
        for (final File childFile : children) {
            if (childFile.isFile()) {
                final EccoNode fileNode = new EccoNode(parent, childFile.getName(),
                        new FilePosition(childFile.toString()), EccoNode.NODE_TYPE.FILE, null);
                parent.addChild(fileNode);
                if (fileTypes.isEmpty()) {
                    // If there are no file types specified, we assume that all should be used
                    visitFileContent(fileNode, childFile);
                } else if (fileTypes.stream()
                        .anyMatch(t -> childFile.getAbsolutePath().endsWith(t))) {
                    visitFileContent(fileNode, childFile);
                }
            } else if (childFile.isDirectory()) {
                final EccoNode directoryNode = new EccoNode(parent, childFile.getName(),
                        new FilePosition(childFile.toString()), EccoNode.NODE_TYPE.FOLDER, null);
                parent.addChild(directoryNode);
                visitFile(directoryNode, childFile);
            } else {
                System.out.println("File error, neither file nor directory");
            }
        }
    }

    protected abstract void visitFileContent(final EccoNode fileNode, final File fileToVisit);

    public EccoNode getRoot() {
        return root;
    }

    public EccoSet<EccoNode> getAstNodes() {
        return astNodes;
    }
}
