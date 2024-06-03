package de.hub.mse.variantsync.ecco.data.specialization;

import de.hub.mse.variantsync.ecco.data.EccoNode;
import de.hub.mse.variantsync.ecco.data.EccoSet;

import java.io.File;

public class CAST extends LineAST {
    private static final String[] fileTypes = new String[] {".c", ".h"};
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
