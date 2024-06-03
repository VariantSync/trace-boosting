package de.hub.mse.variantsync.boosting.parsing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;

import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.ecco.EccoSet;

import java.io.File;

/**
 * Represents an Abstract Syntax Tree (AST) for Java files.
 * This class extends the AbstractAST class and provides specific functionality
 * for Java files.
 * 
 */
public class JavaAST extends AbstractAST {
    public JavaAST() {
        super(".java");
    }

    public JavaAST(final File rootFile) {
        super(rootFile, ".java");
    }

    public JavaAST(final ASTNode root, final EccoSet<ASTNode> astNodes) {
        super(root, astNodes, ".java");
    }

    @Override
    protected void visitFileContent(final ASTNode fileNode, final File fileToVisit) {
        final SourceRoot sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(JavaAST.class)
                .resolve(fileToVisit.getParentFile().getAbsolutePath()));
        final CompilationUnit cu = sourceRoot.parse("", fileToVisit.getName());
        cu.accept(new JavaVisitor(fileToVisit.toString()), fileNode);
    }
}
