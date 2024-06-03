package de.hub.mse.variantsync.boosting.data.specialization;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import de.hub.mse.variantsync.boosting.data.AbstractAST;
import de.hub.mse.variantsync.boosting.data.EccoNode;
import de.hub.mse.variantsync.boosting.data.EccoSet;

import java.io.File;

public class JavaAST extends AbstractAST {
    public JavaAST() {
        super(".java");
    }

    public JavaAST(final File rootFile) {
        super(rootFile, ".java");
    }

    public JavaAST(final EccoNode root, final EccoSet<EccoNode> astNodes) {
        super(root, astNodes, ".java");
    }

    @Override
    protected void visitFileContent(final EccoNode fileNode, final File fileToVisit) {
        final SourceRoot sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(JavaAST.class)
                .resolve(fileToVisit.getParentFile().getAbsolutePath()));
        final CompilationUnit cu = sourceRoot.parse("", fileToVisit.getName());
        cu.accept(new JavaVisitor(fileToVisit.toString()), fileNode);
    }
}
