package org.example.hercules.findContext;


import edu.lu.uni.serval.entity.Pair;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ClassFieldCrossReferenceAnalysis {
    public static String combiner = "_$p$g_";
    public final HashMap<String, SootField> classNameFieldNameToSootFieldMapping = new HashMap<>();
    public final HashMap<String, ArrayList<Value>> fieldToValues = new HashMap<>();
    public final HashMap<String, ArrayList<Pair<Stmt,SootMethod>>> fieldValueToUse = new HashMap<>();
    private final List<SootClass> appClasses;


    public ClassFieldCrossReferenceAnalysis(List<SootClass> classes) {
        appClasses = classes;
        computeFieldToValuesAssignedList();
    }

    public Pair<Stmt,SootMethod> getSigleDefine(FieldRef ref) {
        SootField field = ref.getField();
        String fieldName = field.getName();
        String declaringClass = field.getDeclaringClass().getName();
        String combined = declaringClass + combiner + fieldName;
        ArrayList<Pair<Stmt,SootMethod>> stmtList = fieldValueToUse.get(combined);
        if (stmtList == null || stmtList.size() == 0) {
            return null;
        }
        if (stmtList.size() > 1) {
            System.out.println(stmtList.toString());
        }
        return stmtList.get(0);
    }

    /*
     * Go through all the methods in the application and make a mapping of className+methodName ---> values assigned There can
     * obviously be more than one value assigned to each field
     */
    private void computeFieldToValuesAssignedList() {
        // go through all the classes
        for (SootClass s : appClasses) {

            // go though all the methods
            for (Iterator<SootMethod> it = s.methodIterator(); it.hasNext(); ) {
                SootMethod m = it.next();
                if (!m.hasActiveBody()) {
                    continue;
                }

                JimpleBody body = (JimpleBody) m.getActiveBody();
                for (Unit u : body.getUnits()) {
                    Stmt stmt = (Stmt) u;

                    if (stmt.containsFieldRef()) { // assignStatement{
                        System.out.println("在方法: " + m.getName() + "中发现成员使用" + stmt.toString());
                        if (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof FieldRef) {
                            System.out.println("但是是使用点，不是赋值点，跳过");
                            continue;
                        }
                        FieldRef ref = stmt.getFieldRef();
                        SootField field = ref.getField();
                        String fieldName = field.getName();
                        String declaringClass = field.getDeclaringClass().getName();

                        System.out.println("\t成员名: " + fieldName);
                        System.out.println("\t成员所在类: " + declaringClass);
                        // get the valueList for this class+field combo
                        String combined = declaringClass + combiner + fieldName;
                        ArrayList<Pair<Stmt,SootMethod>> stmtList = fieldValueToUse.get(combined);
                        if (stmtList == null) {
                            stmtList = new ArrayList<>();
                            fieldValueToUse.put(combined, stmtList);
                        }
                        stmtList.add(new Pair<>(stmt, m));

//                        ArrayList<Value> valueList = fieldToValues.get(combined);
//                        if (valueList == null) {
//                            // no value of this field was yet assigned
//                            valueList = new ArrayList<>();
//                            fieldToValues.put(combined, valueList);
//                        }
//                        valueList.add(stmt.getRightOp());

                    }

                }
            }
        } // going through methods of class s
    }
}

