package org.example.hercules.findContext;


import soot.*;
import soot.jimple.Stmt;
import soot.jimple.internal.JIfStmt;
import soot.options.Options;
import soot.tagkit.InnerClassTag;
import soot.tagkit.Tag;
import soot.toolkits.scalar.LocalDefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ReachingDefinitionFinder {
    private static ClassFieldCrossReferenceAnalysis crossRef = null;

    public static List<Integer> findContext(String targetPath, String susJavaClass, Integer susline) {
        initial(targetPath);
        PackManager.v().runPacks();  // process and build call graph
        List<Integer> contextList = post_analysis(susJavaClass, susline);
        System.out.println(contextList);
        return contextList;
    }

    public static List<Integer> findContext(String targetPath, String susJavaClass, Integer susline,String susMethod) {
        initial(targetPath);
        PackManager.v().runPacks();  // process and build call graph
        List<Integer> contextList = post_analysis(susJavaClass, susline,susMethod);
        System.out.println(contextList);
        return contextList;
    }


    private static List<Integer> post_analysis(String susJavaClass, int currentLine) {

        ArrayList<Integer> contextNumList = new ArrayList<>();
        contextNumList.add(currentLine);

        System.out.println("=========post_analysis==========");
        SootClass abicase5 = Scene.v().getSootClass(susJavaClass);
        List<SootClass> sootClasses = new ArrayList<>();
        sootClasses.add(abicase5);
        List<Tag> tags = abicase5.getTags();
        for (Tag tag : tags) {
            if (tag instanceof InnerClassTag) {
                String innerClassName = ((InnerClassTag) tag).getInnerClass();
                innerClassName = innerClassName.replace("/", ".");
                sootClasses.add(Scene.v().getSootClass(innerClassName));
            }
        }
        for (SootClass sootClass : sootClasses) {
            List<SootMethod> methods = sootClass.getMethods();
            for (int index = 0; index < methods.size(); index++) {
//            SootMethod go = abicase5.getMethodByName(susMethod);
                SootMethod go = methods.get(index);
                try {
                    LocalDefs ld = G.v().soot_toolkits_scalar_LocalDefsFactory().newLocalDefs(go.getActiveBody());
                } catch (Exception e) {
                    System.out.println("current function is :" + go);
                    System.out.println("getActiveBody() has something wrong, maybe this function is abstract!");
                    System.out.println(e);
                    continue;
                }

                for (Unit u : go.getActiveBody().getUnits()) {
                    Stmt stmt = (Stmt) u;
                    if (stmt instanceof JIfStmt) {
                        UnitBox targetBox = ((JIfStmt) stmt).getTargetBox();
                        if (targetBox.getUnit().getJavaSourceStartLineNumber() == currentLine) {
                            List<ValueBox> useList = targetBox.getUnit().getUseBoxes();
                            for (int i = 0; i < useList.size(); i++) {
                                int lineNum = useList.get(i).getJavaSourceStartLineNumber();
                                if (lineNum >= 0)
                                    contextNumList.add(lineNum);
                            }
                        }
                    }
                    if (stmt.getJavaSourceStartLineNumber() == currentLine) {
                        List<ValueBox> useList = stmt.getUseBoxes();
                        for (int i = 0; i < useList.size(); i++) {
                            int lineNum = useList.get(i).getJavaSourceStartLineNumber();
                            if (lineNum >= 0)
                                contextNumList.add(lineNum);
                        }
                    }
                }
            }
        }
        contextNumList = new ArrayList<>(new HashSet<>(contextNumList));
        return contextNumList;
    }

    private static List<Integer> post_analysis(String susJavaClass, int currentLine, String susMethod) {
        ArrayList<Integer> contextNumList = new ArrayList<>();
        contextNumList.add(currentLine);

        System.out.println("=========post_analysis==========");
        SootClass abicase5 = Scene.v().getSootClass(susJavaClass);
        List<SootClass> sootClasses = new ArrayList<>();
        sootClasses.add(abicase5);
        List<Tag> tags = abicase5.getTags();
        for (Tag tag : tags) {
            if (tag instanceof InnerClassTag) {
                String innerClassName = ((InnerClassTag) tag).getInnerClass();
                innerClassName = innerClassName.replace("/", ".");
                sootClasses.add(Scene.v().getSootClass(innerClassName));
            }
        }
        for (SootClass sootClass : sootClasses) {
            SootMethod go = null;
            try {
                go = abicase5.getMethodByName(susMethod);
            } catch (Exception e) {
                continue;
            }

            try {
                LocalDefs ld = G.v().soot_toolkits_scalar_LocalDefsFactory().newLocalDefs(go.getActiveBody());
            } catch (Exception e) {
                System.out.println("current function is :" + go);
                System.out.println("getActiveBody() has something wrong, maybe this function is abstract!");
                System.out.println(e);
                continue;
            }
            for (Unit u : go.getActiveBody().getUnits()) {
                Stmt stmt = (Stmt) u;
                if (stmt instanceof JIfStmt) {
                    UnitBox targetBox = ((JIfStmt) stmt).getTargetBox();
                    if (targetBox.getUnit().getJavaSourceStartLineNumber() == currentLine) {
                        List<ValueBox> useList = targetBox.getUnit().getUseBoxes();
                        for (int i = 0; i < useList.size(); i++) {
                            int lineNum = useList.get(i).getJavaSourceStartLineNumber();
                            if (lineNum >= 0)
                                contextNumList.add(lineNum);
                        }
                    }
                }
                if (stmt.getJavaSourceStartLineNumber() == currentLine) {
                    List<ValueBox> useList = stmt.getUseBoxes();
                    for (int i = 0; i < useList.size(); i++) {
                        int lineNum = useList.get(i).getJavaSourceStartLineNumber();
                        if (lineNum >= 0)
                            contextNumList.add(lineNum);
                    }
                }
            }
//            }
        }
        contextNumList = new ArrayList<>(new HashSet<>(contextNumList));
        return contextNumList;
    }

    private static void initial(String apkPath) {
        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_src_prec(Options.src_prec_java);
        Options.v().set_process_dir(Collections.singletonList(apkPath));//路径应为文件夹
        Options.v().set_keep_line_number(true);
//		Options.v().set_whole_program(true);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_app(true);
        Options.v().set_whole_program(true);    // 全程序分析
        Options.v().set_verbose(true);          // 显示详细信息
//		 Scene.v().setMainClass(appclass); // how to make it work ?
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.Thread", SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
    }

}
