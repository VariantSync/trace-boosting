package de.hub.mse.variantsync.ecco.data.specialization;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults;
import de.hub.mse.variantsync.ecco.data.EccoNode;
import de.hub.mse.variantsync.ecco.data.position.LinePosition;
import de.hub.mse.variantsync.ecco.data.position.Position;
import org.tinylog.Logger;

public class JavaVisitor extends VoidVisitorWithDefaults<EccoNode> {
    private final String visitedFile;

    public JavaVisitor(final String visitedFile) {
        this.visitedFile = visitedFile;
    }

    public Position parsePosition(final com.github.javaparser.Position parserPosition) {
        return new LinePosition(visitedFile, parserPosition.line, parserPosition.column);
    }

    @Override
    public void visit(final CompilationUnit n, final EccoNode parent) {
        Logger.debug("Visiting CompilationUnit " + n.toString());
        visit(n.getTypes(), parent);
        for (final EccoNode child : parent.getChildren()) {
            if (n.getType(0).getNameAsString().equals(child.getCode())) {
                visit(n.getImports(), child);
                break;
            }
        }
    }

    @Override
    public void visit(final NodeList nodeList, final EccoNode parent) {
        Logger.debug("Visiting NodeList");
        for (final Object node : nodeList) {
            ((Node) node).accept(this, parent);
        }
    }

    @Override
    public void visit(final IfStmt n, final EccoNode parent) {
        Logger.debug("Visiting if statement");
        final EccoNode child = new EccoNode(parent, n.getCondition().toString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.IF_STATEMENT, null);
        parent.addChild(child);
        final EccoNode thenChild = new EccoNode(child, null,
                n.getThenStmt().getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.THEN_STATEMENT, null);
        child.addChild(thenChild);
        n.getThenStmt().accept(this, thenChild);
        if (n.getElseStmt().isPresent()) {
            final EccoNode elseChild = new EccoNode(child, null,
                    n.getElseStmt().get().getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                    EccoNode.NODE_TYPE.ELSE_STATEMENT, null);
            child.addChild(elseChild);
            n.getElseStmt().get().accept(this, elseChild);
        }
    }

    @Override
    public void visit(final MethodDeclaration n, final EccoNode parent) {
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
        final EccoNode child = new EccoNode(parent, "" + n.getNameAsString() + "(" + paramList + ")",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.METHOD_DECLARATION, null);
        parent.addChild(child);
        if (n.getBody().isPresent())
            visit(n.getBody().get(), child);
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final EccoNode parent) {
        Logger.debug("Visiting class or interface");
        final EccoNode child = new EccoNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.CLASS_OR_INTERFACE_DECLARATION, null);
        parent.addChild(child);
        visit(n.getMembers(), child);
    }

    @Override
    public void visit(final BlockStmt n, final EccoNode parent) {
        Logger.debug("Visiting block statement");
        visit(n.getStatements(), parent);
    }

    @Override
    public void visit(final ForStmt n, final EccoNode parent) {
        Logger.debug("Visiting for statement");
        final EccoNode child;
        if (n.getCompare().isPresent()) {
            child = new EccoNode(parent,
                    "for(" + n.getInitialization() + "; " + n.getCompare().get() + "; " + n.getUpdate() + ")",
                    n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                    EccoNode.NODE_TYPE.FOR_STATEMENT, null);
            n.getCompare().get().accept(this, child);
        } else
            child = new EccoNode(parent, "for(" + n.getInitialization() + "; ; " + n.getUpdate() + ")",
                    n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                    EccoNode.NODE_TYPE.FOR_STATEMENT, null);
        parent.addChild(child);
        n.getBody().accept(this, child);
    }

    @Override
    public void visit(final ForEachStmt n, final EccoNode parent) {
        Logger.debug("Visiting for-each statement");
        final EccoNode child = new EccoNode(parent, "for(" + n.getVariable() + ": " + n.getIterable() + ")",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.FOREACH_STATEMENT, null);
        parent.addChild(child);
        n.getBody().accept(this, child);
    }

    @Override
    public void visit(final ConstructorDeclaration n, final EccoNode parent) {
        Logger.debug("Visiting constructor");
        final EccoNode child = new EccoNode(parent, n.getDeclarationAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.CONSTRUCTOR_DECLARATION, null);
        parent.addChild(child);
        visit(n.getBody(), child);
    }

    @Override
    public void visit(final DoStmt n, final EccoNode parent) {
        Logger.debug("Visiting do statement");
        final EccoNode child = new EccoNode(parent, "do statement",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.DO_STATEMENT, null);
        parent.addChild(child);
        n.getBody().accept(this, child);
    }

    @Override
    public void visit(final EnumConstantDeclaration n, final EccoNode parent) {
        Logger.debug("Visiting enum constant declaration");
        final EccoNode child = new EccoNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.ENUM_CONSTANT_DECLARATION, null);
        parent.addChild(child);
        n.getArguments().accept(this, child);
    }

    @Override
    public void visit(final EnumDeclaration n, final EccoNode parent) {
        Logger.debug("Visiting enum declaration");
        final EccoNode child = new EccoNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.ENUM_DECLARATION, null);
        parent.addChild(child);
        n.getEntries().accept(this, child);
    }

    @Override
    public void visit(final SwitchEntry n, final EccoNode parent) {
        Logger.debug("Visiting switch entry");
        final EccoNode child = new EccoNode(parent, "case " + n.getLabels().toString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.SWITCH_ENTRY, null);
        parent.addChild(child);
        n.getStatements().accept(this, child);
    }

    @Override
    public void visit(final SwitchStmt n, final EccoNode parent) {
        Logger.debug("Visiting switch statement");
        final EccoNode child = new EccoNode(parent, "switch(" + n.getSelector().toString() + ")",
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.SWITCH_STMT, null);
        parent.addChild(child);
        n.getEntries().accept(this, child);
    }

    @Override
    public void visit(final ModuleDeclaration n, final EccoNode parent) {
        Logger.debug("Visiting module declaration");
        final EccoNode child = new EccoNode(parent, n.getNameAsString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null,
                EccoNode.NODE_TYPE.MODULE_DECLARATION, null);
        parent.addChild(child);
        n.getAnnotations().accept(this, child);
    }

    @Override
    public void defaultAction(final Node n, final EccoNode parent) {
        Logger.debug("Visiting default entry");
        final EccoNode child = new EccoNode(parent, n.toString(),
                n.getRange().isPresent() ? parsePosition(n.getRange().get().begin) : null, EccoNode.NODE_TYPE.DEFAULT,
                null);
        parent.addChild(child);
    }
}
