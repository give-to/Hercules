package org.example.hercules.findContext;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractVariable {


    public static void main(String[] args) throws Exception {
        // Java源代码
        String code = "int a = 10;\nString b = \"Hello\";\nSystem.out.println(a + b);";

        JavaParser javaParser = new JavaParser();
        // 解析Java源代码
        ParseResult<CompilationUnit> cu = javaParser.parse(code);
        CompilationUnit result = cu.getResult().get();
        // 遍历抽象语法树（AST），查找变量声明
        List<String> variables = new ArrayList<>();
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(VariableDeclarator vd, Void arg) {
                variables.add(vd.getNameAsString());
                super.visit(vd, arg);
            }
        }.visit(result, null);

        // 输出变量列表
        String variableList = variables.stream().collect(Collectors.joining(", "));
        System.out.println("Variables used in the code: " + variableList);
    }
}

