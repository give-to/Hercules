package org.example.hercules.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class StatementFinder {
    private final Map<String, CompilationUnit> fileIndex = new HashMap<>();

    public StatementFinder() {
    }


    private Optional<Node> findSmartStatementAtLine(File javaFile, int lineNumber)
            throws FileNotFoundException {
        CompilationUnit cu;
        if (fileIndex != null && fileIndex.containsKey(javaFile.getAbsolutePath())) {
            cu = fileIndex.get(javaFile.getAbsolutePath());
        } else {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(javaFile);
            if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
                return Optional.empty();
            }
            cu = parseResult.getResult().get();
            fileIndex.put(javaFile.getAbsolutePath(), cu);
        }
        //inner class
        Optional<ClassOrInterfaceDeclaration> classDecl = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> isExactlyAtLine(c, lineNumber))
                .findFirst();
        if (classDecl.isPresent()) {
            return Optional.of(classDecl.get());
        }

        Optional<MethodDeclaration> method = cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> isExactlyAtLine(m, lineNumber))
                .findFirst();

        if (method.isPresent()) {
            return Optional.of(method.get());
        }

        Optional<ConstructorDeclaration> constructorMethod = cu.findAll(ConstructorDeclaration.class).stream()
                .filter(m -> isExactlyAtLine(m, lineNumber))
                .findFirst();

        if (constructorMethod.isPresent()) {
            return Optional.of(constructorMethod.get());
        }

        Optional<FieldDeclaration> field = cu.findAll(FieldDeclaration.class).stream()
                .filter(m -> isExactlyAtLine(m, lineNumber))
                .findFirst();
        if(field.isPresent()){
            return Optional.of(field.get());
        }

        List<Node> candidates = new ArrayList<>();
        cu.walk(ConstructorDeclaration.class, stmt -> {
            if (containsLine(stmt, lineNumber)) {
                candidates.add(stmt);
            }
        });
        cu.walk(MethodDeclaration.class, stmt -> {
            if (containsLine(stmt, lineNumber)) {
                candidates.add(stmt);
            }
        });
        cu.walk(EnumDeclaration.class, stmt -> {
            if (containsLine(stmt, lineNumber)) {
                candidates.add(stmt);
            }
        });
        cu.walk(EnumConstantDeclaration.class, stmt -> {
            if (containsLine(stmt, lineNumber)) {
                candidates.add(stmt);
            }
        });
        cu.walk(Statement.class, stmt -> {
            if (containsLine(stmt, lineNumber)) {
                candidates.add(stmt);
            }
        });
        cu.walk(SwitchEntry.class, stmt -> {
            if (containsLine(stmt, lineNumber)) {
                candidates.add(stmt);
            }
        });

        return selectSmartStatement(candidates, lineNumber);
    }
    private static Optional<Node> selectSmartStatement(List<Node> candidates, int targetLine) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Optional<Node> closingControl = candidates.stream()
                .filter(stmt -> isControlStatement(stmt) && isClosingLine(stmt, targetLine))
                .findFirst();

        if (closingControl.isPresent()) {
            return closingControl;
        }

        Optional<Node> controlCondition = candidates.stream()
                .filter(stmt -> isControlCondition(stmt, targetLine))
                .findFirst();

        if (controlCondition.isPresent()) {
            return controlCondition;
        }

        return candidates.stream()
                .min(Comparator.comparingInt(StatementFinder::getRangeSize));
    }

    private static boolean isExactlyAtLine(Node node, int lineNumber) {
        return node.getRange()
                .map(range -> {
                    int bodyStartLine  = range.begin.line;
                    if(node instanceof MethodDeclaration) {
                        Optional<BlockStmt> body = ((MethodDeclaration) node).getBody();
                        if (body.isPresent())
                            bodyStartLine = ((MethodDeclaration) node).getBody().get().getRange().get().begin.line;
                    }else if(node instanceof ConstructorDeclaration)
                        bodyStartLine = ((ConstructorDeclaration) node).getBody().getRange().get().begin.line;
                    return (range.begin.line <= lineNumber && lineNumber <= bodyStartLine) || range.end.line == lineNumber;
                })
                .orElse(false);
    }

    private static boolean isClosingLine(Node stmt, int targetLine) {
        return stmt.getRange()
                .map(range -> range.end.line == targetLine)
                .orElse(false);
    }

    private static boolean isControlStatement(Node stmt) {
        return stmt instanceof IfStmt ||
                stmt instanceof ForStmt ||
                stmt instanceof WhileStmt ||
                stmt instanceof DoStmt ||
                stmt instanceof SwitchStmt;
    }

    private static boolean isControlCondition(Node stmt, int targetLine) {
        if (stmt instanceof IfStmt) {
            return containsLine(((IfStmt) stmt).getCondition(), targetLine);
        }
        if (stmt instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) stmt;
            return forStmt.getRange().get().begin.line <= targetLine && targetLine <= forStmt.getBody().getRange().get().begin.line;
        }
        if (stmt instanceof WhileStmt) {
            return containsLine(((WhileStmt) stmt).getCondition(), targetLine);
        }
        if (stmt instanceof SwitchStmt) {
            return containsLine(((SwitchStmt) stmt).getSelector(), targetLine);
        }
        if(stmt instanceof SwitchEntry){
            boolean inside = false;
            for (int i = 0; i < ((SwitchEntry) stmt).getLabels().size(); i++) {
                inside = inside || containsLine(((SwitchEntry) stmt).getLabels().get(i), targetLine);
                if(inside)    break;
            }
            return inside;
        }
        return false;
    }

    private static int getRangeSize(Node node) {
        return node.getRange()
                .map(range -> range.end.line - range.begin.line)
                .orElse(Integer.MAX_VALUE);
    }

    private static boolean containsLine(Node node, int lineNumber) {
        return node.getRange()
                .map(range -> range.begin.line <= lineNumber && range.end.line >= lineNumber)
                .orElse(false);
    }

    public Node findStatementNode(String filePath, int targetLine) throws FileNotFoundException {
        File javaFile = new File(filePath);
        Optional<Node> statement = findSmartStatementAtLine(javaFile, targetLine);
        return statement.get();
    }

}