package de.hub.mse.variantsync.boosting.parsing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults;

import de.hub.mse.variantsync.boosting.ecco.ASTNode;
import de.hub.mse.variantsync.boosting.position.LinePosition;
import de.hub.mse.variantsync.boosting.position.Position;

import org.tinylog.Logger;

public class JavaVisitor extends VoidVisitorWithDefaults<ASTNode> {
    private final String visitedFile;

    public JavaVisitor(final String visitedFile) {
        this.visitedFile = visitedFile;
    }

    public Position parsePosition(final com.github.javaparser.Position parserPosition) {
        return new LinePosition(visitedFile, parserPosition.line, parserPosition.column);
    }

    @Override
    public void visit(final CompilationUnit n, final ASTNode parent) {
        Logger.debug("Visiting CompilationUnit " + n.toString());
        visit(n.getTypes(), parent);
        for (final ASTNode child : parent.getChildren()) {
            if (n.getType(0).getNameAsString().equals(child.getCode())) {
                visit(n.getImports(), child);
                break;
            }
        }
    }

    @Override
    public void visit(final NodeList nodeList, final ASTNode parent) {
        Logger.debug("Visiting NodeList");
        for (final Object node : nodeList) {
            ((Node) node).accept(this, parent);
        }
    }

    @Override
    public void visit(final IfStmt n, final ASTNode parent) {
        Logger.debug("Visiting if statement");
        final ASTNode child = new ASTNode(parent, n.getCondition().toString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.IF_STATEMENT, null);
        parent.addChild(child);
        final ASTNode thenChild = new ASTNode(child, null,
                n.getThenStmt().getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.THEN_STATEMENT, null);
        child.addChild(thenChild);
        n.getThenStmt().accept(this, thenChild);
        if (n.getElseStmt().isPresent()) {
            final ASTNode elseChild = new ASTNode(child, null,
                    n.getElseStmt().get().getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                    ASTNode.NODE_TYPE.ELSE_STATEMENT, null);
            child.addChild(elseChild);
            n.getElseStmt().get().accept(this, elseChild);
        }
    }

    @Override
    public void visit(final MethodDeclaration n, final ASTNode parent) {
        Logger.debug("Visiting method declaration");
        final NodeList<Parameter> params = n.getParameters();
        StringBuilder paramList = new StringBuilder();
        for (final Parameter parameter : params) {
            if (paramList.toString().equals("")) {
                paramList = new StringBuilder(parameter.getTypeAsString());
            } else {
                paramList.append(",").append(parameter.getTypeAsString());
            }
        }
        final ASTNode child = new ASTNode(parent, "" + n.getNameAsString() + "(" + paramList + ")",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.METHOD_DECLARATION, null);
        parent.addChild(child);
        if (n.getBody().isPresent())
            visit(n.getBody().get(), child);
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final ASTNode parent) {
        Logger.debug("Visiting class or interface");
        final ASTNode child = new ASTNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.CLASS_OR_INTERFACE_DECLARATION, null);
        parent.addChild(child);
        visit(n.getMembers(), child);
    }

    @Override
    public void visit(final BlockStmt n, final ASTNode parent) {
        Logger.debug("Visiting block statement");
        visit(n.getStatements(), parent);
    }

    @Override
    public void visit(final ForStmt n, final ASTNode parent) {
        Logger.debug("Visiting for statement");
        final ASTNode child;
        if (n.getCompare().isPresent()) {
            child = new ASTNode(parent,
                    "for(" + n.getInitialization() + "; " + n.getCompare().get() + "; " + n.getUpdate() + ")",
                    n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                    ASTNode.NODE_TYPE.FOR_STATEMENT, null);
            n.getCompare().get().accept(this, child);
        } else
            child = new ASTNode(parent, "for(" + n.getInitialization() + "; ; " + n.getUpdate() + ")",
                    n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                    ASTNode.NODE_TYPE.FOR_STATEMENT, null);
        parent.addChild(child);
        n.getBody().accept(this, child);
    }

    @Override
    public void visit(final ForEachStmt n, final ASTNode parent) {
        Logger.debug("Visiting for-each statement");
        final ASTNode child = new ASTNode(parent, "for(" + n.getVariable() + ": " + n.getIterable() + ")",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.FOREACH_STATEMENT, null);
        parent.addChild(child);
        n.getBody().accept(this, child);
    }

    @Override
    public void visit(final ConstructorDeclaration n, final ASTNode parent) {
        Logger.debug("Visiting constructor");
        final ASTNode child = new ASTNode(parent, n.getDeclarationAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.CONSTRUCTOR_DECLARATION, null);
        parent.addChild(child);
        visit(n.getBody(), child);
    }

    @Override
    public void visit(final DoStmt n, final ASTNode parent) {
        Logger.debug("Visiting do statement");
        final ASTNode child = new ASTNode(parent, "do statement",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.DO_STATEMENT, null);
        parent.addChild(child);
        n.getBody().accept(this, child);
    }

    @Override
    public void visit(final EnumConstantDeclaration n, final ASTNode parent) {
        Logger.debug("Visiting enum constant declaration");
        final ASTNode child = new ASTNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.ENUM_CONSTANT_DECLARATION, null);
        parent.addChild(child);
        n.getArguments().accept(this, child);
    }

    @Override
    public void visit(final EnumDeclaration n, final ASTNode parent) {
        Logger.debug("Visiting enum declaration");
        final ASTNode child = new ASTNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.ENUM_DECLARATION, null);
        parent.addChild(child);
        n.getEntries().accept(this, child);
    }

    @Override
    public void visit(final SwitchEntry n, final ASTNode parent) {
        Logger.debug("Visiting switch entry");
        final ASTNode child = new ASTNode(parent, "case " + n.getLabels().toString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.SWITCH_ENTRY, null);
        parent.addChild(child);
        n.getStatements().accept(this, child);
    }

    @Override
    public void visit(final SwitchStmt n, final ASTNode parent) {
        Logger.debug("Visiting switch statement");
        final ASTNode child = new ASTNode(parent, "switch(" + n.getSelector().toString() + ")",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.SWITCH_STMT, null);
        parent.addChild(child);
        n.getEntries().accept(this, child);
    }

    @Override
    public void visit(final ModuleDeclaration n, final ASTNode parent) {
        Logger.debug("Visiting module declaration");
        final ASTNode child = new ASTNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                ASTNode.NODE_TYPE.MODULE_DECLARATION, null);
        parent.addChild(child);
        n.getAnnotations().accept(this, child);
    }

    @Override
    public void defaultAction(final Node n, final ASTNode parent) {
        Logger.debug("Visiting default entry");
        final ASTNode child = new ASTNode(parent, n.toString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null, ASTNode.NODE_TYPE.DEFAULT,
                null);
        parent.addChild(child);
    }
}
