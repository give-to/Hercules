package org.example.hercules.utils;

import edu.lu.uni.serval.jdt.tree.ITree;
import edu.lu.uni.serval.jdt.tree.Tree;

import java.util.List;

public class GenerateStringFromTree {
    public static String generateString(Tree root) {
        StringBuffer rst = new StringBuffer();
        StringBuffer childStr = new StringBuffer();
        //Debug
        if(root.getName().contains("handleTypeCycle(t);")){
            System.out.println("Debug!");
        }

        String name = root.getName().replace("{","LeftCurlyBraces");
        name = name.replace("}","RightCurlyBraces");
//        if (root != null) {
        String currentValue = root.getType() + "###" + root.getHerculesType() + "###" + name;
//        String currentValue = root.getName();
        List<ITree> children = root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            childStr.append("{" + generateString((Tree) children.get(i)) + "}");
        }
        rst.append(currentValue + childStr);
//        }

        return rst.toString();
    }
}
