package org.example.hercules;

import org.eclipse.jdt.core.dom.ASTNode;
import tree.LblTree;
import util.FormatUtilities;

import java.util.Vector;

public class HerculesLblTree extends LblTree {
    public int type;
    public String kind;
    public String name;
    public Class kindClass;
    public Object typeClass;

    public HerculesLblTree(int type, String kind, String name, int treeId) {
        super(type + ":" + kind + ":" + name, treeId);
        this.type = type;
        this.kind = kind;
        this.name = name;
        typeClass = ASTNode.nodeClassForType(type);

    }


    public static LblTree fromString(String s) {
//        int treeID = FormatUtilities.getTreeID(s);
        int treeID = -1;
        s = s.substring(s.indexOf(OPEN_BRACKET), s.lastIndexOf(CLOSE_BRACKET) + 1);
        LblTree node = new LblTree(FormatUtilities.getRoot(s), treeID);
        Vector c = FormatUtilities.getChildren(s);
        for (int i = 0; i < c.size(); i++) {
            node.add(fromString((String) c.elementAt(i)));
        }
        return node;
    }
}
