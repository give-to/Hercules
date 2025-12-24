package org.example.hercules.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class VariableAssignmentAnalyzer {
    public static List<Integer> getContextAssignment(String filePath, int lineNumber) throws FileNotFoundException {
        List<Integer> lineList = new ArrayList<>();
        Map<String, Integer> result = findNearestAssignmentsInScope(filePath, lineNumber);
        result.forEach((varName, line) -> {
            lineList.add(line);
        });
        lineList.add(lineNumber);
        return lineList;
    }

    public static Map<String, Integer> findNearestAssignmentsInScope(String filePath, int targetLine)
            throws FileNotFoundException {
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = parser.parse(new File(filePath));
        Map<String, Integer> result = new HashMap<>();
        if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
            throw new RuntimeException("解析Java文件失败");
        }

        CompilationUnit cu = parseResult.getResult().get();

        TargetContextFinder contextFinder = new TargetContextFinder(filePath, targetLine);
        Set<String> variables = extractVariables(contextFinder.getTargetStatement(), targetLine);

        if (variables.isEmpty()) {
            return result;
        }

        return findNearestAssignments(
                contextFinder.getEnclosingMethod(),
                contextFinder.getTargetStatement(),
                variables);
    }

    private static Set<String> extractVariables(Node statement, int targetLine) {
        VariableExtractor extractor = new VariableExtractor(targetLine);
        statement.accept(extractor, null);
        return extractor.getVariables();
    }

    private static Map<String, Integer> findNearestAssignments(
            Node method,
            Node targetStatement,
            Set<String> variables) {

        AssignmentFinder finder = new AssignmentFinder(variables, targetStatement);
        method.accept(finder, null);
        return finder.getAssignment();
    }

    private static class TargetContextFinder extends VoidVisitorAdapter<Void> {
        private StatementFinder statementFinder = new StatementFinder();
        private final int targetLine;
        private Node targetStatement;
        private Node enclosingMethod;

        public TargetContextFinder(String filePath, int targetLine) throws FileNotFoundException {
            this.targetLine = targetLine;
            this.targetStatement = statementFinder.findStatementNode(filePath, targetLine);
            Node parentNode = this.targetStatement.getParentNode().get();
            if (parentNode instanceof CompilationUnit || parentNode instanceof ClassOrInterfaceDeclaration || parentNode instanceof ImportDeclaration || parentNode instanceof FieldDeclaration)
                this.enclosingMethod = parentNode;
            else {
                while (!(parentNode instanceof MethodDeclaration)) {
                    parentNode = parentNode.getParentNode().get();
                }
                this.enclosingMethod = parentNode;
            }
        }

        public Node getTargetStatement() {
            return targetStatement;
        }

        public Node getEnclosingMethod() {
            return enclosingMethod;
        }
    }

    private static class VariableExtractor extends VoidVisitorAdapter<Void> {
        private final Set<String> variables = new HashSet<>();
        private final int targetLine;

        private VariableExtractor(int targetLine) {
            this.targetLine = targetLine;
        }

        @Override
        public void visit(NameExpr nameExpr, Void arg) {
            if (nameExpr.getRange().get().begin.line <= targetLine && targetLine <= nameExpr.getRange().get().end.line)
                variables.add(nameExpr.getNameAsString());
            super.visit(nameExpr, arg);
        }

        public Set<String> getVariables() {
            return new HashSet<>(variables);
        }
    }

    private static class AssignmentFinder extends VoidVisitorAdapter<Void> {
        private final Set<String> targetVariables;
        private final Node targetStatement;
        private final Map<String, Stack<Integer>> nearestAssignments = new HashMap<>();
        private final Map<String, Integer> sameScopeAssignments = new HashMap<>();
        private final Deque<Map<String, Integer>> scopeStack = new ArrayDeque<>();
        private boolean foundTarget = false;

        public AssignmentFinder(Set<String> targetVariables, Node targetStatement) {
            this.targetVariables = targetVariables;
            this.targetStatement = targetStatement;
            scopeStack.push(new HashMap<>());
        }

        @Override
        public void visit(BlockStmt block, Void arg) {
            if (foundTarget) return;

            scopeStack.push(new HashMap<>());
            super.visit(block, arg);

            // 检查当前块是否包含目标语句
            if ((block.getStatements().contains(targetStatement) || foundTarget)) {
                foundTarget = true;
                targetVariables.forEach((var) -> {
                    Deque<Map<String, Integer>> scopeStackCopy = new ArrayDeque<>(scopeStack);
                    while(!scopeStackCopy.isEmpty()){
                        if(scopeStackCopy.peek().containsKey(var)){
                            sameScopeAssignments.put(var, scopeStackCopy.peek().get(var));
                            break;
                        }
                        scopeStackCopy.pop();
                    }
                });
                return;
            }

            scopeStack.pop();
        }

        @Override
        public void visit(SwitchStmt block, Void arg) {
            if (foundTarget) return;

            scopeStack.push(new HashMap<>());
            super.visit(block, arg);

            if (block.getEntries().contains(targetStatement) || foundTarget) {
                foundTarget = true;
                targetVariables.forEach((var) -> {
                    Deque<Map<String, Integer>> scopeStackCopy = new ArrayDeque<>(scopeStack);
                    while(!scopeStackCopy.isEmpty()){
                        if(scopeStackCopy.peek().containsKey(var)){
                            sameScopeAssignments.put(var, scopeStackCopy.peek().get(var));
                            break;
                        }
                        scopeStackCopy.pop();
                    }
                });
                return;
            }

            scopeStack.pop();
        }

        @Override
        public void visit(VariableDeclarationExpr vde, Void arg) {
            if (foundTarget) return;
            vde.getVariables().forEach(v ->
                    scopeStack.peek().put(v.getNameAsString(), vde.getRange().get().begin.line));
            super.visit(vde, arg);
        }

        @Override
        public void visit(AssignExpr assignExpr, Void arg) {
            if (foundTarget) return;
            if (assignExpr.getTarget() instanceof NameExpr) {
                String varName = ((NameExpr) assignExpr.getTarget()).getNameAsString();
                scopeStack.peek().put(varName, assignExpr.getRange().get().begin.line);
            }

            super.visit(assignExpr, arg);
        }

        @Override
        public void visit(ExpressionStmt stmt, Void arg) {
            if (foundTarget) return;
            if(targetStatement.getRange().get().begin.line  <= stmt.getRange().get().begin.line){
                foundTarget = true;
                return;
            }
            super.visit(stmt, arg);
        }

        @Override
        public void visit(ForStmt stmt, Void arg) {
            if (foundTarget) return;

            if(targetStatement.getRange().get().begin.line  <= stmt.getRange().get().begin.line) {
                foundTarget = true;
                return;
            }
            super.visit(stmt, arg);
        }

        @Override
        public void visit(IfStmt stmt, Void arg) {
            if (foundTarget) return;

             if(targetStatement.getRange().get().begin.line  <= stmt.getRange().get().begin.line
            && targetStatement.getRange().get().end.line  >= stmt.getRange().get().end.line){
                foundTarget = true;
                return;
            }
            super.visit(stmt, arg);
        }

        @Override
        public void visit(SwitchEntry stmt, Void arg){
            if (foundTarget) return;

            if(targetStatement.getRange().get().begin.line  <= stmt.getRange().get().begin.line){
                foundTarget = true;
                return;
            }
            super.visit(stmt, arg);
        }

        @Override
        public void visit(MethodCallExpr stmt, Void arg){
            if (foundTarget) return;

            if(targetStatement.getRange().get().begin.line  <= stmt.getRange().get().begin.line){
                foundTarget = true;
                return;
            }
            super.visit(stmt, arg);
        }

        @Override
        public void visit(LabeledStmt stmt, Void arg){
            if (foundTarget) return;

            if(targetStatement.getRange().get().begin.line  <= stmt.getRange().get().begin.line){
                foundTarget = true;
                return;
            }
            super.visit(stmt, arg);
        }

        public Map<String, Integer> getAssignment() {
            return sameScopeAssignments;
        }
    }
}