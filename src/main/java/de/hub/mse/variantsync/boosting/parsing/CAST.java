package de.hub.mse.variantsync.boosting.parsing;

import java.io.File;

import de.hub.mse.variantsync.boosting.ecco.EccoNode;
import de.hub.mse.variantsync.boosting.ecco.EccoSet;

public class CAST extends LineAST {
    private static final String[] fileTypes = new String[] { ".c", ".h" };

    public CAST() {
        super(fileTypes);
    }

    public CAST(final File rootFile) {
        super(rootFile, fileTypes);
    }

    public CAST(final EccoNode root, final EccoSet<EccoNode> astNodes) {
        super(root, astNodes, fileTypes);
    }
}
