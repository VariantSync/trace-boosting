package de.hub.mse.variantsync.ecco.data.specialization;

import de.hub.mse.variantsync.ecco.data.*;
import de.hub.mse.variantsync.ecco.data.position.LinePosition;
import de.hub.mse.variantsync.ecco.data.position.Position;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

public class LineAST extends AbstractAST {
    private static final int LINE_BASE_INDEX = 0;

    public LineAST(final String... fileTypes) {
        super(fileTypes);
    }

    public LineAST(final File rootFile, final String... fileTypes) {
        super(rootFile, fileTypes);
    }

    public LineAST(final EccoNode root, final EccoSet<EccoNode> astNodes, final String... fileTypes) {
        super(root, astNodes, fileTypes);
    }

    @Override
    protected void visitFileContent(final EccoNode fileNode, final File fileToVisit) {
        // Create an EccoNode for each line in the file
        try {
            final List<String> lines = Files.readAllLines(fileToVisit.toPath());
            int lineIndex = LINE_BASE_INDEX;
            for (final String line : lines) {
                final Position position = new LinePosition(fileToVisit.toString(), lineIndex, 0);
                final EccoNode lineNode = new EccoNode(fileNode, line, position, EccoNode.NODE_TYPE.LINE, null);
                fileNode.addChild(lineNode);
                lineIndex++;
            }
        } catch (final IOException e) {
            e.printStackTrace();
            Logger.error("Was not able to read file " + fileToVisit, e);
            throw new UncheckedIOException(e);
        }
    }
}
