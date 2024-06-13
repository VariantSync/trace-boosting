package org.variantsync.boosting.parsing;

import org.tinylog.Logger;

import org.variantsync.boosting.datastructure.ASTNode;
import org.variantsync.boosting.datastructure.CustomHashSet;
import org.variantsync.boosting.position.LinePosition;
import org.variantsync.boosting.position.Position;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Represents a node in the Abstract Syntax Tree (AST) that represents a line of
 * code in a file.
 * This class extends AbstractAST and provides functionality to visit the
 * content of a file and create
 * AST nodes for each line in the file.
 *
 */
public class LineAST extends AbstractAST {
    private static final int LINE_BASE_INDEX = 0;

    /**
     * Constructs a LineAST object with the specified file types.
     *
     * @param fileTypes the file types to be used for parsing
     */
    public LineAST(final String... fileTypes) {
        super(fileTypes);
    }

    /**
     * Constructs a LineAST object with the specified root file and file types.
     *
     * @param rootFile  the root file to start parsing from
     * @param fileTypes the file types to be used for parsing
     */
    public LineAST(final File rootFile, final String... fileTypes) {
        super(rootFile, fileTypes);
    }

    /**
     * Constructs a LineAST object with the specified root node, AST nodes, and file
     * types.
     *
     * @param root      the root node of the AST
     * @param astNodes  the set of AST nodes
     * @param fileTypes the file types to be used for parsing
     */
    public LineAST(final ASTNode root, final CustomHashSet<ASTNode> astNodes, final String... fileTypes) {
        super(root, astNodes, fileTypes);
    }

    /**
     * This method is used to visit the content of a file and create an AST node for
     * each line in the file.
     * 
     * @param fileNode    The ASTNode representing the file being visited
     * @param fileToVisit The File object representing the file to be visited
     * @throws UncheckedIOException If an IOException occurs while reading the file
     */
    @Override
    protected void visitFileContent(final ASTNode fileNode, final File fileToVisit) {
        try {
            // Read all lines from the file
            final List<String> lines = Files.readAllLines(fileToVisit.toPath());
            int lineIndex = LINE_BASE_INDEX;

            // Create a node for each line in the file
            for (final String line : lines) {
                final Position position = new LinePosition(fileToVisit.toString(), lineIndex, 0);
                final ASTNode lineNode = new ASTNode(fileNode, line, position, ASTNode.NODE_TYPE.LINE, null);
                fileNode.addChild(lineNode);
                lineIndex++;
            }
        } catch (final IOException e) {
            // Handle any IOException that occurs
            e.printStackTrace();
            Logger.error("Was not able to read file " + fileToVisit, e);
            throw new UncheckedIOException(e);
        }
    }

}
